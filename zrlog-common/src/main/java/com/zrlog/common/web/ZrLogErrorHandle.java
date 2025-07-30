package com.zrlog.common.web;

import com.hibegin.common.util.LoggerUtil;
import com.hibegin.http.server.api.HttpErrorHandle;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.execption.NotFindResourceException;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.common.Constants;
import com.zrlog.common.exception.AbstractBusinessException;
import com.zrlog.common.exception.NotFindDbEntryException;
import com.zrlog.common.rest.response.ApiStandardResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 全局异常处理
 */
public class ZrLogErrorHandle implements HttpErrorHandle {

    private static final Logger LOGGER = LoggerUtil.getLogger(ZrLogErrorHandle.class);

    private final int httpStatueCode;

    public ZrLogErrorHandle(int httpStatueCode) {
        this.httpStatueCode = httpStatueCode;
    }

    /**
     * @param request
     * @param response
     * @param e
     */
    @Override
    public void doHandle(HttpRequest request, HttpResponse response, Throwable e) {
        if (Constants.debugLoggerPrintAble()) {
            LOGGER.log(Level.SEVERE, "handle " + request.getUri() + " error", e);
        } else {
            LOGGER.log(Level.WARNING, "handle " + request.getUri() + " error " + e.getMessage());
        }
        if (request.getUri().startsWith("/api")) {
            if (e instanceof AbstractBusinessException) {
                AbstractBusinessException ee = (AbstractBusinessException) e;
                ApiStandardResponse<Void> error = new ApiStandardResponse<>();
                error.setError(ee.getError());
                error.setMessage(ee.getMessage());
                response.renderJson(error);
            } else if (e instanceof NotFindResourceException) {
                ApiStandardResponse<Void> error = new ApiStandardResponse<>();
                error.setError(9404);
                error.setMessage(e.getMessage());
                response.renderJson(error);
            } else {
                ApiStandardResponse<Void> error = new ApiStandardResponse<>();
                error.setError(9999);
                error.setMessage(e.getMessage());
                response.renderJson(error);
            }
            return;
        }
        if (request.getUri().startsWith(Constants.ADMIN_URI_BASE_PATH)) {
            if (e instanceof NotFindResourceException || e instanceof NotFindDbEntryException) {
                response.redirect(Constants.ADMIN_URI_BASE_PATH + "/404?queryString=" + request.getQueryStr() + "&uriPath=" + request.getUri() + "&message=" + e.getMessage());
            }
            response.redirect(Constants.ADMIN_URI_BASE_PATH + "/admin/500?message=" + e.getMessage());
            return;
        }
        InputStream errorInputStream = getErrorInputStream(e);
        if (Objects.isNull(errorInputStream)) {
            response.renderCode(500);
            return;
        }
        response.write(errorInputStream, httpStatueCode);
    }

    private InputStream getErrorInputStream(Throwable e) {
        if (Constants.debugLoggerPrintAble()) {
            return new ByteArrayInputStream(("<pre style='color:red'>" + LoggerUtil.recordStackTraceMsg(e) + "</pre>").getBytes());
        }
        InputStream inputStream = PathUtil.getConfInputStream("/error/" + httpStatueCode + ".html");
        if (Objects.nonNull(inputStream)) {
            return inputStream;
        }
        return PathUtil.getConfInputStream("/error/500.html");
    }
}