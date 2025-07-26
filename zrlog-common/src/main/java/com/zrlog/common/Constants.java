package com.zrlog.common;

import com.hibegin.common.util.BooleanUtils;
import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.StringUtils;

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
    public static final int DEFAULT_ARTICLE_DIGEST_LENGTH = 200;
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

    public static String getTemplatePath() {
        return Constants.getStringByFromWebSite("template", Constants.DEFAULT_TEMPLATE_PATH);
    }

    public static String getZrLogHome() {
        if (Constants.getZrLogHomeByEnv() == null) {
            return System.getProperty("user.dir");
        } else {
            return Constants.getZrLogHomeByEnv();
        }
    }

    public static boolean isStaticHtmlStatus() {
        return getBooleanByFromWebSite("generator_html_status");
    }

    public static String getArticleUri() {
        return "";
    }

    public static boolean getBooleanByFromWebSite(String key) {
        Object dbSetting = getStringByFromWebSite(key);
        return websiteValueIsTrue(dbSetting);
    }

    public static String getStringByFromWebSite(String key) {
        return getStringByFromWebSite(key, null);
    }

    public static String getHost() {
        return getStringByFromWebSite("host", "");
    }

    public static String getStringByFromWebSite(String key, String defaultValue) {
        CacheService<?> cacheService = zrLogConfig.getCacheService();
        if (Objects.isNull(cacheService)) {
            return "";
        }
        Object dbSetting = cacheService.getPublicWebSiteInfoFirstByCache(key);
        if (Objects.isNull(dbSetting)) {
            return defaultValue;
        }
        return dbSetting + "";
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

    public static boolean isAllowComment() {
        return !Constants.getBooleanByFromWebSite("disable_comment_status");
    }

    public static long getDefaultRows() {
        return (long) Double.parseDouble(Constants.getStringByFromWebSite("rows", "10"));
    }

    public static String getAppId() {
        return String.valueOf(Constants.getStringByFromWebSite("appId"));
    }

    public static void init() {
        System.getProperties().put("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");
    }

    public static String getLanguage() {
        return Constants.getStringByFromWebSite("language");
    }
}