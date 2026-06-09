package com.zrlog.blog.web.util;

import com.hibegin.http.server.api.HttpRequest;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class WebToolsTest {

    @Test
    public void shouldPutRealIpHeaderFromForwardedFor() {
        Map<String, String> output = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        WebTools.putRealIpHeader(output, request(headers, "127.0.0.1"));

        assertEquals("203.0.113.10", output.get(WebTools.REAL_IP_HEADER));
    }

    @Test
    public void shouldFallbackToRemoteHostWhenProxyHeadersMissing() {
        Map<String, String> output = new HashMap<>();

        WebTools.putRealIpHeader(output, request(new HashMap<>(), "198.51.100.20"));

        assertEquals("198.51.100.20", output.get(WebTools.REAL_IP_HEADER));
    }

    private HttpRequest request(Map<String, String> headers, String remoteHost) {
        return (HttpRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName())) {
                        return headers.get((String) args[0]);
                    }
                    if ("getHeaderMap".equals(method.getName())) {
                        return headers;
                    }
                    if ("getRemoteHost".equals(method.getName())) {
                        return remoteHost;
                    }
                    if ("toString".equals(method.getName())) {
                        return "HttpRequestProxy";
                    }
                    return null;
                }
        );
    }
}
