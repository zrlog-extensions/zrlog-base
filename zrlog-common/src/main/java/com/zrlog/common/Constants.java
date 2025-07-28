package com.zrlog.common;

import com.hibegin.common.util.BooleanUtils;
import com.hibegin.common.util.EnvKit;
import com.zrlog.common.vo.PublicWebSiteInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 存放全局的静态变量，有多个地方使用一个key时，存放在这里，方便代码的维护。
 */
public class Constants {

    public static final String ADMIN_URI_BASE_PATH = "/admin";
    public static final String TEMPLATE_BASE_PATH = "/include/templates/";
    public static final String DEFAULT_TEMPLATE_PATH = TEMPLATE_BASE_PATH + "default";
    public static final long DEFAULT_ARTICLE_DIGEST_LENGTH = 200;
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ssXXX";
    public static ZrLogConfig zrLogConfig;
    private static volatile long lastAccessTime = System.currentTimeMillis();

    /**
     * 处理静态化文件,仅仅缓存文章页(变化较小)
     */
    public static boolean catGeneratorHtml(String targetUri) {
        if (!Constants.isStaticHtmlStatus()) {
            return false;
        }
        return "/".equals(targetUri) || (targetUri.startsWith("/" + Constants.getArticleUri()) && targetUri.endsWith(".html"));
    }

    static {
        init();
    }

    public static long getLastAccessTime() {
        return lastAccessTime;
    }

    public static void setLastAccessTime(long lastAccessTime) {
        Constants.lastAccessTime = lastAccessTime;
    }

    public static boolean debugLoggerPrintAble() {
        return EnvKit.isDevMode();
    }

    public static String getZrLogHomeByEnv() {
        return System.getenv().get("ZRLOG_HOME");
    }

    public static String getZrLogHome() {
        if (Constants.getZrLogHomeByEnv() == null) {
            return System.getProperty("user.dir");
        } else {
            return Constants.getZrLogHomeByEnv();
        }
    }

    public static boolean isStaticHtmlStatus() {
        CacheService<?> cacheService = zrLogConfig.getCacheService();
        if (Objects.isNull(cacheService)) {
            return false;
        }
        PublicWebSiteInfo publicWebSiteInfo = cacheService.getPublicWebSiteInfo();
        if (Objects.isNull(publicWebSiteInfo)) {
            return false;
        }
        return publicWebSiteInfo.getGenerator_html_status();
    }

    public static String getArticleUri() {
        return "";
    }

    public static String getHost() {
        CacheService<?> cacheService = zrLogConfig.getCacheService();
        if (Objects.isNull(cacheService)) {
            return "";
        }
        if (Objects.isNull(cacheService.getPublicWebSiteInfo())) {
            return "";
        }
        return cacheService.getPublicWebSiteInfo().getHost();
    }

    public static boolean websiteValueIsTrue(Object dbSetting) {
        if (Objects.isNull(dbSetting)) {
            return false;
        }
        if (dbSetting instanceof Boolean) {
            return (boolean) dbSetting;
        }
        if (dbSetting instanceof Number) {
            return ((Number) dbSetting).intValue() == 1;
        }
        return dbSetting instanceof String && ("1".equals(dbSetting) || ("1.0".equals(dbSetting) || "on".equals(dbSetting) || BooleanUtils.isTrue((String) dbSetting)));
    }

    public static List<String> articleRouterList() {
        return Collections.singletonList("/");
    }

    public static void init() {
        System.getProperties().put("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");
    }

    public static String getLanguage() {
        CacheService<?> cacheService = zrLogConfig.getCacheService();
        if (Objects.isNull(cacheService)) {
            return "zh_CN";
        }
        PublicWebSiteInfo publicWebSiteInfo = cacheService.getPublicWebSiteInfo();
        if (Objects.isNull(publicWebSiteInfo)) {
            return "zh_CN";
        }
        return publicWebSiteInfo.getLanguage();
    }
}