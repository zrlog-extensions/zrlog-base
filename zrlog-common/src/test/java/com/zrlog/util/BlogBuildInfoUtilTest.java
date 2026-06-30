package com.zrlog.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BlogBuildInfoUtilTest {

    @Test
    public void shouldExposeBuildPropertiesSnapshot() {
        Properties properties = BlogBuildInfoUtil.getBlogProp();

        assertEquals(BlogBuildInfoUtil.getVersion(), properties.get("version"));
        assertEquals(BlogBuildInfoUtil.getBuildId(), properties.get("buildId"));
        assertEquals(BlogBuildInfoUtil.getRunMode(), properties.get("runMode"));
        assertEquals(new SimpleDateFormat("yyyy-MM-dd").format(BlogBuildInfoUtil.getTime()),
                properties.get("buildTime"));
    }

    @Test
    public void shouldFormatVersionInformation() {
        assertEquals(BlogBuildInfoUtil.getVersion() + " - " + BlogBuildInfoUtil.getBuildId(),
                BlogBuildInfoUtil.getVersionShortInfo());
        assertTrue(BlogBuildInfoUtil.getVersionInfo().startsWith(BlogBuildInfoUtil.getVersionShortInfo()));
        assertTrue(BlogBuildInfoUtil.getVersionInfoFull().startsWith(BlogBuildInfoUtil.getVersionShortInfo()));
        assertTrue(BlogBuildInfoUtil.getVersionInfoFull().contains(":"));
    }

    @Test
    public void shouldExposeRunModeAndPackageDefaults() {
        assertNotNull(BlogBuildInfoUtil.getFileArch());
        assertNotNull(BlogBuildInfoUtil.getRuntimeType());
        assertNotNull(BlogBuildInfoUtil.getPackageType());
        assertNotNull(BlogBuildInfoUtil.getUpdateVersionJsonFilename());
        assertFalse(BlogBuildInfoUtil.getResourceDownloadUrl().endsWith("/"));

        String runMode = BlogBuildInfoUtil.getRunMode();
        assertEquals("RELEASE".equalsIgnoreCase(runMode), BlogBuildInfoUtil.isRelease());
        assertEquals("PREVIEW".equalsIgnoreCase(runMode), BlogBuildInfoUtil.isPreview());
        assertEquals("DEV".equalsIgnoreCase(runMode), BlogBuildInfoUtil.isDev());
    }
}
