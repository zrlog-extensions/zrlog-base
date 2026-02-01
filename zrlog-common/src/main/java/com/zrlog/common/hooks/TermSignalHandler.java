package com.zrlog.common.hooks;

import com.zrlog.common.ZrLogConfig;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class TermSignalHandler implements SignalHandler {

    private final ZrLogConfig zrLogConfig;

    public TermSignalHandler(ZrLogConfig zrLogConfig) {
        this.zrLogConfig = zrLogConfig;
    }

    @Override
    public void handle(Signal signal) {
        zrLogConfig.stop();
    }
}
