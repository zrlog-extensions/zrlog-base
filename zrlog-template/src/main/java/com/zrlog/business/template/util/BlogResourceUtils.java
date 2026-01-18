package com.zrlog.business.template.util;

import com.hibegin.common.util.IOUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BlogResourceUtils {

    public static List<String> getResources() {
        return Arrays.stream(IOUtil.getStringInputStream(BlogResourceUtils.class.getResourceAsStream("/resource.txt")).split("\n")).map(String::trim).collect(Collectors.toList());
    }

    public static List<String> searchResources(String prefix) {
        return getResources().stream().filter(e -> e.startsWith(prefix)).collect(Collectors.toList());
    }
}
