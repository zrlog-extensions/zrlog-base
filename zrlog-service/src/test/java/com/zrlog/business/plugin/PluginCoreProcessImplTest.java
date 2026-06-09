package com.zrlog.business.plugin;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;

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

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private void delete(Path path) throws IOException {
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
}
