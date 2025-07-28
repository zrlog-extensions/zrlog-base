package com.zrlog.model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hibegin.common.dao.DAO;
import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.common.dao.ResultBeanUtils;
import com.hibegin.common.util.BeanUtil;
import com.hibegin.common.util.IOUtil;
import com.hibegin.common.util.StringUtils;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.FaviconBase64DTO;

import java.sql.SQLException;
import java.util.*;

/**
 * 存放全局的设置，比如网站标题，关键字，插件，主题的配置信息等，当字典表处理即可，对应数据库的website表
 */
public class WebSite extends DAO {

    private static final List<String> websitePublicQueryKeys;
    private static final String TEMPLATE_CONFIG_SUFFIX = "_setting";


    static {
        String[] list = new Gson().fromJson(IOUtil.getStringInputStream(WebSite.class.getResourceAsStream("/conf/website-key-public.json")), String[].class);
        websitePublicQueryKeys = Arrays.asList(list);
    }

    public static void main(String[] args) {
        System.out.println(websitePublicQueryKeys);
    }

    public WebSite() {
        this.tableName = "website";
        this.pk = "siteId";
    }

    public WebSite(DataSourceWrapper dataSource) {
        super(dataSource);
        this.tableName = "website";
        this.pk = "siteId";
    }

    public PublicWebSiteInfo getPublicWebSite() {
        PublicWebSiteInfo convert = ResultBeanUtils.convert(getWebSiteByNameIn(websitePublicQueryKeys), PublicWebSiteInfo.class);
        if (Objects.isNull(convert.getRows())) {
            convert.setRows(10L);
        }
        if (Objects.isNull(convert.getArticle_auto_digest_length())) {
            convert.setArticle_auto_digest_length(Constants.DEFAULT_ARTICLE_DIGEST_LENGTH);
        }
        if (Objects.isNull(convert.getSession_timeout())) {
            convert.setArticle_auto_digest_length(TokenService.DEFAULT_SESSION_TIMEOUT / 60 / 1000);
        }
        convert.setAdmin_darkMode(Objects.equals(convert.getAdmin_darkMode(), true));
        convert.setChangyan_status(Objects.equals(convert.getChangyan_status(), true));
        convert.setDisable_comment_status(Objects.equals(convert.getDisable_comment_status(), true));
        convert.setGenerator_html_status(Objects.equals(convert.getGenerator_html_status(), true));
        convert.setArticle_thumbnail_status(Objects.equals(convert.getArticle_thumbnail_status(), true));
        return convert;
    }

    private Map<String, Object> fillToMap(List<Map<String, Object>> lw, List<String> names) {
        Map<String, Object> webSites = new LinkedHashMap<>();
        for (String name : names) {
            Object value = null;
            Object remark = null;
            for (Map<String, Object> map : lw) {
                if (Objects.equals(map.get("name"), name)) {
                    value = map.get("value");
                    remark = map.get("remark");
                    break;
                }
            }
            webSites.put(name, value);
            webSites.put(name + "Remark", remark);
        }
        return webSites;
    }

    public Map<String, Object> getWebSiteByNameIn(List<String> names) {
        List<Map<String, Object>> lw;
        StringJoiner sj = new StringJoiner(",");
        Object[] params = new String[names.size()];
        for (int i = 0; i < params.length; i++) {
            sj.add("?");
            params[i] = names.get(i);
        }
        try {
            lw = queryListWithParams("select name,remark,value from " + tableName + " where name in " + "(" + sj + ")", params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return fillToMap(lw, names);
    }

    public boolean updateByKV(String name, Object value) throws SQLException {
        if (queryFirstObj("select siteId from " + tableName + " where name=?", name) != null) {
            return execute("update " + tableName + " set value=? where name=?", value, name);
        } else {
            return execute("insert INTO  " + tableName + " (`value`,`name`) VALUES (?,?)", value, name);
        }
    }

    public String getStringValueByName(String name) {
        try {
            Map<String, Object> webSiteByNameIn = getWebSiteByNameIn(Collections.singletonList(name));
            Object value = webSiteByNameIn.get(name);
            if (Objects.isNull(value)) {
                return "";
            }
            return value.toString();
        } catch (Exception e) {
            //ignore，比如在未安装时，会有该异常但是不影响逻辑
        }
        return "";
    }

    public String getPublicStringValueByName(String name) {
        if (!websitePublicQueryKeys.contains(name)) {
            throw new ArithmeticException(name);
        }
        return getStringValueByName(name);
    }

    public static void clearTemplateConfigMap() {
        Constants.zrLogConfig.getTemplateConfigCacheMap().clear();
    }

    public Map<String, Object> getTemplateConfigMapWithCache(String templateName) {
        return Constants.zrLogConfig.getTemplateConfigCacheMap().computeIfAbsent(templateName, (k) -> {
            String dbJsonStr = new WebSite().getStringValueByName(k + TEMPLATE_CONFIG_SUFFIX);
            if (StringUtils.isNotEmpty(dbJsonStr)) {
                return new Gson().fromJson(dbJsonStr, Map.class);
            }
            return new HashMap<>();
        });
    }

    public Map<String, Object> updateTemplateConfigMap(String templateName, Map<String, Object> settingMap) throws SQLException {
        new WebSite().updateByKV(templateName + TEMPLATE_CONFIG_SUFFIX, new GsonBuilder().serializeNulls().create().toJson(settingMap));
        return settingMap;
    }

    public FaviconBase64DTO faviconBase64DTO() {
        Map<String, Object> dataMap = getWebSiteByNameIn(Arrays.asList("favicon_ico_base64", "favicon_png_pwa_192_base64", "favicon_png_pwa_512_base64", "generator_html_status"));
        return BeanUtil.convert(dataMap, FaviconBase64DTO.class);
    }
}
