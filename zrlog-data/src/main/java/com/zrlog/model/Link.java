package com.zrlog.model;

import com.hibegin.common.dao.BasePageableDAO;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.zrlog.data.dto.LinkDTO;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 存放程序的友情链接，对应数据库link表
 */
public class Link extends BasePageableDAO {


    public Link() {
        this.tableName = "link";
        this.pk = "linkId";
    }

    public List<Map<String, Object>> findAll() throws SQLException {
        return queryListWithParams("select linkName,linkId as id,sort,url,alt from " + tableName + " order by sort");
    }

    public PageData<LinkDTO> find(PageRequest page) {
        return queryPageData("select linkName,linkId as id,sort,url,alt from " + tableName + " order by sort", page, new Object[0], LinkDTO.class);
    }
}
