package com.zrlog.business.util;

import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.business.plugin.type.StaticSiteType;
import com.zrlog.common.Constants;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.util.ThreadUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 缓存工具，便于处理程序缓存和插件缓存
 */
public class CacheUtils {

    private static final Logger LOGGER = LoggerUtil.getLogger(CacheUtils.class);

    public static void updateCache(boolean async, HttpRequest request, List<StaticSiteType> staticSiteTypeList) {
        try {
            BaseDataInitVO initVO = Constants.zrLogConfig.getCacheService().refreshInitData();
            if (async) {
                ThreadUtils.start(() -> {
                    CacheUtils.refreshPluginCacheData(initVO.getVersion() + "", request, staticSiteTypeList);
                });
            } else {
                CacheUtils.refreshPluginCacheData(initVO.getVersion() + "", request, staticSiteTypeList);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Refresh cache error ", e);
        }
    }


    private static int getSyncTimeout() {
        if (EnvKit.isFaaSMode()) {
            //建议配置 FaaS 为最大超时
            return 12 * 60;
        }
        return 3600;
    }

    public static boolean refreshStaticSiteCache(HttpRequest request, List<StaticSiteType> siteTypes) {
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
                    results.add(staticSitePlugin.waitCacheSync(request, getSyncTimeout()));
                }, executorService);
            }).toArray(CompletableFuture[]::new)).join();
            return results.stream().allMatch(e -> Objects.equals(e, Boolean.TRUE));
        } finally {
            executorService.shutdown();
        }
    }

    private static void refreshPluginCacheData(String cacheVersion, HttpRequest request, List<StaticSiteType> staticSiteTypeList) {
        //启动插件
        PluginCorePlugin pluginCorePlugin = Constants.zrLogConfig.getPlugin(PluginCorePlugin.class);
        if (Objects.nonNull(pluginCorePlugin) && !pluginCorePlugin.isStarted()) {
            pluginCorePlugin.start();
        }
        //plugin cache
        if (Objects.nonNull(pluginCorePlugin)) {
            pluginCorePlugin.refreshCache(cacheVersion, request);
        }
        if (!StaticSitePlugin.isDisabled()) {
            refreshStaticSiteCache(request, staticSiteTypeList);
        }
    }
}
