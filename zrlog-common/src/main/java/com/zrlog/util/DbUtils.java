package com.zrlog.util;

import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.IOUtil;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.common.ZrLogConfig;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLRecoverableException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbUtils {

    private static final Logger LOGGER = LoggerUtil.getLogger(DbUtils.class);

    /**
     * 将 env 配置的 DB_PROPERTIES 写入到实际的文件中，便于程序读取
     */
    public static File initDbPropertiesFile(ZrLogConfig zrLogConfig) {
        File dbFiles = new File(PathUtil.getConfPath() + "/db.properties");
        dbFiles.getParentFile().mkdirs();
        try {
            if (!zrLogConfig.isInstalled()) {
                return dbFiles;
            }
            if (StringUtils.isNotEmpty(ZrLogUtil.getDbInfoByEnv())) {
                IOUtil.writeBytesToFile(new String(ZrLogUtil.getDbInfoByEnv().getBytes()).replaceAll(" ", "\n").getBytes(), dbFiles);
            }
            Properties properties = getDbProp(dbFiles);
            String driverClass = properties.getProperty("driverClass");
            if (StringUtils.isEmpty(driverClass)) {
                return dbFiles;
            }
            if ("com.mysql.cj.jdbc.Driver".equals(properties.get("driverClass"))) {
                return dbFiles;
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(dbFiles)) {
                properties.put("driverClass", "com.mysql.cj.jdbc.Driver");
                properties.put("jdbcUrl", properties.get("jdbcUrl") + "&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=GMT");
                properties.store(fileOutputStream, "Support mysql8");
                LOGGER.info("Upgrade properties success");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "initDbPropertiesFile error " + e.getMessage());
        }
        return dbFiles;
    }


    public static Properties getDbProp(File dbPropertiesFile) {
        Properties dbProperties = new Properties();
        try (FileInputStream in = new FileInputStream(dbPropertiesFile)) {
            dbProperties.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dbProperties;
    }


    public static DataSource configDatabaseWithRetry(int timeoutInSeconds, ZrLogConfig zrLogConfig) {
        try {
            return zrLogConfig.configDatabase();
        } catch (Exception e) {
            if (timeoutInSeconds > 0 && e instanceof SQLRecoverableException) {
                int seekSeconds = 5;
                try {
                    Thread.sleep(seekSeconds * 1000);
                } catch (InterruptedException ex) {
                    //ignore
                }
                return configDatabaseWithRetry(timeoutInSeconds - seekSeconds, zrLogConfig);
            }
            LoggerUtil.getLogger(DbUtils.class).warning("Config database error " + e.getMessage());
            if (EnvKit.isNativeImage()) {
                System.exit(-1);
            }
        }
        return null;
    }

}
