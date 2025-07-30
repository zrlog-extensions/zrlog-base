package com.zrlog.common.exception;

import com.zrlog.util.I18nUtil;

public class UnknownException extends AbstractBusinessException {

    public UnknownException() {
        super();
    }

    public UnknownException(Throwable cause) {
        super(cause);
    }

    @Override
    public int getError() {
        return 9999;
    }

    @Override
    public String getMessage() {
        return I18nUtil.getBackendStringFromRes("unknownError");
    }
}
