package com.zrlog.common;

import com.zrlog.common.cache.dto.TagDTO;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.cache.dto.UserBasicDTO;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.PublicWebSiteInfo;

import java.util.List;

public interface CacheService {

    String ZRLOG_SQL_VERSION_KEY = "zrlogSqlVersion";

    long getCurrentSqlVersion();

    long getWebSiteVersion();

    BaseDataInitVO getInitData();

    BaseDataInitVO refreshInitData();

    PublicWebSiteInfo getPublicWebSiteInfo();

    List<TypeDTO> getArticleTypes();

    List<TagDTO> getTags();

    UserBasicDTO findByUserId(Long userId);

}
