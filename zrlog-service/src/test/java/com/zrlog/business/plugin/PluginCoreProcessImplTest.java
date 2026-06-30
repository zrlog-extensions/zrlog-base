package com.zrlog.business.plugin;

import com.hibegin.http.server.util.PathUtil;
import com.hibegin.common.dao.DataSourceWrapper;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.util.BlogBuildInfoUtil;
import org.graalvm.nativeimage.ImageInfo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PluginCoreProcessImplTest {

    @Test
    public void constructorShouldNotCreatePluginCoreLogFiles() throws Exception {
        Path logDir = Files.createTempDirectory("zrlog-plugin-core-log");
        String previousLogPath = System.getProperty("sws.log.path");
        try {
            System.setProperty("sws.log.path", logDir.toString());

            new PluginCoreProcessImpl(null, "");

            try (Stream<Path> paths = Files.list(logDir)) {
                assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith("plugin-core-")));
            }
        } finally {
            restoreProperty("sws.log.path", previousLogPath);
            delete(logDir);
        }
    }

    @Test
    public void shouldResolveProgramAndPluginWorkerPaths() throws Exception {
        Path root = Files.createTempDirectory("zrlog-plugin-core-root");
        try {
            PathUtil.setRootPath(root.toString());
            PluginCoreProcessImpl process = new PluginCoreProcessImpl(null, "/blog");

            String javaProgram = process.programName(new File("plugin-core.jar"));
            String workerPath = process.getPluginWorkerPath(new File(root + "/conf/plugins"));
            String installedPlugins = process.getInstalledPluginFolder();

            assertTrue(javaProgram.replace("\\", "/").endsWith("/bin/java"));
            assertEquals(new File(root + "/conf").toString(), workerPath);
            assertTrue(installedPlugins.replace("\\", "/").endsWith("/conf/plugins/installed-plugins"));
        } finally {
            delete(root);
        }
    }

    @Test
    public void shouldUsePluginCoreFileAsProgramInNativeImage() throws Exception {
        PluginCoreProcessImpl process = new PluginCoreProcessImpl(null, "/blog");
        File pluginCore = new File("plugin-core-Linux-x86_64.bin");
        try {
            ImageInfo.setInImageRuntimeCode(true);

            assertEquals(pluginCore.toString(), process.programName(pluginCore));
        } finally {
            ImageInfo.setInImageRuntimeCode(false);
        }
    }

    @Test
    public void shouldStartNativePluginCoreExecutableWithExpectedArguments() throws Exception {
        Path root = Files.createTempDirectory("zrlog-plugin-core-native");
        Path argsFile = root.resolve("plugin-args.txt");
        Path pluginCore = root.resolve("conf/plugins/plugin-core-test.bin");
        Files.createDirectories(pluginCore.getParent());
        Files.writeString(pluginCore,
                "#!/bin/sh\nprintf '%s\\n' \"$@\" > " + argsFile + "\n",
                StandardCharsets.UTF_8);
        assertTrue(pluginCore.toFile().setExecutable(true));
        String previousRootPath = System.getProperty("sws.root.path");
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        try {
            System.setProperty("sws.root.path", root.toString());
            Constants.zrLogConfig = new TestZrLogConfig();
            ImageInfo.setInImageRuntimeCode(true);
            PluginCoreProcessImpl process = new PluginCoreProcessImpl(null, "/blog");

            Process started = process.startPluginCore(pluginCore.toFile(), "db=ok",
                    "-Xmx64m", root.resolve("static").toString(), "3.6.0", "token-123",
                    21000, 51000);

            assertNotNull(started);
            assertEquals(0, started.waitFor());
            List<String> args = Files.readAllLines(argsFile);
            assertEquals("21000", args.get(0));
            assertEquals("41000", args.get(1));
            assertEquals("db=ok", args.get(2));
            assertTrue(args.get(3).endsWith("conf/plugins/installed-plugins"));
            assertEquals("51000", args.get(4));
            assertEquals(root.resolve("static").toString(), args.get(5));
            assertEquals("3.6.0", args.get(6));
            assertEquals("19084", args.get(7));
            assertEquals("token-123", args.get(8));
            assertEquals("/blog", args.get(10));
            assertTrue(args.get(11).startsWith("-Duser.dir="));
        } finally {
            ImageInfo.setInImageRuntimeCode(false);
            Constants.zrLogConfig = previousConfig;
            restoreProperty("sws.root.path", previousRootPath);
            delete(root);
        }
    }

    @Test
    public void shouldReturnNullWhenPluginCoreFileIsMissing() throws Exception {
        Path root = Files.createTempDirectory("zrlog-plugin-core-missing");
        String previousRootPath = System.getProperty("sws.root.path");
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        try {
            System.setProperty("sws.root.path", root.toString());
            Constants.zrLogConfig = new TestZrLogConfig();
            PluginCoreProcessImpl process = new PluginCoreProcessImpl(null, "/blog");

            Process started = process.startPluginCore(root.resolve("missing.jar").toFile(),
                    "db=ok", "-Xmx64m", root.resolve("static").toString(), "3.6.0", "token-123",
                    21000, 51000);

            assertEquals(null, started);
        } finally {
            Constants.zrLogConfig = previousConfig;
            restoreProperty("sws.root.path", previousRootPath);
            delete(root);
        }
    }

    @Test
    public void shouldUsePlaceholderContextPathWhenContextPathIsRoot() throws Exception {
        Path root = Files.createTempDirectory("zrlog-plugin-core-root-context");
        Path argsFile = root.resolve("plugin-args.txt");
        Path pluginCore = root.resolve("conf/plugins/plugin-core-test.bin");
        Files.createDirectories(pluginCore.getParent());
        Files.writeString(pluginCore,
                "#!/bin/sh\nprintf '%s\\n' \"$@\" > " + argsFile + "\n",
                StandardCharsets.UTF_8);
        assertTrue(pluginCore.toFile().setExecutable(true));
        String previousRootPath = System.getProperty("sws.root.path");
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        try {
            System.setProperty("sws.root.path", root.toString());
            Constants.zrLogConfig = new TestZrLogConfig();
            ImageInfo.setInImageRuntimeCode(true);
            PluginCoreProcessImpl process = new PluginCoreProcessImpl(null, "/");

            Process started = process.startPluginCore(pluginCore.toFile(), "db=ok",
                    "-Xmx64m", root.resolve("static").toString(), "3.6.0", "token-123",
                    21000, 51000);

            assertNotNull(started);
            assertEquals(0, started.waitFor());
            List<String> args = Files.readAllLines(argsFile);
            assertEquals("#", args.get(10));
        } finally {
            ImageInfo.setInImageRuntimeCode(false);
            Constants.zrLogConfig = previousConfig;
            restoreProperty("sws.root.path", previousRootPath);
            delete(root);
        }
    }

    @Test
    public void pluginServerStartShouldBuildRunnableHandleWithoutBackgroundThread() throws Exception {
        Path root = Files.createTempDirectory("zrlog-plugin-core-handle");
        Path argsFile = root.resolve("plugin-args.txt");
        Path pluginCore = root.resolve("conf/plugins/plugin-core-Linux-x86_64.bin");
        Files.createDirectories(pluginCore.getParent());
        Files.writeString(pluginCore,
                "#!/bin/sh\nprintf 'launch %s %s %s\\n' \"$1\" \"$2\" \"$5\" >> " + argsFile + "\n",
                StandardCharsets.UTF_8);
        assertTrue(pluginCore.toFile().setExecutable(true));
        String previousRootPath = System.getProperty("sws.root.path");
        ZrLogConfig previousConfig = Constants.zrLogConfig;
        String previousFileArch = setFileArch("Linux-x86_64");
        try {
            System.setProperty("sws.root.path", root.toString());
            Constants.zrLogConfig = new TestZrLogConfig();
            ImageInfo.setInImageRuntimeCode(true);
            TestablePluginCoreProcessImpl process = new TestablePluginCoreProcessImpl(null, "/blog");

            int port = process.pluginServerStart("db=ok", "-Xmx64m", root.resolve("static").toString(),
                    "3.6.0", "token-456");

            assertTrue(port >= 20000);
            assertNotNull(process.handle.get());
            process.handle.get().run();
            process.stopPluginCore();

            List<String> launches = Files.readAllLines(argsFile);
            assertTrue(launches.size() >= 1);
            assertTrue(launches.get(0).contains(String.valueOf(port)));
            assertEquals(1, process.handleStartCount.get());
            assertEquals(1, process.watcherCount.get());
        } finally {
            ImageInfo.setInImageRuntimeCode(false);
            setFileArch(previousFileArch);
            Constants.zrLogConfig = previousConfig;
            restoreProperty("sws.root.path", previousRootPath);
            delete(root);
        }
    }

    @Test
    public void stopPluginCoreShouldCloseHandleAndRunCallbackOnlyOnce() throws Exception {
        AtomicInteger stopCallbacks = new AtomicInteger();
        PluginCoreProcessImpl process = new PluginCoreProcessImpl(stopCallbacks::incrementAndGet, "/blog");
        TestPluginCoreProcessHandle handle = new TestPluginCoreProcessHandle();
        Field field = PluginCoreProcessImpl.class.getDeclaredField("pluginCoreProcessHandle");
        field.setAccessible(true);
        field.set(process, handle);

        process.stopPluginCore();
        process.stopPluginCore();

        assertEquals(1, handle.closeCount);
        assertEquals(1, stopCallbacks.get());
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static void delete(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static String setFileArch(String value) throws Exception {
        Field fileArch = BlogBuildInfoUtil.class.getDeclaredField("fileArch");
        fileArch.setAccessible(true);
        String previousFileArch = (String) fileArch.get(null);
        fileArch.set(null, value);
        return previousFileArch;
    }

    private static class TestPluginCoreProcessHandle extends AbstractPluginCoreProcessHandle {

        private int closeCount;

        @Override
        public void run() {
        }

        @Override
        void close() {
            closeCount++;
        }
    }

    private static class TestablePluginCoreProcessImpl extends PluginCoreProcessImpl {

        private final AtomicReference<AbstractPluginCoreProcessHandle> handle = new AtomicReference<>();
        private final AtomicInteger handleStartCount = new AtomicInteger();
        private final AtomicInteger watcherCount = new AtomicInteger();

        TestablePluginCoreProcessImpl(Runnable onStopRunnable, String contextPath) {
            super(onStopRunnable, contextPath);
        }

        @Override
        PluginGhostWatcher newPluginGhostWatcher(String host, int port) {
            return new PluginGhostWatcher(host, port, 0) {
                @Override
                public void doWatch() {
                    watcherCount.incrementAndGet();
                    TestablePluginCoreProcessImpl.this.stopPluginCore();
                }
            };
        }

        @Override
        void pauseAfterPluginGhostWatcher() {
        }

        @Override
        Thread startPluginCoreHandle(AbstractPluginCoreProcessHandle handle) {
            this.handle.set(handle);
            handleStartCount.incrementAndGet();
            return new Thread(handle, "captured-plugin-core-thread");
        }
    }

    private static class TestZrLogConfig extends ZrLogConfig {

        TestZrLogConfig() {
            super(19084, null, "/blog");
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
            return null;
        }

        @Override
        public List<IPlugin> getBasePluginList() {
            return new Plugins();
        }
    }
}
