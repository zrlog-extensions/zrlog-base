package com.zrlog.business.service;

import com.hibegin.common.util.StringUtils;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.business.rest.response.PublicInfoVO;
import com.zrlog.common.Constants;
import com.zrlog.util.BlogBuildInfoUtil;
import com.zrlog.util.ZrLogUtil;

import java.util.Objects;

public class CommonService {

    public PublicInfoVO getPublicInfo(HttpRequest request) {
        boolean darkMode = Objects.equals(Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo().getAdmin_darkMode(), true);
        String themeColor;
        String adminColor = Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo().getAdmin_color_primary();
        if (StringUtils.isEmpty(adminColor)) {
            adminColor = Constants.DEFAULT_COLOR_PRIMARY_COLOR;
        }
        if (darkMode) {
            themeColor = "#000000";
        } else {
            themeColor = adminColor;
        }
        String appId = Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo().getAppId();
        return new PublicInfoVO(BlogBuildInfoUtil.getVersion(), Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo().getTitle(), ZrLogUtil.getHomeUrlWithHost(request), darkMode, adminColor, themeColor, appId);
    }


}
