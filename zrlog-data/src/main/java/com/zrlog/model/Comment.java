package com.zrlog.model;

import com.hibegin.common.dao.BasePageableDAO;
import com.hibegin.common.dao.ResultValueConvertUtils;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.hibegin.common.util.SecurityUtils;
import com.zrlog.data.dto.CommentDTO;
import com.zrlog.data.dto.VisitorCommentDTO;
import com.zrlog.data.exception.DAOException;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对应数据里面的 comment 表，用于存放文章对应的评论信息。
 */
public class Comment extends BasePageableDAO {

    public static final String TABLE_NAME = "comment";

    public Comment() {
        this.tableName = TABLE_NAME;
        this.pk = "commentId";
    }

    public PageData<CommentDTO> find(PageRequest page) {
        String sql = "select commentId as id,userComment,header,commTime,userMail,userHome,userIp,userName,hide,logId from " + tableName + " order by commTime desc";
        PageData<CommentDTO> commentDTOPageData = queryPageData(sql, page, new Object[0], CommentDTO.class);
        commentDTOPageData.getRows().forEach(e -> {
            e.setCommTime(ResultValueConvertUtils.formatDate(e.getCommTime(), "yyyy-MM-dd HH:mm:ss"));
        });
        return commentDTOPageData;
    }

    public Long count() throws SQLException {
        String sql = "select count(1) from " + tableName;
        return ((Number) queryFirstObj(sql)).longValue();
    }

    public Long countToDayComment() throws SQLException {
        String sql = "select count(1) from " + tableName + " where commTime>?";
        return ((Number) queryFirstObj(sql, new SimpleDateFormat("yyyy-MM-dd").format(new Date()))).longValue();
    }

    public List<Map<String, Object>> findHaveReadIsFalse() throws SQLException {
        String sql = "select commentId as id,userComment,header,userMail,userHome,userIp,userName,hide,logId from " + tableName + " where have_read = ?  order by commTime desc ";
        return queryListWithParams(sql, false);
    }

    public List<Map<String, Object>> findAllByLogId(int logId) throws SQLException {
        List<Map<String, Object>> comments = queryListWithParams("select * from " + tableName + " where logId=?", logId);
        for (Map<String, Object> comment : comments) {
            comment.put("commTime", ResultValueConvertUtils.formatDate(comment.get("commTime"), "yyyy-MM-dd HH:mm:ss"));
        }
        return comments;
    }

    public List<VisitorCommentDTO> visitorFindAllByLogId(int logId) throws SQLException {
        List<Map<String, Object>> comments = queryListWithParams("select * from " + tableName + " where logId=?", logId);
        for (Map<String, Object> comment : comments) {
            comment.put("commTime", ResultValueConvertUtils.formatDate(comment.get("commTime"), "yyyy-MM-dd HH:mm:ss"));
            String email = (String) comment.get("userMail");
            if (Objects.isNull(email)) {
                comment.put("gravatarId", "");
            } else {
                comment.put("gravatarId", SecurityUtils.md5(email));
            }
        }
        return doConvertList(comments, VisitorCommentDTO.class);
    }

    public void doRead(long id) {
        try {
            new Comment().set("have_read", true).updateById(id);
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }
}
