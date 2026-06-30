package com.zrlog.business.plugin;

import com.hibegin.common.dao.DataSourceWrapper;
import com.zrlog.business.service.WebsiteKvService;
import com.zrlog.business.support.InMemoryZrLogDatabase;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PluginLifecycleContractTest {

    @Test
    public void shouldClosePluginConsoleAndDeleteOutputFile() throws Exception {
        File output = Files.createTempFile("zrlog-plugin-console", ".log").toFile();
        File marker = Files.createTempFile("zrlog-plugin-server", ".lock").toFile();
        Files.writeString(output.toPath(), "log", StandardCharsets.UTF_8);

        new PluginConsole(output, marker, false).close();

        assertFalse(output.exists());
        assertTrue(marker.exists());
    }

    @Test
    public void shouldPrintPluginConsoleOutputIncrementally() throws Exception {
        File output = Files.createTempFile("zrlog-plugin-console", ".log").toFile();
        File marker = Files.createTempFile("zrlog-plugin-server", ".lock").toFile();
        PluginConsole console = new PluginConsole(output, marker, false);
        PrintStream previousOut = System.out;
        ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outputCapture, true, StandardCharsets.UTF_8.name()));

            Files.writeString(output.toPath(), "first", StandardCharsets.UTF_8);
            console.printAsync();
            waitUntil(() -> captured(outputCapture).contains("first"));
            Files.writeString(output.toPath(), " second", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
            waitUntil(() -> captured(outputCapture).contains("first second"));
        } finally {
            console.close();
            System.setOut(previousOut);
        }

        assertFalse(output.exists());
        assertTrue(marker.exists());
    }

    @Test
    public void shouldPrintInvalidJarErrorAndDeleteServerMarker() throws Exception {
        File output = Files.createTempFile("zrlog-plugin-console-error", ".log").toFile();
        File marker = Files.createTempFile("zrlog-plugin-server", ".lock").toFile();
        PluginConsole console = new PluginConsole(output, marker, true);
        PrintStream previousErr = System.err;
        ByteArrayOutputStream errorCapture = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(errorCapture, true, StandardCharsets.UTF_8.name()));

            Files.writeString(output.toPath(), "Error: Invalid or corrupt jarfile plugin-core.jar",
                    StandardCharsets.UTF_8);
            console.printAsync();
            waitUntil(() -> captured(errorCapture).contains("Invalid or corrupt jarfile")
                    && !marker.exists());
        } finally {
            console.close();
            System.setErr(previousErr);
        }

        assertFalse(output.exists());
        assertFalse(marker.exists());
    }

    @Test
    public void shouldPrintNormalErrorStreamWithoutDeletingServerMarker() throws Exception {
        File output = Files.createTempFile("zrlog-plugin-console-stderr", ".log").toFile();
        File marker = Files.createTempFile("zrlog-plugin-server", ".lock").toFile();
        PluginConsole console = new PluginConsole(output, marker, true);
        PrintStream previousErr = System.err;
        ByteArrayOutputStream errorCapture = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(errorCapture, true, StandardCharsets.UTF_8.name()));

            Files.writeString(output.toPath(), "plain error", StandardCharsets.UTF_8);
            console.printAsync();
            waitUntil(() -> captured(errorCapture).contains("plain error"));
        } finally {
            console.close();
            System.setErr(previousErr);
        }

        assertFalse(output.exists());
        assertTrue(marker.exists());
    }

    @Test
    public void shouldShutdownPluginConsoleWhenOutputFileIsMissing() throws Exception {
        File output = Files.createTempFile("zrlog-plugin-console-missing", ".log").toFile();
        File marker = Files.createTempFile("zrlog-plugin-server", ".lock").toFile();
        assertTrue(output.delete());
        PluginConsole console = new PluginConsole(output, marker, false);
        ScheduledExecutorService scheduler = scheduler(console);

        console.printAsync();
        waitUntil(scheduler::isShutdown);

        assertFalse(output.exists());
        assertTrue(marker.exists());
    }

    @Test
    public void shouldExposeCacheManagerLifecycleWithoutStartingScheduler() throws Exception {
        CacheManagerPlugin plugin = new CacheManagerPlugin(new TestZrLogConfig());

        assertTrue(plugin.autoStart());
        assertFalse(plugin.isStarted());
        assertTrue(plugin.stop());
        assertFalse(plugin.isStarted());
    }

    @Test
    public void shouldRefreshInitDataWhenCacheTimeoutExpiresUsingDatabaseConfig() throws Exception {
        long previousLastAccessTime = Constants.getLastAccessTime();
        AtomicInteger refreshCount = new AtomicInteger();
        try (InMemoryZrLogDatabase ignored = InMemoryZrLogDatabase.open()) {
            new WebsiteKvService().putString("cache_timeout_minutes", "0.001");
            CacheManagerPlugin plugin = new CacheManagerPlugin(new TestZrLogConfig(countingCacheService(refreshCount)));
            try {
                Constants.setLastAccessTime(System.currentTimeMillis() - 1_000);

                assertTrue(plugin.start());
                assertTrue(plugin.start());
                waitUntil(() -> refreshCount.get() > 0);

                assertTrue(plugin.isStarted());
            } finally {
                assertTrue(plugin.stop());
            }
        } finally {
            Constants.setLastAccessTime(previousLastAccessTime);
        }

        assertEquals(1, refreshCount.get());
    }

    private static String captured(ByteArrayOutputStream outputCapture) {
        return new String(outputCapture.toByteArray(), StandardCharsets.UTF_8);
    }

    private static ScheduledExecutorService scheduler(PluginConsole console) throws Exception {
        Field scheduler = PluginConsole.class.getDeclaredField("scheduler");
        scheduler.setAccessible(true);
        return (ScheduledExecutorService) scheduler.get(console);
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Timed out waiting for async plugin lifecycle action");
    }

    private static class TestZrLogConfig extends ZrLogConfig {

        private final CacheService cacheService;

        TestZrLogConfig() throws Exception {
            this(nullCacheService());
        }

        TestZrLogConfig(CacheService cacheService) throws Exception {
            super(19083, null, "/");
            this.cacheService = cacheService;
        }

        @Override
        public boolean isInstalled() {
            return false;
        }

        @Override
        public DataSourceWrapper configDatabase() {
            return null;
        }

        @Override
        protected TokenService initTokenService() {
            return null;
        }

        @Override
        public CacheService getCacheService() {
            return cacheService;
        }

        @Override
        public List<IPlugin> getBasePluginList() {
            return new Plugins();
        }

        private static CacheService nullCacheService() {
            return (CacheService) java.lang.reflect.Proxy.newProxyInstance(
                    PluginLifecycleContractTest.class.getClassLoader(),
                    new Class[]{CacheService.class},
                    (proxy, method, args) -> {
                        if ("getCurrentSqlVersion".equals(method.getName()) || "getWebSiteVersion".equals(method.getName())) {
                            return 0L;
                        }
                        if ("toString".equals(method.getName())) {
                            return "CacheServiceProxy";
                        }
                        return null;
                    });
        }
    }

    private static CacheService countingCacheService(AtomicInteger refreshCount) {
        return (CacheService) java.lang.reflect.Proxy.newProxyInstance(
                PluginLifecycleContractTest.class.getClassLoader(),
                new Class[]{CacheService.class},
                (proxy, method, args) -> {
                    if ("refreshInitData".equals(method.getName())) {
                        refreshCount.incrementAndGet();
                        return new BaseDataInitVO();
                    }
                    if ("getCurrentSqlVersion".equals(method.getName()) || "getWebSiteVersion".equals(method.getName())) {
                        return 0L;
                    }
                    if ("toString".equals(method.getName())) {
                        return "CountingCacheServiceProxy";
                    }
                    return null;
                });
    }
}
