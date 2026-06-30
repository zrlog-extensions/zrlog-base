package com.zrlog.business.service;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class DbUpgradeServiceTest {

    @Test
    public void shouldResolveDatabaseNameFromCommonJdbcUrls() throws Exception {
        assertEquals("blog", resolveDbName("jdbc:mysql://127.0.0.1:3306/blog?useSSL=false"));
        assertEquals("zrlog_base", resolveDbName("jdbc:h2:mem:zrlog_base;MODE=MySQL"));
        assertEquals("zrlog", resolveDbName("jdbc:sqlite:/var/lib/zrlog/zrlog"));
        assertEquals("broken url", resolveDbName("jdbc:broken url"));
        assertEquals("", resolveDbName(""));
        assertEquals("", resolveDbName(null));
    }

    private static String resolveDbName(String jdbcUrl) throws Exception {
        Method method = DbUpgradeService.class.getDeclaredMethod("resolveDbName", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, jdbcUrl);
    }
}
