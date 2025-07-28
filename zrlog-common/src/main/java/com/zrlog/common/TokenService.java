package com.zrlog.common;

import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.zrlog.common.vo.AdminFullTokenVO;

public interface TokenService {

    long DEFAULT_SESSION_TIMEOUT = 1000 * 60 * 60 * 24L;

    String ADMIN_LOGIN_URI_PATH = Constants.ADMIN_URI_BASE_PATH + "/login";

    AdminFullTokenVO getAdminTokenVO(HttpRequest request);

    void removeAdminToken(HttpRequest request, HttpResponse response);

    void setAdminToken(Integer userId, String secretKey, String sessionId, String protocol, HttpRequest request, HttpResponse response) throws Exception;
}
