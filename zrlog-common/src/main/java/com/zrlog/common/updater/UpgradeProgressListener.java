package com.zrlog.common.updater;

import java.util.Map;

public interface UpgradeProgressListener {

    UpgradeProgressListener NONE = (event, data) -> {
    };

    void onProgress(String event, Map<String, Object> data) throws Exception;
}
