package com.zrlog.common.updater;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class UpgradeProgressEvent {

    /**
     * Upgrade SSE uses one event name. The payload stage tells the client how to render or replace the line.
     */
    public static final String EVENT = "upgrade-progress";
    public static final String STAGE_DOWNLOAD = "download";
    public static final String STAGE_VALIDATE = "validate";
    public static final String STAGE_UPLOAD = "upload";
    public static final String STAGE_UPDATE = "update";
    public static final String STAGE_BACKUP = "backup";
    public static final String STAGE_MERGE = "merge";
    public static final String STAGE_UNZIP = "unzip";
    public static final String STAGE_RESTART = "restart";
    public static final String STAGE_COMPLETE = "complete";
    public static final String STAGE_MANUAL = "manual";
    public static final String STAGE_ERROR = "error";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_COMPLETE = "complete";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_MANUAL = "manual";

    private UpgradeProgressEvent() {
    }

    public static Map<String, Object> data(String stage, String message) {
        return data(stage, STATUS_RUNNING, message, null);
    }

    public static Map<String, Object> data(String stage, String message, String detail) {
        return data(stage, STATUS_RUNNING, message, detail);
    }

    public static Map<String, Object> data(String stage, String status, String message, String detail) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", stage);
        data.put("status", status);
        data.put("message", Objects.toString(message, ""));
        if (Objects.nonNull(detail) && !detail.isBlank()) {
            data.put("detail", detail);
        }
        return data;
    }
}
