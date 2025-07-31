package com.zrlog.business.service;

import com.hibegin.common.dao.ResultValueConvertUtils;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.hibegin.common.util.BeanUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.common.util.UrlEncodeUtils;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.blog.web.util.WebTools;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.common.Constants;
import com.zrlog.data.cache.vo.BaseDataInitVO;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.model.Log;
import com.zrlog.util.ParseUtil;
import com.zrlog.util.ZrLogUtil;

import java.util.List;
import java.util.Objects;

/**
 * 与文章相关的业务代码
 */
public class VisitorArticleService {

    /**
     * 高亮用户检索的关键字
     */
    public static void wrapperSearchKeyword(PageData<ArticleBasicDTO> data, String keywords) {
        if (StringUtils.isEmpty(keywords)) {
            return;
        }
        List<ArticleBasicDTO> logs = data.getRows();
        if (logs == null || logs.isEmpty()) {
            return;
        }
        for (ArticleBasicDTO log : logs) {
            String title = log.getTitle();
            String content = log.getContent();
            String digest = log.getDigest();
            log.setTitle(ParseUtil.wrapperKeyword(title, keywords));
            String tryWrapperDigest = ParseUtil.wrapperKeyword(digest, keywords);
            if (tryWrapperDigest != null && tryWrapperDigest.length() != digest.length()) {
                log.setDigest(tryWrapperDigest);
            } else {
                log.setDigest(ParseUtil.wrapperKeyword(ParseUtil.removeHtmlElement(content), keywords));
            }
        }
    }

    public static <T extends ArticleBasicDTO> T handlerArticle(T log, HttpRequest request) {
        String suffix = StaticSitePlugin.getSuffix(request);
        String aliasUrl = UrlEncodeUtils.encodeUrl(log.getAlias()) + suffix;
        log.setAlias(aliasUrl);
        log.setRubbish(ResultValueConvertUtils.toBoolean(log.getRubbish()));
        log.setPrivacy(ResultValueConvertUtils.toBoolean(log.getPrivacy()));
        log.setHot(ResultValueConvertUtils.toBoolean(log.getHot()));
        log.setCanComment(ResultValueConvertUtils.toBoolean(log.getCanComment()) && Objects.equals(Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo().getDisable_comment_status(), false));
        log.setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + log.getAlias()));
        log.setTypeUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + "sort/" + log.getTypeAlias() + suffix));
        log.setNoSchemeUrl(ZrLogUtil.getHomeUrlWithHost(request) + Constants.getArticleUri() + UrlEncodeUtils.encodeUrl(log.getAlias()));
        //
        log.setRecommended(ResultValueConvertUtils.toBoolean(log.getRecommended()));
        log.setReleaseTime(ResultValueConvertUtils.formatDate(log.getReleaseTime(), "yyyy-MM-dd"));
        if (Objects.nonNull(log.getLogId())) {
            log.setId(log.getLogId());
        }
        log.setLastUpdateDate(ResultValueConvertUtils.formatDate(log.getLast_update_date(), "yyyy-MM-dd"));
        log.setLast_update_date(ResultValueConvertUtils.formatDate(log.getLast_update_date(), "yyyy-MM-dd"));
        BaseDataInitVO baseDataInitVO = BeanUtil.cloneObject((BaseDataInitVO) Constants.zrLogConfig.getCacheService().getInitData());

        if (Objects.isNull(log.getDigest())) {
            log.setDigest("");
        }
        if (Objects.isNull(log.getContent())) {
            log.setContent("");
        }
        if (baseDataInitVO.getWebSite().getArticle_thumbnail_status() && StringUtils.isNotEmpty(log.getThumbnail())) {
            log.setThumbnailAlt(ParseUtil.removeHtmlElement(log.getTitle()));
        } else {
            log.setThumbnail(null);
            log.setThumbnailAlt(null);
        }
        return log;
    }

    public PageData<ArticleBasicDTO> pageByKeywords(PageRequest pageRequest, String keywords, HttpRequest request) {
        PageData<ArticleBasicDTO> data = new Log().visitorFind(pageRequest, keywords);
        wrapperSearchKeyword(data, keywords);
        data.setKey(keywords);
        data.getRows().forEach(e -> handlerArticle(e, request));
        return data;
    }
}
