package com.zrlog.web;

public interface WebSetupProvider {

    String name();

    default int order() {
        return 100;
    }

    WebSetup create(WebSetupContext context);
}
