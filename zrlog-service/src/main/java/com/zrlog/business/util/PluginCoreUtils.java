package com.zrlog.business.util;

import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.http.HttpUtil;
import com.hibegin.common.util.http.handle.HttpFileHandle;
import com.zrlog.util.BlogBuildInfoUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PluginCoreUtils {

    private static final Logger LOGGER = LoggerUtil.getLogger(PluginCoreUtils.class);

    private static File getPluginFileName(String pluginsFolder) {
        return getPluginFileName(pluginsFolder, EnvKit.isNativeImage(), BlogBuildInfoUtil.getFileArch());
    }

    static File getPluginFileName(String pluginsFolder, boolean nativeImage, String fileArch) {
        if (!nativeImage) {
            return new File(pluginsFolder + "/plugin-core.jar");
        }
        if (fileArch.contains("Window")) {
            return new File(pluginsFolder + "/plugin-core-" + fileArch + ".exe");
        }
        return new File(pluginsFolder + "/plugin-core-" + fileArch + ".bin");
    }


    public static File tryDownloadPluginCoreFile(String pluginsFolder) {
        return tryDownloadPluginCoreFile(pluginsFolder, EnvKit.isNativeImage(), BlogBuildInfoUtil.getFileArch());
    }

    static File tryDownloadPluginCoreFile(String pluginsFolder, boolean nativeImage, String fileArch) {
        File pluginCoreFile = getPluginFileName(pluginsFolder, nativeImage, fileArch);
        try {
            if (pluginCoreFile.exists()) {
                return pluginCoreFile;
            }
            pluginCoreFile.getParentFile().mkdirs();
            String filePath = pluginCoreFile.getParentFile().toString();
            //插件服务的下载地址
            String withoutCacheDownloadUrl = BlogBuildInfoUtil.getResourceDownloadUrl() + "/plugin/core/" + pluginCoreFile.getName() + "?_t=" + System.currentTimeMillis();
            LOGGER.info(pluginCoreFile.getName() + " not exists will download from " + withoutCacheDownloadUrl);
            Map<String, String> map = new HashMap<>();
            map.put("Cache-Control", "no-cache");
            HttpUtil.getInstance().sendGetRequest(withoutCacheDownloadUrl, new HashMap<>(), new HttpFileHandle(filePath), map);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException("download plugin core error, " + e.getMessage(), e);
        } finally {
            if (pluginCoreFile.exists() && pluginCoreFile.length() > 0) {
                if (pluginCoreFile.getName().endsWith(".bin")) {
                    CmdUtil.sendCmd("chmod", "a+x", pluginCoreFile.toString());
                }
            }
        }
        return pluginCoreFile;
    }
}
