package com.zrlog.util;

import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.web.cookie.Cookie;
import com.zrlog.common.Constants;

import java.util.Objects;

public class TemplateHelper {

    private static String getTemplatePathByCookie(Cookie[] cookies) {
        String previewTemplate = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("template".equals(cookie.getName()) && cookie.getValue().startsWith(Constants.TEMPLATE_BASE_PATH)) {
                    previewTemplate = cookie.getValue();
                    break;
                }
            }
        }
        return previewTemplate;
    }

    /**
     * 获取主题的相对于程序的路径，当Cookie中有值的情况下，优先使用Cookie里面的数据（仅当主题存在的情况下，否则返回默认的主题），
     */
    public static String getTemplatePath(HttpRequest request) {
        String previewTheme = TemplateHelper.getTemplatePathByCookie(request.getCookies());
        if (Objects.nonNull(previewTheme)) {
            return previewTheme;
        }
        return Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo().getTemplate();
    }


}
