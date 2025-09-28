package com.zrlog.business.plugin;

import com.google.gson.Gson;
import com.hibegin.common.BaseLockObject;
import com.hibegin.common.util.*;
import com.hibegin.common.util.http.HttpUtil;
import com.hibegin.common.util.http.handle.CloseResponseHandle;
import com.hibegin.common.util.http.handle.HttpHandle;
import com.hibegin.common.util.http.handle.HttpResponseJsonHandle;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.config.ConfigKit;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.business.rest.response.PluginCoreStatus;
import com.zrlog.business.rest.response.PluginStatusResponse;
import com.zrlog.common.Constants;
import com.zrlog.common.vo.AdminTokenVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.util.BlogBuildInfoUtil;
import com.zrlog.util.I18nUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 运行ZrLog的插件，当 conf/plugins/这里目录下面不存在插件核心服务时，会通过网络请求下载最新的插件核心服务，也可以通过
 * 这种方式进行插件的及时更新。
 * 插件核心服务通过调用系统命令的命令进行启动的。
 */
public class PluginCorePluginImpl extends BaseLockObject implements PluginCorePlugin {

    private static final Logger LOGGER = LoggerUtil.getLogger(PluginCorePluginImpl.class);

    private final File dbPropertiesPath;
    private final String pluginJvmArgs;
    private final PluginCoreProcess pluginCoreProcess;
    private final String token;
    private volatile String pluginServerBaseUrl;

    public PluginCorePluginImpl(File dbPropertiesPath, String contextPath) {
        this.dbPropertiesPath = dbPropertiesPath;
        String args = ConfigKit.get("pluginJvmArgs", "-Xms8m -Xmx64m -Dfile.encoding=UTF-8");
        if (EnvKit.isDevMode()) {
            args += " -Dsws.run.mode=dev";
        }
        this.pluginJvmArgs = args;
        this.pluginCoreProcess = new PluginCoreProcessImpl(this::stop, contextPath);
        this.token = UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public boolean autoStart() {
        return !EnvKit.isFaaSMode();
    }

    private static Map<String, String> genHeaderMapByRequest(HttpRequest request, AdminTokenVO adminTokenVO) {
        Map<String, String> map = new HashMap<>();
        if (adminTokenVO != null) {
            map.put("LoginUserId", adminTokenVO.getUserId() + "");
        }
        map.put("IsLogin", (adminTokenVO != null) + "");
        map.put("Current-Locale", I18nUtil.getCurrentLocale());
        map.put("Blog-Version", BlogBuildInfoUtil.getVersion());
        PublicWebSiteInfo publicWebSiteInfo = Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo();
        map.put("Dark-Mode", publicWebSiteInfo.getAdmin_darkMode() + "");
        if (EnvKit.isDevMode()) {
            map.put("DEV_MODE", "true");
        }
        map.put("Admin-Color-Primary", publicWebSiteInfo.getAdmin_color_primary());
        if (Objects.isNull(request)) {
            return map;
        }
        if (Objects.nonNull(request.getHeader("Cookie"))) {
            map.put("Cookie", request.getHeader("Cookie"));
        }
        map.put("AccessUrl", "http://127.0.0.1:" + request.getServerConfig().getPort());
        if (Objects.nonNull(request.getHeader("Content-Type"))) {
            map.put("Content-Type", request.getHeader("Content-Type"));
        }
        if (StringUtils.isNotEmpty(request.getHeader("Referer"))) {
            map.put("Referer", request.getHeader("Referer"));
        }
        String fullUrl;
        if (Objects.nonNull(adminTokenVO)) {
            fullUrl = request.getFullUrl().replaceFirst("http://", adminTokenVO.getProtocol() + "://");
        } else {
            fullUrl = request.getFullUrl();
        }
        map.put("Full-Url", UrlEncodeUtils.encodeUrl(fullUrl));
        return map;
    }

    private void waitToStarted() {
        lock.lock();
        try {
            if (this.isStarted()) {
                return;
            }
            this.start();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CloseResponseHandle getContext(String uri, HttpMethod method, HttpRequest request, AdminTokenVO adminTokenVO) throws IOException, URISyntaxException, InterruptedException {
        waitToStarted();
        CloseResponseHandle handle = new CloseResponseHandle();
        String forwardUrl = pluginServerBaseUrl + uri + (uri.contains("?") ? "&" : "?") + request.getQueryStr();
        //GET请求不关心request.getInputStream() 的数据
        if (method.equals(request.getMethod()) && method == HttpMethod.GET) {
            HttpUtil.getInstance().sendGetRequest(forwardUrl, new HashMap<>(), handle, genHeaderMapByRequest(request, adminTokenVO));
        } else {
            HttpUtil.getInstance().sendPostRequest(forwardUrl, IOUtil.getByteByInputStream(request.getInputStream()), handle, genHeaderMapByRequest(request, adminTokenVO));
        }
        return handle;
    }

    @Override
    public <T> T requestService(HttpRequest inputRequest, Map<String, String[]> body, AdminTokenVO adminTokenVO, Class<T> clazz) throws IOException, InterruptedException {
        waitToStarted();
        return HttpUtil.getInstance().sendPostRequest(pluginServerBaseUrl + "/service", body, new HttpResponseJsonHandle<>(clazz), genHeaderMapByRequest(inputRequest, adminTokenVO)).getT();
    }


    @Override
    public boolean accessPlugin(String uri, HttpRequest request, HttpResponse response, AdminTokenVO adminTokenVO) throws IOException, URISyntaxException, InterruptedException {
        waitToStarted();
        CloseResponseHandle handle = getContext(uri, request.getMethod(), request, adminTokenVO);
        if (Objects.isNull(handle.getT()) || Objects.isNull(handle.getT().body())) {
            return false;
        }
        List<String> ignoreHeaderKeys = Arrays.asList("content-encoding", "transfer-encoding", "content-length", "server", "connection");
        try (InputStream inputStream = handle.getT().body()) {
            for (Map.Entry<String, List<String>> header : handle.getT().headers().map().entrySet()) {
                if (ignoreHeaderKeys.stream().anyMatch(x -> Objects.equals(x, header.getKey()))) {
                    continue;
                }
                String value = header.getValue().get(0);
                //处理 302，contextPath 丢失的问题
                if (Objects.equals(header.getKey().toLowerCase(), "location") && handle.getT().statusCode() == 302) {
                    if (value.startsWith("/")) {
                        value = request.getContextPath() + value;
                    }
                }
                response.addHeader(header.getKey(), value);
            }
            //将插件服务的HTTP的body返回给调用者
            response.write(inputStream, handle.getT().statusCode());
            return true;
        }
    }

    /**
     * 这里使用独立的线程进行启动，主要是为了防止插件服务出问题后，影响整体，同时是避免启动过慢的问题。
     *
     * @return
     */
    @Override
    public boolean start() {
        if (isStarted()) {
            return true;
        }
        lock.lock();
        try {
            if (isStarted()) {
                return true;
            }
            //加载 ZrLog 提供的插件
            int port = pluginCoreProcess.pluginServerStart(dbPropertiesPath.toString(), pluginJvmArgs, PathUtil.getStaticPath(), BlogBuildInfoUtil.getVersion(), token);
            String serverUrl = "http://127.0.0.1:" + port;
            waitToStarted(serverUrl, token, 360);
            this.pluginServerBaseUrl = serverUrl;
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isStarted() {
        return Objects.nonNull(pluginServerBaseUrl);
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public boolean stop() {
        pluginCoreProcess.stopPluginCore();
        pluginServerBaseUrl = null;
        return true;
    }

    @Override
    public boolean refreshCache(String cacheVersion, HttpRequest request) {
        waitToStarted();
        if (Objects.isNull(pluginServerBaseUrl)) {
            return false;
        }
        refreshCacheWithRetry(EnvKit.isFaaSMode() ? Integer.MAX_VALUE : 5, cacheVersion);
        return true;
    }


    private void refreshCacheWithRetry(int retryCount, String cacheVersion) {
        try {
            HttpUtil.getInstance().getSuccessTextByUrl(pluginServerBaseUrl + "/api/refreshCache?cacheVersion=" + cacheVersion + "&token=" + token);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            int rCount = retryCount - 1;
            if (retryCount < 0) {
                LOGGER.log(Level.SEVERE, "refresh plugin cache error ", e);
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            refreshCacheWithRetry(rCount, cacheVersion);
        }
    }

    private static void waitToStarted(String pluginServerBaseUrl, String token, int retryCount) {
        if (retryCount < 0) {
            LOGGER.severe("refresh plugin cache error, timeout");
            return;
        }
        int seek = 1000;
        try {
            HttpHandle<PluginStatusResponse> httpHandle = HttpUtil.getInstance().sendGetRequest(pluginServerBaseUrl + "/api/status?token=" + token, new HttpResponseJsonHandle<>(PluginStatusResponse.class), new ConcurrentHashMap<>());
            PluginStatusResponse statusResponse = httpHandle.getT();
            if (Objects.isNull(statusResponse) || Objects.isNull(statusResponse.getStatus())) {
                LOGGER.info("Need upgrade plugin-core");
                return;
            }
            if (Constants.debugLoggerPrintAble()) {
                LOGGER.info("Plugin status: " + new Gson().toJson(statusResponse));
            }
            if (!Objects.equals(statusResponse.getStatus(), PluginCoreStatus.STARTED)) {
                int rCount = retryCount - 1;
                Thread.sleep(seek);
                waitToStarted(pluginServerBaseUrl, token, rCount);
            }
        } catch (Exception e) {
            int rCount = retryCount - 1;

            try {
                Thread.sleep(seek);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            waitToStarted(pluginServerBaseUrl, token, rCount);
        }
    }
}
