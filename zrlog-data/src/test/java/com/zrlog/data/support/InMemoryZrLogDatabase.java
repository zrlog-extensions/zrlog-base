package com.zrlog.data.support;

import com.hibegin.common.dao.DAO;
import com.hibegin.common.dao.DataSourceWrapper;
import com.zrlog.util.DataSourceUtil;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class InMemoryZrLogDatabase implements AutoCloseable {

    private final DataSourceWrapper dataSource;
    private final DataSourceWrapper previousDataSource;

    private InMemoryZrLogDatabase() throws Exception {
        this.previousDataSource = currentDefaultDataSource();
        this.dataSource = newDataSource();
        DAO.setDs(dataSource);
        loadSchema();
    }

    public static InMemoryZrLogDatabase open() throws Exception {
        return new InMemoryZrLogDatabase();
    }

    public int update(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().update(sql, params);
    }

    public Object scalar(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().query(sql, new ScalarHandler<>(1), params);
    }

    public Map<String, Object> queryOne(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().query(sql, new MapHandler(), params);
    }

    public List<Map<String, Object>> queryList(String sql, Object... params) throws SQLException {
        return dataSource.getQueryRunner().query(sql, new MapListHandler(), params);
    }

    private static DataSourceWrapper newDataSource() {
        Properties properties = new Properties();
        properties.setProperty("driverClass", "org.h2.Driver");
        properties.setProperty("jdbcUrl", "jdbc:h2:mem:zrlog_data_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
                + ";NON_KEYWORDS=USER,VALUE,COMMENT,TYPE;DB_CLOSE_DELAY=-1");
        properties.setProperty("user", "sa");
        properties.setProperty("password", "");
        return DataSourceUtil.buildDataSource(properties);
    }

    private void loadSchema() throws Exception {
        try (InputStream input = InMemoryZrLogDatabase.class.getResourceAsStream("/init-table-structure.sql")) {
            if (input == null) {
                throw new IllegalStateException("Missing init-table-structure.sql from zrlog-install-web test dependency");
            }
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : normalizeInstallSqlForH2(sql).split(";")) {
                String trimmed = normalizeStatement(statement);
                if (!trimmed.isEmpty()) {
                    dataSource.getQueryRunner().update(trimmed);
                }
            }
        }
    }

    private static String normalizeInstallSqlForH2(String sql) {
        StringBuilder builder = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("/*!")) {
                continue;
            }
            String normalizedLine = line
                    .replaceAll("(?i)UNIQUE\\s+KEY\\s+`[^`]+`\\s*\\(", "UNIQUE (")
                    .replaceAll("(?i)KEY\\s+`[^`]+`\\s*\\(", "INDEX (")
                    .replaceAll("(?i)\\s+COMMENT\\s+'[^']*'", "");
            builder.append(normalizedLine).append('\n');
        }
        return builder.toString()
                .replace("bit(1)", "boolean")
                .replace("DEFAULT b'0'", "DEFAULT false")
                .replace("DEFAULT b'1'", "DEFAULT true")
                .replaceAll("(?i)\\)\\s*ENGINE\\s*=\\s*InnoDB\\s+DEFAULT\\s+CHARSET\\s*=\\s*[^\\s;]+"
                        + "(?:\\s+COLLATE\\s+[^\\s;]+)?", ")");
    }

    private static String normalizeStatement(String statement) {
        String trimmed = statement.trim();
        if (trimmed.toLowerCase().startsWith("drop table if exists") && trimmed.contains(",")) {
            return "";
        }
        return trimmed;
    }

    private static DataSourceWrapper currentDefaultDataSource() throws Exception {
        Field field = DAO.class.getDeclaredField("defaultDataSource");
        field.setAccessible(true);
        return (DataSourceWrapper) field.get(null);
    }

    @Override
    public void close() throws Exception {
        try {
            dataSource.close();
        } finally {
            DAO.setDs(previousDataSource);
        }
    }
}
