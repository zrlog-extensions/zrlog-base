package com.zrlog.business.service;

import com.zrlog.business.support.InMemoryZrLogDatabase;
import com.zrlog.business.version.UpgradeVersionHandler;
import com.zrlog.common.CacheService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DbUpgradeServiceDatabaseTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldSkipUpgradeWhenDatabaseIsAlreadyAtLatestSqlVersion() throws Exception {
        try (InMemoryZrLogDatabase db = InMemoryZrLogDatabase.open()) {
            db.update("insert into website(name, value, remark) values(?, ?, ?)",
                    CacheService.ZRLOG_SQL_VERSION_KEY, String.valueOf(UpgradeVersionHandler.SQL_VERSION), "");

            new DbUpgradeService(db.dataSource(), UpgradeVersionHandler.SQL_VERSION).tryDoUpgrade();

            assertEquals(String.valueOf(UpgradeVersionHandler.SQL_VERSION),
                    db.scalar("select value from website where name=?", CacheService.ZRLOG_SQL_VERSION_KEY));
        }
    }

    @Test
    public void shouldSkipUpgradeWhenCurrentSqlVersionIsUnknown() throws Exception {
        try (InMemoryZrLogDatabase db = InMemoryZrLogDatabase.open()) {
            new DbUpgradeService(db.dataSource(), -1).tryDoUpgrade();

            assertNull(db.scalar("select value from website where name=?", CacheService.ZRLOG_SQL_VERSION_KEY));
        }
    }

    @Test
    public void shouldExecutePendingSqlAndPersistLatestSqlVersionUsingConfiguredConfPath() throws Exception {
        String previousConfPath = System.getProperty("sws.conf.path");
        File confFolder = writeUpgradeSql("conf",
                "insert into website(name, value, remark) values('db.upgrade.marker', 'ok', '');",
                UpgradeVersionHandler.SQL_VERSION);
        try {
            System.setProperty("sws.conf.path", confFolder.getAbsolutePath());
            try (InMemoryZrLogDatabase db = InMemoryZrLogDatabase.open()) {
                new DbUpgradeService(db.dataSource(), UpgradeVersionHandler.SQL_VERSION - 1).tryDoUpgrade();

                assertEquals("ok", db.scalar("select value from website where name=?", "db.upgrade.marker"));
                assertEquals(String.valueOf(UpgradeVersionHandler.SQL_VERSION),
                        db.scalar("select value from website where name=?", CacheService.ZRLOG_SQL_VERSION_KEY));
            }
        } finally {
            restoreProperty("sws.conf.path", previousConfPath);
        }
    }

    @Test
    public void shouldStopUpgradeAndKeepVersionWhenPendingSqlFails() throws Exception {
        String previousConfPath = System.getProperty("sws.conf.path");
        File confFolder = writeUpgradeSql("bad-conf", "bad sql statement;", UpgradeVersionHandler.SQL_VERSION);
        try {
            System.setProperty("sws.conf.path", confFolder.getAbsolutePath());
            try (InMemoryZrLogDatabase db = InMemoryZrLogDatabase.open()) {
                new DbUpgradeService(db.dataSource(), UpgradeVersionHandler.SQL_VERSION - 1).tryDoUpgrade();

                assertNull(db.scalar("select value from website where name=?", CacheService.ZRLOG_SQL_VERSION_KEY));
            }
        } finally {
            restoreProperty("sws.conf.path", previousConfPath);
        }
    }

    private File writeUpgradeSql(String folderName, String sql, int version) throws Exception {
        File confFolder = temporaryFolder.newFolder(folderName);
        File updateSqlFolder = new File(confFolder, "update-sql");
        assertEquals(true, updateSqlFolder.mkdirs());
        Files.writeString(new File(updateSqlFolder, version + ".sql").toPath(), sql, StandardCharsets.UTF_8);
        return confFolder;
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
