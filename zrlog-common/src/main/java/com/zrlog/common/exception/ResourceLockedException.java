package com.zrlog.common.exception;

public class ResourceLockedException extends AbstractBusinessException {

    @Override
    public int getError() {
        return 9100;
    }
}
