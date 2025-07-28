package com.zrlog.data.util;

import com.hibegin.common.dao.ResultValueConvertUtils;
import com.hibegin.common.util.StringUtils;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.vo.PublicWebSiteInfo;

import java.util.Objects;

public class WebSiteUtils {

    public static PublicWebSiteInfo fillDefaultInfo(PublicWebSiteInfo info) {
        if (Objects.isNull(info.getRows())) {
            info.setRows(10L);
        }
        if (Objects.isNull(info.getArticle_auto_digest_length())) {
            info.setArticle_auto_digest_length(Constants.DEFAULT_ARTICLE_DIGEST_LENGTH);
        }
        if (Objects.isNull(info.getSession_timeout())) {
            info.setArticle_auto_digest_length(TokenService.DEFAULT_SESSION_TIMEOUT / 60 / 1000);
        }
        info.setAdmin_darkMode(Objects.equals(info.getAdmin_darkMode(), true));
        info.setDisable_comment_status(Objects.equals(info.getDisable_comment_status(), true));
        info.setGenerator_html_status(Objects.equals(info.getGenerator_html_status(), true));
        info.setArticle_thumbnail_status(Objects.equals(info.getArticle_thumbnail_status(), true));
        info.setComment_plugin_status(Objects.equals(info.getComment_plugin_status(), true));
        if (StringUtils.isEmpty(info.getAdmin_color_primary())) {
            info.setAdmin_color_primary(Constants.DEFAULT_COLOR_PRIMARY_COLOR);
        }
        if (StringUtils.isEmpty(info.getLanguage())) {
            info.setLanguage(Constants.DEFAULT_LANGUAGE);
        }
        if (StringUtils.isEmpty(info.getTemplate())) {
            info.setTemplate(Constants.DEFAULT_TEMPLATE_PATH);
        }
        boolean changyanStatus = ResultValueConvertUtils.toBoolean(info.getChangyan_status());
        if (changyanStatus) {
            if (StringUtils.isEmpty(info.getComment_plugin_name())) {
                info.setComment_plugin_name("changyan");
                info.setComment_plugin_status(true);
            }
        }
        return info;
    }
}
