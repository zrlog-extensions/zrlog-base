package com.zrlog.model;

import com.hibegin.common.dao.BasePageableDAO;
import com.zrlog.common.cache.dto.UserBasicDTO;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 存放用于管理员的相关信息，对应数据的user表
 */
public class User extends BasePageableDAO {


    public User() {
        this.tableName = "user";
        this.pk = "userId";
    }

    public List<UserBasicDTO> findBasicAll() throws SQLException {
        return doConvertList(new User().queryList("userName", "userId", "header"), UserBasicDTO.class);
    }

    public Map<String, Object> getUserByUserName(String userName) throws SQLException {
        return queryFirstWithParams("select * from " + tableName + " where userName=?", userName);
    }

    public String getPasswordByUserId(int userId) throws SQLException {
        return (String) queryFirstWithParams("select password from " + tableName + " where userId=?", userId).get("password");
    }

    public boolean updatePassword(int userId, String password) throws SQLException {
        return execute("update " + tableName + " set password=? where userId=?", password, userId);
    }

    public void updateEmailUserNameHeaderByUserId(String email, String userName, String header, int userId) throws SQLException {
        execute("update " + tableName + " set header=?,email=?,userName=? where userId=?", header, email, userName, userId);
    }
}
