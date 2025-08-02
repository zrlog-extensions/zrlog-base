package com.zrlog.common.cache.dto;

import java.io.Serializable;

public class TypeDTO implements Serializable {

    private String alias;
    private String typeName;
    private String remark;
    private Long id;
    private Long amount;
    private Long typeamount;
    private String url;
    private String arrange_plugin;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getTypeamount() {
        return typeamount;
    }

    public void setTypeamount(Long typeamount) {
        this.typeamount = typeamount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getArrange_plugin() {
        return arrange_plugin;
    }

    public void setArrange_plugin(String arrange_plugin) {
        this.arrange_plugin = arrange_plugin;
    }
}
