package com.zrlog.common.vo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class I18nVO {

    private Map<String, Map<String, Object>> blog = new ConcurrentHashMap<>();
    private Map<String, Map<String, Object>> backend = new ConcurrentHashMap<>();
    private Map<String, Map<String, Object>> admin = new ConcurrentHashMap<>();
    private Map<String, Map<String, Object>> adminBackend = new ConcurrentHashMap<>();
    private String locale;
    private String lang;

    public Map<String, Map<String, Object>> getBlog() {
        return blog;
    }

    public void setBlog(Map<String, Map<String, Object>> blog) {
        this.blog = blog;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Map<String, Map<String, Object>> getBackend() {
        return backend;
    }

    public void setBackend(Map<String, Map<String, Object>> backend) {
        this.backend = backend;
    }

    public Map<String, Map<String, Object>> getAdmin() {
        return admin;
    }

    public void setAdmin(Map<String, Map<String, Object>> admin) {
        this.admin = admin;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Map<String, Map<String, Object>> getAdminBackend() {
        return adminBackend;
    }

    public void setAdminBackend(Map<String, Map<String, Object>> adminBackend) {
        this.adminBackend = adminBackend;
    }
}
