package com.zrlog.business.template.util;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class BlogResourceUtilsTest {

    @Test
    public void shouldExposeResourceLookupsFromClasspathIndex() {
        BlogResourceUtils utils = BlogResourceUtils.getInstance();
        List<String> resources = utils.getResources();

        assertEquals(resources.contains("missing"), utils.existsResource("missing"));
        assertEquals(resources.stream().filter(e -> e.startsWith("/")).collect(Collectors.toList()),
                utils.searchResources("/"));
    }
}
