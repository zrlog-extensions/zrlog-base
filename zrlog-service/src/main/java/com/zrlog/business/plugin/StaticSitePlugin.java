package com.zrlog.business.plugin;

import com.hibegin.common.util.*;
import com.hibegin.common.util.http.HttpUtil;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.ApplicationContext;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.config.AbstractServerConfig;
import com.hibegin.http.server.config.RequestConfig;
import com.hibegin.http.server.config.ResponseConfig;
import com.hibegin.http.server.handler.HttpRequestHandlerRunnable;
import com.hibegin.http.server.util.HttpRequestBuilder;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.business.plugin.type.StaticSiteType;
import com.zrlog.common.Constants;
import com.zrlog.common.exception.ArgsException;
import com.zrlog.data.cache.CacheServiceImpl;
import com.zrlog.model.WebSite;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.util.I18nUtil;
import com.zrlog.util.ThreadUtils;
import com.zrlog.util.ZrLogUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.zrlog.plugin.BaseStaticSitePlugin.isStaticPluginRequest;

public interface StaticSitePlugin extends BaseStaticSitePlugin {

    static String getSuffix(HttpRequest request) {
        if (isStaticPluginRequest(request) || Constants.isStaticHtmlStatus()) {
            return ".html";
        }
        return "";
    }

    Logger LOGGER = LoggerUtil.getLogger(StaticSitePlugin.class);

    default void faviconHandle(String faviconIconBase64, String saveFilePath, Boolean saveToCache) {
        if (StringUtils.isEmpty(faviconIconBase64)) {
            copyResourceToCacheFolder(saveFilePath);
            return;
        }
        try {
            File file = PathUtil.getStaticFile(saveFilePath);
            file.getParentFile().mkdirs();
            byte[] binBytes;
            if (faviconIconBase64.contains(",")) {
                binBytes = Base64.getDecoder().decode(faviconIconBase64.split(",")[1]);
            } else {
                binBytes = Base64.getDecoder().decode(faviconIconBase64);
            }
            IOUtil.writeBytesToFile(binBytes, file);
            if (Objects.equals(saveToCache, true)) {
                saveToCacheFolder(new ByteArrayInputStream(binBytes), saveFilePath);
            }
        } catch (Exception e) {
            LOGGER.warning("Save favicon error " + e.getMessage());
        }
    }

    default void copyResourceToCacheFolder(String resourceName) {
        InputStream inputStream = CacheServiceImpl.class.getResourceAsStream(resourceName);
        if (Objects.isNull(inputStream)) {
            LOGGER.warning("Missing resource " + resourceName);
            return;
        }
        saveToCacheFolder(inputStream, resourceName);
    }

    static boolean isDisabled() {
        if (!Constants.isStaticHtmlStatus()) {
            return true;
        }
        return StringUtils.isEmpty(ZrLogUtil.getBlogHostByWebSite());
    }

    default String getSiteVersion() {
        StringBuilder sb = new StringBuilder();
        for (File file : new TreeSet<>(getCacheFiles())) {
            sb.append(SecurityUtils.md5ByFile(file));
        }
        return Math.abs(sb.toString().hashCode()) + "";
    }

    String getVersionFileName();

    String getDbCacheKey();

    default boolean isSynchronized(String scheme) {
        try {
            if (isDisabled()) {
                return true;
            }
            String versionFile = (Objects.nonNull(scheme) ? scheme : "https") + "://" + Constants.getHost() + getContextPath() + "/" + getVersionFileName();
            String remoteSiteVersion = HttpUtil.getInstance().getSuccessTextByUrl(versionFile);
            return Objects.equals(getSiteVersion(), remoteSiteVersion);
        } catch (Exception e) {
            if (Constants.debugLoggerPrintAble()) {
                LOGGER.severe("isSynchronized error, " + LoggerUtil.recordStackTraceMsg(e));
            } else {
                LOGGER.warning("isSynchronized error " + e.getMessage());
            }
        }
        return false;
    }

    default File loadCacheFile(HttpRequest request) {
        String lang = I18nUtil.getAcceptLocal(request);
        String cacheKey = request.getUri();
        if (Objects.equals(cacheKey, "/")) {
            cacheKey = "/index.html";
        }
        File file = getCacheFile(lang, cacheKey);
        if (file.exists()) {
            return file;
        }
        return getCacheFile(getDefaultLang(), cacheKey);
    }

    String getContextPath();

    String getDefaultLang();

    private File getCacheFile(String lang, String uri) {
        StringJoiner fileSj = new StringJoiner("/");
        fileSj.add(lang);
        fileSj.add(getContextPath());
        fileSj.add(uri);
        return PathUtil.getCacheFile(fileSj.toString());
    }

    default File getCacheFile(String uri) {
        return getCacheFile(getDefaultLang(), uri);
    }

    default void saveToCacheFolder(InputStream inputStream, String uri) {
        if (!Constants.isStaticHtmlStatus()) {
            return;
        }
        File file = getCacheFile(uri);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            inputStream.transferTo(outputStream);
            inputStream.close();
            getCacheFiles().add(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Map<String, HandleState> getHandleStatusPageMap();

    Lock getParseLock();

    private HttpRequest buildMockRequest(HttpMethod method, String uri, RequestConfig requestConfig, ApplicationContext applicationContext) throws Exception {
        return HttpRequestBuilder.buildRequest(method, uri, ZrLogUtil.getBlogHostByWebSite(), STATIC_USER_AGENT, requestConfig, applicationContext);
    }

    private void doParseHtml(File file) throws IOException {
        if (file.getName().endsWith(".js") || file.getName().endsWith(".json") || file.getName().endsWith(".css")) {
            return;
        }
        getParseLock().lock();
        try {
            Document document = Jsoup.parse(file);
            Elements links = document.select("a");
            links.forEach(element -> {
                String href = element.attr("href");
                if (href.startsWith("//")) {
                    return;
                }
                //exists jobs
                if (getHandleStatusPageMap().containsKey(href)) {
                    return;
                }
                if (href.startsWith("/") && href.endsWith(".html")) {
                    getHandleStatusPageMap().put(href, HandleState.NEW);
                }
            });
        } finally {
            getParseLock().unlock();
        }
    }

    default String notFindFile() {
        return getContextPath() + "/error/404.html";
    }

    private CompletableFuture<Void> doAsyncFetch(String key, AbstractServerConfig serverConfig, ApplicationContext applicationContext, Executor executor) {
        getHandleStatusPageMap().put(key, HandleState.HANDING);
        return CompletableFuture.runAsync(() -> {
            boolean error = false;
            ResponseConfig responseConfig = serverConfig.getResponseConfig();
            responseConfig.setEnableGzip(false);
            HttpRequest httpRequest;
            try {
                httpRequest = buildMockRequest(HttpMethod.GET, key, serverConfig.getRequestConfig(), applicationContext);
            } catch (Exception e) {
                LOGGER.warning("Generator " + key + " error: " + e.getMessage());
                return;
            }
            String uri = httpRequest.getUri();
            try (BodySaveResponse bodySaveResponse = new BodySaveResponse(httpRequest, serverConfig.getResponseConfig(), true)) {
                new HttpRequestHandlerRunnable(httpRequest, bodySaveResponse).run();
                File file = bodySaveResponse.getCacheFile();
                if (Objects.nonNull(file)) {
                    getCacheFiles().add(file);
                }
                if (httpRequest.getUri().startsWith(Constants.ADMIN_URI_BASE_PATH + "/")) {
                    return;
                }
                if (Objects.equals(uri, notFindFile())) {
                    return;
                }
                if (Objects.isNull(file) || !file.exists()) {
                    LOGGER.warning("Generator " + uri + " error: missing static file");
                    error = true;
                    return;
                }
                doParseHtml(file);
            } catch (Exception ex) {
                LOGGER.warning("Generator " + uri + " error: " + ex.getMessage());
            } finally {
                if (error) {
                    //如果抓取错误了，需要暂停一下，避免重新获取过快
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        LOGGER.warning("Generator " + uri + " error: " + ex.getMessage());
                    }
                }
                getHandleStatusPageMap().put(key, error ? HandleState.RE_FETCH : HandleState.HANDLED);
            }
        }, executor);
    }

    Executor getExecutorService();

    List<File> getCacheFiles();

    default void doFetch(AbstractServerConfig serverConfig, ApplicationContext applicationContext) {
        long start = System.currentTimeMillis();

        try {
            while (getHandleStatusPageMap().values().stream().anyMatch(e -> e == HandleState.NEW)) {
                CompletableFuture.allOf(getHandleStatusPageMap().entrySet().stream().filter(e -> Arrays.asList(HandleState.NEW, HandleState.RE_FETCH).contains(e.getValue())).map(e -> {
                    return doAsyncFetch(e.getKey(), serverConfig, applicationContext, getExecutorService());
                }).toArray(CompletableFuture[]::new)).join();
            }
            String version = saveCacheVersion();
            long usedTime = (System.currentTimeMillis() - start);
            if (Constants.debugLoggerPrintAble()) {
                LOGGER.info("Generator [" + getDbCacheKey() + "] [" + version + "] " + "size " + getHandleStatusPageMap().size() + " finished in " + usedTime + "ms");
            } else if (usedTime > Duration.ofSeconds(10).toMillis()) {
                LOGGER.warning("Generator [" + getDbCacheKey() + "] [" + version + "] slow size " + getHandleStatusPageMap().size() + " finished in " + usedTime + "ms");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "do fetch error", e.getMessage());
        }
    }

    default int getSyncTimeout() {
        if (EnvKit.isFaaSMode()) {
            //建议配置 FaaS 为最大超时
            return 12 * 60;
        }
        return 3600;
    }

    StaticSiteType getType();

    default boolean refreshStaticSiteCache(HttpRequest request, List<StaticSiteType> siteTypes) {
        if (siteTypes == null || siteTypes.isEmpty()) {
            return false;
        }
        List<StaticSitePlugin> staticSitePlugins = Constants.zrLogConfig.getPluginsByClazz(StaticSitePlugin.class).stream().filter(e -> siteTypes.contains(e.getType())).collect(Collectors.toList());
        ExecutorService executorService = ThreadUtils.newFixedThreadPool(staticSitePlugins.size());
        try {
            List<Boolean> results = new CopyOnWriteArrayList<>();
            CompletableFuture.allOf(staticSitePlugins.stream().map(staticSitePlugin -> {
                return CompletableFuture.runAsync(() -> {
                    staticSitePlugin.start();
                    results.add(waitCacheSync(request, getSyncTimeout()));
                }, executorService);
            }).toArray(CompletableFuture[]::new)).join();
            return results.stream().allMatch(e -> Objects.equals(e, Boolean.TRUE));
        } finally {
            executorService.shutdown();
        }
    }

    private boolean waitCacheSync(HttpRequest request, int timeoutInSeconds) {
        if (timeoutInSeconds <= 0) {
            throw new ArgsException("timeoutInSeconds must be greater than 0");
        }
        //启动插件
        String version = getSiteVersion();
        PluginCorePlugin pluginCorePlugin = Constants.zrLogConfig.getPlugin(PluginCorePlugin.class);
        if (Objects.nonNull(pluginCorePlugin) && !pluginCorePlugin.isStarted()) {
            pluginCorePlugin.start();
            pluginCorePlugin.refreshCache(version, request);
        }
        for (int i = 0; i < timeoutInSeconds; i++) {
            if (isSynchronized(request.getScheme())) {
                try {
                    new WebSite().updateByKV(getDbCacheKey(), version);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "update site version " + version + " cache error", e);
                }
                if (Constants.debugLoggerPrintAble()) {
                    LOGGER.info("update site version " + version + " cache success");
                }
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        LOGGER.warning("update site version " + getSiteVersion() + " cache timeout");
        return false;
    }

    private String saveCacheVersion() {
        String siteVersion = getSiteVersion();
        IOUtil.writeStrToFile(siteVersion, getCacheFile(getVersionFileName()));
        return siteVersion;
    }

    enum HandleState {
        NEW, HANDING, RE_FETCH, HANDLED
    }
}
