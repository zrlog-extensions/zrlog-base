package com.zrlog.util;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.http.server.util.NativeImageUtils;
import com.zrlog.common.cache.dto.*;
import com.zrlog.common.cache.vo.Archive;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.cache.vo.HotLogBasicInfoEntry;
import com.zrlog.common.vo.*;
import org.apache.commons.dbutils.BasicRowProcessor;

import java.io.InputStream;
import java.util.*;

public class ZrLogBaseNativeImageUtils {

    public static class MyBasicRowProcessor extends BasicRowProcessor {
        public static Map<String, Object> createMap() {
            return createCaseInsensitiveHashMap(2);
        }
    }

    public static void reg() {
        List<String> resourceFiles = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String filePath = "/conf/update-sql/" + i + ".sql";
            InputStream inputStream = ZrLogBaseNativeImageUtils.class.getResourceAsStream(filePath);
            if (Objects.nonNull(inputStream)) {
                resourceFiles.add(filePath);
            }
        }
        resourceFiles.add("/i18n/backend_en_US.properties");
        resourceFiles.add("/i18n/backend_zh_CN.properties");
        resourceFiles.add("/conf/error/403.html");
        resourceFiles.add("/conf/error/404.html");
        resourceFiles.add("/conf/error/500.html");

        NativeImageUtils.doResourceLoadByResourceNames(resourceFiles);
        //
        NativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(LockVO.class, I18nVO.class, TemplateVO.class, MyBasicRowProcessor.class));
        //freemarker
        regWithGetMethod(TypeDTO.class,
                LinkDTO.class, LogNavDTO.class, TagDTO.class, PluginDTO.class,
                BaseDataInitVO.class, BaseDataInitVO.Statistics.class,
                HotLogBasicInfoEntry.class, PageData.class,
                Version.class, Archive.class, Outline.class,
                PublicWebSiteInfo.class);
    }

    public static void regWithGetMethod(Class<?>... objects) {
        NativeImageUtils.gsonNativeAgentByClazz(List.of(objects));
        for (Class<?> o : objects) {
            NativeImageUtils.regGetMethodByClassName(o);
        }
    }
}
