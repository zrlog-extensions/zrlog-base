package com.zrlog.business.util;

import com.hibegin.http.server.util.NativeImageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceNativeImageUtils {

    public static void regResource() throws IOException {
        List<String> resourceFiles = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String filePath = "/conf/update-sql/" + i + ".sql";
            InputStream inputStream = ServiceNativeImageUtils.class.getResourceAsStream(filePath);
            if (Objects.nonNull(inputStream)) {
                resourceFiles.add(filePath);
            }
        }
        resourceFiles.add("/i18n/backend_en_US.properties");
        resourceFiles.add("/i18n/backend_zh_CN.properties");
        resourceFiles.add("/zrlog.properties");
        resourceFiles.add("/conf/website-key-public.json");

        NativeImageUtils.doResourceLoadByResourceNames(resourceFiles);
    }
}
