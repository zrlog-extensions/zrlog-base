package com.zrlog.business.service;

import com.zrlog.common.updater.UpgradeProgressEvent;
import com.zrlog.common.updater.UpgradeProgressListener;
import com.zrlog.common.vo.Version;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UpgradeServiceHelpersTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldCalculateDownloadProgress() throws Exception {
        Method method = method("getDownloadProcess", File.class, long.class);
        File file = temporaryFolder.newFile("zrlog-upgrade.zip");
        Files.writeString(file.toPath(), "hello", StandardCharsets.UTF_8);

        assertEquals(0, method.invoke(null, null, 10L));
        assertEquals(0, method.invoke(null, file, 0L));
        assertEquals(0, method.invoke(null, new File(temporaryFolder.getRoot(), "missing.zip"), 10L));
        assertEquals(50, method.invoke(null, file, 10L));
        assertEquals(100, method.invoke(null, file, 2L));
    }

    @Test
    public void shouldDetectInvalidDownloadedPackage() throws Exception {
        Method method = method("isErrorFile", File.class, long.class, String.class);
        File file = temporaryFolder.newFile("zrlog-upgrade.zip");
        Files.writeString(file.toPath(), "hello", StandardCharsets.UTF_8);
        File empty = temporaryFolder.newFile("empty.zip");

        assertTrue((Boolean) method.invoke(null, null, 0L, ""));
        assertTrue((Boolean) method.invoke(null, new File(temporaryFolder.getRoot(), "missing.zip"), 0L, ""));
        assertTrue((Boolean) method.invoke(null, empty, 0L, ""));
        assertTrue((Boolean) method.invoke(null, file, 6L, ""));
        assertTrue((Boolean) method.invoke(null, file, 5L, "bad-md5"));
        assertFalse((Boolean) method.invoke(null, file, 5L, "5d41402abc4b2a76b9719d911017c592"));
        assertFalse((Boolean) method.invoke(null, file, 0L, ""));
    }

    @Test
    public void shouldBuildSanitizedUpgradeKey() throws Exception {
        Method method = method("buildUpgradeKey", Version.class);
        Version buildIdVersion = new Version();
        buildIdVersion.setBuildId("3.6.0 SNAPSHOT/abc");
        buildIdVersion.setVersion("ignored");
        Version versionFallback = new Version();
        versionFallback.setVersion("4.0.0+meta");

        String buildIdKey = (String) method.invoke(null, buildIdVersion);
        String versionKey = (String) method.invoke(null, versionFallback);
        String fallbackKey = (String) method.invoke(null, new Object[]{null});

        assertTrue(buildIdKey.startsWith("upgrade-3.6.0-SNAPSHOT-abc-"));
        assertTrue(versionKey.startsWith("upgrade-4.0.0-meta-"));
        assertTrue(fallbackKey.startsWith("upgrade-"));
        assertTrue(buildIdKey.matches("[A-Za-z0-9._-]+"));
        assertTrue(versionKey.matches("[A-Za-z0-9._-]+"));
    }

    @Test
    public void shouldBuildDownloadMessagesFromBackendMap() throws Exception {
        Method backendString = method("backendString", Map.class, String.class);
        Method getDownloadMessage = method("getDownloadMessage", Map.class, Integer.class);
        Map<String, Object> backend = new HashMap<>();
        backend.put("upgrade.status.downloading", "Downloading");

        assertEquals("Downloading", backendString.invoke(null, backend, "upgrade.status.downloading"));
        assertEquals("missing.key", backendString.invoke(null, backend, "missing.key"));
        assertEquals("Downloading", getDownloadMessage.invoke(null, backend, null));
        assertEquals("Downloading", getDownloadMessage.invoke(null, backend, 0));
        assertEquals("Downloading 42%", getDownloadMessage.invoke(null, backend, 42));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPublishProgressAndWrapListenerFailures() throws Exception {
        Method method = method("publishProgress", UpgradeProgressListener.class, String.class, String.class,
                String.class, String.class);
        AtomicReference<String> eventRef = new AtomicReference<>();
        AtomicReference<Map<String, Object>> dataRef = new AtomicReference<>();
        UpgradeProgressListener listener = (event, data) -> {
            eventRef.set(event);
            dataRef.set(data);
        };

        method.invoke(null, listener, UpgradeProgressEvent.STAGE_DOWNLOAD, UpgradeProgressEvent.STATUS_RUNNING,
                "Downloading", "zrlog.zip");

        assertEquals(UpgradeProgressEvent.EVENT, eventRef.get());
        Map<String, Object> data = dataRef.get();
        assertNotNull(data);
        assertEquals(UpgradeProgressEvent.STAGE_DOWNLOAD, data.get("stage"));
        assertEquals(UpgradeProgressEvent.STATUS_RUNNING, data.get("status"));
        assertEquals("Downloading", data.get("message"));
        assertEquals("zrlog.zip", data.get("detail"));

        UpgradeProgressListener failing = (event, progressData) -> {
            throw new Exception("listener failed");
        };
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, failing, UpgradeProgressEvent.STAGE_DOWNLOAD,
                        UpgradeProgressEvent.STATUS_RUNNING, "Downloading", null));
        assertTrue(thrown.getCause() instanceof IllegalStateException);
    }

    private static Method method(String name, Class<?>... parameterTypes) throws Exception {
        Method method = UpgradeService.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}
