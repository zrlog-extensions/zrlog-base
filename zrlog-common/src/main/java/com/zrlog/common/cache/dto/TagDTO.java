package com.zrlog.common.cache.dto;

import java.io.Serializable;

public class TagDTO implements Serializable {

    private Long id;
    private String text;
    private Long count;
    private String url;
    private Long keycode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getKeycode() {
        return keycode;
    }

    public void setKeycode(Long keycode) {
        this.keycode = keycode;
    }
}
