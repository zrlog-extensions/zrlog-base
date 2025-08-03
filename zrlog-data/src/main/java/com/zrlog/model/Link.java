package com.zrlog.model;

import com.hibegin.common.dao.BasePageableDAO;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.zrlog.common.cache.dto.LinkDTO;

import java.sql.SQLException;
import java.util.List;

/**
 * 存放程序的友情链接，对应数据库link表
 */
public class Link extends BasePageableDAO {


    public Link() {
        this.tableName = "link";
        this.pk = "linkId";
    }

    public List<LinkDTO> findAll() throws SQLException {
        return doConvertList(queryListWithParams("select linkName,linkId as id,sort,url,alt from " + tableName + " order by sort"), LinkDTO.class);
    }

    public PageData<LinkDTO> find(PageRequest page) {
        return queryPageData("select linkName,linkId as id,sort,url,alt from " + tableName + " order by sort", page, new Object[0], LinkDTO.class);
    }
}
