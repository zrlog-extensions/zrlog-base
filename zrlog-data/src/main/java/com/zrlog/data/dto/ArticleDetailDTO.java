package com.zrlog.data.dto;

import java.util.List;
import java.util.Map;

public class ArticleDetailDTO extends ArticleBasicDTO {

    //
    private List<TagsDTO> tags;
    private LastLogDTO lastLog;
    private NextLogDTO nextLog;
    private List<Map<String, Object>> comments;
    private String tocHtml;
    private OutlineVO toc;

    public OutlineVO getToc() {
        return toc;
    }

    public void setToc(OutlineVO toc) {
        this.toc = toc;
    }

    public List<TagsDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagsDTO> tags) {
        this.tags = tags;
    }

    public LastLogDTO getLastLog() {
        return lastLog;
    }

    public void setLastLog(LastLogDTO lastLog) {
        this.lastLog = lastLog;
    }

    public NextLogDTO getNextLog() {
        return nextLog;
    }

    public void setNextLog(NextLogDTO nextLog) {
        this.nextLog = nextLog;
    }


    public static class LastLogDTO {
        private String title;
        private String alias;
        private String url;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class NextLogDTO {
        private String title;
        private String alias;
        private String url;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class TagsDTO {
        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public List<Map<String, Object>> getComments() {
        return comments;
    }

    public void setComments(List<Map<String, Object>> comments) {
        this.comments = comments;
    }

    public String getTocHtml() {
        return tocHtml;
    }

    public void setTocHtml(String tocHtml) {
        this.tocHtml = tocHtml;
    }
}
