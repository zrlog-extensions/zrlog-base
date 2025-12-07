package com.zrlog.data.exception;

import com.zrlog.common.exception.AbstractBusinessException;
import com.zrlog.util.I18nUtil;

public class DAOException extends AbstractBusinessException {
    public DAOException() {
        super();
    }

    public DAOException(Throwable cause) {
        super(cause);
    }

    @Override
    public int getError() {
        return 9101;
    }

    @Override
    public String getMessage() {
        return I18nUtil.getBackendStringFromRes("dbUnknownError");
    }
}
