package com.zrlog.web.inteceptor;

import com.hibegin.http.server.api.HandleAbleInterceptor;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.zrlog.common.Constants;

import java.util.Objects;

/**
 * 全局拦截对程序状态的处理，访问非安装页时都统一跳转到 /install 下
 */
public class InstallStatusInterceptor implements HandleAbleInterceptor {
    @Override
    public boolean isHandleAble(HttpRequest request) {
        String uri = request.getUri();
        return !Objects.equals(uri, "/install") && !uri.startsWith("/install/");
    }

    @Override
    public boolean doInterceptor(HttpRequest request, HttpResponse response) {
        String target = request.getUri();
        if (!Constants.zrLogConfig.isInstalled()) {
            response.redirect("/install?ref=" + target);
            return false;
        }
        if (Constants.zrLogConfig.isMissingConfig()) {
            response.redirect("/install?ref=" + target + "&missingConfig=true");
            return false;
        }
        return true;
    }
}
