package com.zrlog.common.exception;

import com.zrlog.util.I18nUtil;

public class ResourceLockedException extends AbstractBusinessException {

    @Override
    public int getError() {
        return 9100;
    }

    @Override
    public String getMessage() {
        return I18nUtil.getBackendStringFromRes("resourceBusy");
    }
}
