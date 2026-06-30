package com.zrlog.business.type;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemplateTypeTest {

    @Test
    public void shouldExposeTemplateMetadataFiles() {
        assertEquals("template.properties", TemplateType.STANDARD.getInfoFile());
        assertEquals("setting/config-form.json", TemplateType.STANDARD.getConfigFile());
        assertEquals("package.json", TemplateType.NODE_JS.getInfoFile());
        assertEquals("_config.yml", TemplateType.NODE_JS.getConfigFile());
    }
}
