package com.zrlog.blog.web.template;

import org.junit.Test;

import java.util.StringJoiner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TemplateRenderUtilsTest {

    @Test
    public void shouldAppendListTypeAndNameToTitle() {
        StringJoiner title = new StringJoiner(" - ");

        TemplateRenderUtils.appendListPageTitle(title, "Category", "Java");

        assertEquals("Category - Java", title.toString());
    }

    @Test
    public void shouldSkipMissingListTitleParts() {
        StringJoiner title = new StringJoiner(" - ");

        TemplateRenderUtils.appendListPageTitle(title, "Tags", "");
        TemplateRenderUtils.appendListPageTitle(title, null, null);

        assertEquals("Tags", title.toString());
    }

    @Test
    public void shouldResolveNotFoundPageTitle() {
        assertFalse(TemplateRenderUtils.notFoundPageTitle().isEmpty());
    }
}
