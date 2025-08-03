package com.zrlog.model;

import com.hibegin.common.dao.BasePageableDAO;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.zrlog.common.cache.dto.LogNavDTO;

import java.sql.SQLException;
import java.util.List;

/**
 * 存放导航条的信息，对应数据库的lognav表
 */
public class LogNav extends BasePageableDAO {

    public LogNav() {
        this.pk = "navId";
        this.tableName = "lognav";
    }

    public List<LogNavDTO> findAll() throws SQLException {
        return doConvertList(queryListWithParams("select l.navId as id,l.navName,l.url,l.sort from " + tableName + " l where l.url is not null and l.navName is not null order by sort"), LogNavDTO.class);
    }

    public PageData<LogNavDTO> find(PageRequest page) {
        return queryPageData("select l.navId as id,l.navName,l.url,l.sort from " + tableName + " l where l.url is not null and l.navName is not null order by sort", page, new Object[0], LogNavDTO.class);
    }
}
