package com.zrlog.business.plugin;

import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.config.ResponseConfig;
import com.hibegin.http.server.impl.SimpleHttpResponse;
import com.zrlog.common.Constants;

import java.io.*;
import java.util.Objects;

public class BodySaveResponse extends SimpleHttpResponse implements AutoCloseable {

    private final OutputStream outputStream;
    private final File cacheFile;

    public BodySaveResponse(HttpRequest request, ResponseConfig responseConfig) {
        super(request, responseConfig);
        this.cacheFile = buildCacheFile();
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
        cacheFile.getParentFile().mkdirs();
        try {
            this.outputStream = new FileOutputStream(cacheFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
    protected boolean needChunked(InputStream inputStream, long bodyLength) {
        return false;
    }

    @Override
    protected void send(byte[] bytes, boolean body, boolean close) {
        if (body) {
            try {
                outputStream.write(bytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public File getCacheFile() {
        return cacheFile;
    }

    @Override
    public void close() throws Exception {
        if (Objects.nonNull(this.outputStream)) {
            this.outputStream.close();
        }
    }
}
