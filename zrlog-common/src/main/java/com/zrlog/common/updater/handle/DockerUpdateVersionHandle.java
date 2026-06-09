package com.zrlog.common.updater.handle;

import com.zrlog.common.updater.UpdateVersionHandler;

import java.util.Map;

public class DockerUpdateVersionHandle implements UpdateVersionHandler {

    private String message;
    private boolean finish;
    private final Map<String, Object> blogRes;

    public DockerUpdateVersionHandle(Map<String, Object> blogRes) {
        this.blogRes = blogRes;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isFinish() {
        return finish;
    }

    @Override
    public void doHandle() {
        message = blogRes.get("upgrade.tips.docker").toString();
        finish = true;
    }
}
