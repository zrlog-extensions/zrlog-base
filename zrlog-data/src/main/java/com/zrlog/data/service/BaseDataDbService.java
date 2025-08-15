package com.zrlog.data.service;

import com.google.gson.Gson;
import com.hibegin.common.dao.ResultValueConvertUtils;
import com.hibegin.common.dao.dto.PageRequestImpl;
import com.hibegin.common.util.BeanUtil;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.zrlog.common.Constants;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.vo.Archive;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.cache.vo.HotLogBasicInfoEntry;
import com.zrlog.common.cache.vo.HotTypeLogInfo;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.model.*;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BaseDataDbService {

    private static final Logger LOGGER = LoggerUtil.getLogger(BaseDataDbService.class);

    private HotLogBasicInfoEntry convertToBasicVO(ArticleBasicDTO log) {
        String format = "yyyy-MM-dd";
        log.setReleaseTime(ResultValueConvertUtils.formatDate(log.getReleaseTime(), format));
        log.setLastUpdateDate(ResultValueConvertUtils.formatDate(log.getLastUpdateDate(), format));
        log.setLast_update_date(ResultValueConvertUtils.formatDate(log.getLast_update_date(), format));
        return BeanUtil.convert(log, HotLogBasicInfoEntry.class);
    }

    public static long getWebSiteVersion(PublicWebSiteInfo website) {
        return new Gson().toJson(website).hashCode();
    }

    private static List<Archive> getConvertedArchives(Map<String, Long> archiveMap) {
        List<Archive> archives = new ArrayList<>();
        for (Map.Entry<String, Long> entry : archiveMap.entrySet()) {
            Archive archive = new Archive();
            archive.setCount(entry.getValue());
            archive.setText(entry.getKey());
            String tagUri = Constants.getArticleUri() + "record/" + entry.getKey();
            archive.setUrl(tagUri);
            archives.add(archive);
        }
        return archives;
    }

    public BaseDataInitVO queryCacheInit(Executor executor) {
        BaseDataInitVO cacheInit = new BaseDataInitVO();
        //first set website info
        PublicWebSiteInfo refreshWebSite = new WebSite().getPublicWebSite();
        cacheInit.setWebSite(refreshWebSite);
        cacheInit.setWebSiteVersion(getWebSiteVersion(refreshWebSite));
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        BaseDataInitVO.Statistics statistics = new BaseDataInitVO.Statistics();
        futures.add(CompletableFuture.runAsync(() -> {
            statistics.setTotalArticleSize(new Log().getVisitorCount());
            cacheInit.setStatistics(statistics);
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                cacheInit.setLinks(new Link().findAll());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                cacheInit.setTypes(new Type().findAll());
                statistics.setTotalTypeSize((long) cacheInit.getTypes().size());
                List<TypeDTO> types = cacheInit.getTypes();
                List<HotTypeLogInfo> indexHotLog = new ArrayList<>();
                cacheInit.setTypeHotLogs(indexHotLog);
                //设置分类Hot
                for (TypeDTO type : types) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        HotTypeLogInfo hotTypeLogInfo = new HotTypeLogInfo();
                        String alias = type.getAlias();
                        hotTypeLogInfo.setTypeAlias(alias);
                        hotTypeLogInfo.setTypeName(type.getTypeName());
                        hotTypeLogInfo.setTypeId(type.getId());
                        hotTypeLogInfo.setLogs(new Log().findByTypeAlias(1, 6, alias).getRows().stream().map(this::convertToBasicVO).collect(Collectors.toList()));
                        indexHotLog.add(hotTypeLogInfo);
                    }, executor));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            //Last article
            cacheInit.setHotLogs(new Log().visitorFind(new PageRequestImpl(1L, 6L), "").getRows().stream().map(this::convertToBasicVO).collect(Collectors.toList()));
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                cacheInit.setLogNavs(new LogNav().findAll());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                cacheInit.setPlugins(new Plugin().findAll());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                cacheInit.setArchives(new Log().getArchives());
                cacheInit.setArchiveList(getConvertedArchives(cacheInit.getArchives()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                cacheInit.setUsers(new User().findBasicAll());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                cacheInit.setTags(new Tag().refreshTag());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            statistics.setTotalTagSize((long) cacheInit.getTags().size());
        }, executor));
        futures.add(CompletableFuture.runAsync(() -> {
            String template = refreshWebSite.getTemplate();
            if (StringUtils.isNotEmpty(template)) {
                cacheInit.getTemplateConfigCacheMap().put(template, new WebSite().getTemplateConfigMap(template));
            }
        }));
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            LOGGER.warning("Load data error " + e.getMessage());
        }
        if (cacheInit.getTags() == null || cacheInit.getTags().isEmpty()) {
            cacheInit.getPlugins().stream().filter(e -> Objects.equals(e.getPluginName(), "tags")).findFirst().ifPresent(e -> {
                cacheInit.getPlugins().remove(e);
            });
        }
        if (cacheInit.getArchives() == null || cacheInit.getArchives().isEmpty()) {
            cacheInit.getPlugins().stream().filter(e -> Objects.equals(e.getPluginName(), "archives")).findFirst().ifPresent(e -> {
                cacheInit.getPlugins().remove(e);
            });
        }
        if (cacheInit.getTypes() == null || cacheInit.getTypes().isEmpty()) {
            cacheInit.getPlugins().stream().filter(e -> Objects.equals(e.getPluginName(), "types")).findFirst().ifPresent(e -> {
                cacheInit.getPlugins().remove(e);
            });
        }
        if (cacheInit.getLinks() == null || cacheInit.getLinks().isEmpty()) {
            cacheInit.getPlugins().stream().filter(e -> Objects.equals(e.getPluginName(), "links")).findFirst().ifPresent(e -> {
                cacheInit.getPlugins().remove(e);
            });
        }
        cacheInit.getTypeHotLogs().sort(Comparator.comparing(x -> Math.toIntExact(x.getTypeId())));
        return cacheInit;
    }
}
