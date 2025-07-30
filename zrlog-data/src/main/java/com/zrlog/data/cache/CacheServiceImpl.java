package com.zrlog.data.cache;

import com.google.gson.Gson;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.ObjectUtil;
import com.hibegin.common.util.StringUtils;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.cache.vo.BaseDataInitVO;
import com.zrlog.data.service.BaseDataDbService;
import com.zrlog.data.service.DistributedLock;
import com.zrlog.data.util.WebSiteUtils;
import com.zrlog.model.Tag;
import com.zrlog.model.Type;
import com.zrlog.model.WebSite;
import com.zrlog.util.ThreadUtils;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 对缓存数据的操作
 */
public class CacheServiceImpl implements CacheService<BaseDataInitVO> {
    private static final Logger LOGGER = LoggerUtil.getLogger(CacheServiceImpl.class);

    private final AtomicLong version;
    private volatile BaseDataInitVO cacheInit;
    private final String CACHE_KEY = "base_data_init_cache";
    private final long sqlVersion;
    private final Lock lock = new DistributedLock("zrlog-data-cache");

    public CacheServiceImpl() {
        long versionStart = Long.parseLong(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "00000");
        this.version = new AtomicLong(versionStart);
        Map<String, Object> baseInfo = new WebSite().getWebSiteByNameIn(Arrays.asList(CACHE_KEY, ZRLOG_SQL_VERSION_KEY));
        String cacheKey = (String) baseInfo.get(CACHE_KEY);
        if (StringUtils.isNotEmpty(CACHE_KEY)) {
            try {
                this.cacheInit = new Gson().fromJson(cacheKey, BaseDataInitVO.class);
            } catch (Exception e) {
                LOGGER.info("load cache error " + e.getMessage());
            }
        }
        String sqlVersion = (String) baseInfo.get(ZRLOG_SQL_VERSION_KEY);
        if (StringUtils.isNotEmpty(sqlVersion)) {
            this.sqlVersion = Long.parseLong(sqlVersion);
        } else {
            this.sqlVersion = -1L;
        }
    }

    @Override
    public long getCurrentSqlVersion() {
        return sqlVersion;
    }

    @Override
    public long getWebSiteVersion() {
        if (Objects.isNull(cacheInit)) {
            return 0;
        }
        return ObjectUtil.requireNonNullElse(cacheInit.getWebSiteVersion(), 0L);
    }

    private long getUpdateVersion(boolean cleanAble) {
        if (Objects.isNull(cacheInit) || cleanAble) {
            return version.incrementAndGet();
        }
        return version.get();
    }

    @Override
    public BaseDataInitVO getInitData() {
        return getInitData(false);
    }

    @Override
    public BaseDataInitVO refreshInitData() {
        return getInitData(true);
    }

    private BaseDataInitVO getInitData(boolean cleanAble) {
        long expectVersion = getUpdateVersion(cleanAble);
        if (!cleanAble && Objects.nonNull(cacheInit)) {
            return cacheInit;
        }
        try {
            try {
                boolean locked = lock.tryLock(2, TimeUnit.MINUTES);
                if (!locked) {
                    LOGGER.warning("RefreshInitDataCache [" + version.get() + "] get lock missing");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!Objects.equals(version.get(), expectVersion)) {
                if (Constants.debugLoggerPrintAble()) {
                    LOGGER.info("Version skip " + version.get() + " -> " + expectVersion);
                }
                return cacheInit;
            } else {
                long start = System.currentTimeMillis();
                ExecutorService executor = ThreadUtils.newFixedThreadPool(10);
                try {
                    cacheInit = new BaseDataDbService().queryCacheInit(executor);
                    cacheInit.setVersion(expectVersion);
                    //清除模版的缓存数据
                    WebSite.clearTemplateConfigMap();
                    try {
                        new WebSite().updateByKV(CACHE_KEY, new Gson().toJson(cacheInit));
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "save cache error " + e.getMessage(), e);
                    }
                } finally {
                    executor.shutdown();
                    if (Constants.debugLoggerPrintAble()) {
                        LOGGER.info("RefreshInitDataCache [" + version + "] used time " + (System.currentTimeMillis() - start) + "ms");
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        return cacheInit;
    }

    @Override
    public List<Map<String, Object>> getArticleTypes() {
        if (Objects.nonNull(cacheInit)) {
            return cacheInit.getTypes();
        }
        try {
            return new Type().findAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Map<String, Object>> getTags() {
        if (Objects.nonNull(cacheInit)) {
            return cacheInit.getTags();
        }
        try {
            return new Tag().findAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PublicWebSiteInfo getPublicWebSiteInfo() {
        if (Constants.zrLogConfig.isInstalled() && Objects.nonNull(cacheInit)) {
            PublicWebSiteInfo db = cacheInit.getWebSite();
            if (Objects.nonNull(db)) {
                return db;
            }
            return new WebSite().getPublicWebSite();
        }
        PublicWebSiteInfo publicWebSiteInfo = WebSiteUtils.fillDefaultInfo(new PublicWebSiteInfo());
        //blog
        publicWebSiteInfo.setAppId("");
        publicWebSiteInfo.setTitle("");
        publicWebSiteInfo.setSecond_title("");
        publicWebSiteInfo.setKeywords("");
        publicWebSiteInfo.setDescription("");
        publicWebSiteInfo.setStaticResourceHost("");

        //other
        publicWebSiteInfo.setWebCm("");
        publicWebSiteInfo.setIcp("");
        publicWebSiteInfo.setRobotRuleContent("");
        //plugin
        publicWebSiteInfo.setComment_plugin_name("");
        publicWebSiteInfo.setHost("");

        return publicWebSiteInfo;
    }

}
