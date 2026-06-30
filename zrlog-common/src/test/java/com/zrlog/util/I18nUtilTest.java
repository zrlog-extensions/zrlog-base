package com.zrlog.util;

import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.common.Constants;
import com.zrlog.common.vo.I18nVO;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;

public class I18nUtilTest {

    @Test
    public void shouldLoadClasspathI18nForNonDefaultTemplate() {
        try {
            I18nVO zh = I18nUtil.addToRequestWithTemplatePath(
                    Constants.TEMPLATE_BASE_PATH + "acceptance", request(null));
            assertEquals("classpath-zh", zh.getBlog().get("zh_CN").get("acceptanceOnly"));
            assertEquals("zh_CN", zh.getLocale());

            I18nVO en = I18nUtil.addToRequestWithTemplatePath(
                    Constants.TEMPLATE_BASE_PATH + "acceptance", request("en-US,en;q=0.9"));
            assertEquals("classpath-en", en.getBlog().get("en_US").get("acceptanceOnly"));
            assertEquals("en_US", en.getLocale());
        } finally {
            I18nUtil.threadLocal.remove();
        }
    }

    private static HttpRequest request(String acceptLanguage) {
        return (HttpRequest) Proxy.newProxyInstance(
                I18nUtilTest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getUri".equals(method.getName())) {
                        return "/article";
                    }
                    if ("getHeader".equals(method.getName()) && "Accept-Language".equals(args[0])) {
                        return acceptLanguage;
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                });
    }
}
