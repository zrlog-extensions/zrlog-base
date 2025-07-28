package com.zrlog.business.plugin;

import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.config.ResponseConfig;
import com.hibegin.http.server.impl.SimpleHttpResponse;
import com.zrlog.common.Constants;

import java.io.File;
import java.io.FileOutputStream;

public class BodySaveResponse extends SimpleHttpResponse {

    private final File cacheFile;

    public BodySaveResponse(HttpRequest request, ResponseConfig responseConfig) {
        super(request, responseConfig);
        this.cacheFile = buildCacheFile();
        this.cacheFile.getParentFile().mkdirs();
    }

    private File buildCacheFile() {
        StaticSitePlugin staticSitePlugin = Constants.zrLogConfig.getPlugin(StaticSitePlugin.class);
        File cacheFile = staticSitePlugin.loadCacheFile(request);
        if (request.getUri().startsWith("/admin") && !request.getUri().contains(".")) {
            cacheFile = new File(cacheFile + ".html");
        }
        return cacheFile;
    }

    @Override
    protected byte[] toChunkedBytes(byte[] inputBytes) {
        return inputBytes;
    }

    @Override
    protected void send(byte[] bytes, boolean body, boolean close) {
        super.send(bytes, body, close);
        if (body) {
            try {
                try (FileOutputStream fileOutputStream = new FileOutputStream(cacheFile, true)) {
                    fileOutputStream.write(bytes);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public File getCacheFile() {
        return cacheFile;
    }
}
