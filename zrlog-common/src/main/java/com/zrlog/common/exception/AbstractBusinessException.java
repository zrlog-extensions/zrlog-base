package com.zrlog.common.exception;

public abstract class AbstractBusinessException extends RuntimeException {

    public AbstractBusinessException() {
        super();
    }

    public AbstractBusinessException(Throwable cause) {
        super(cause);
    }

    public abstract int getError();
}
