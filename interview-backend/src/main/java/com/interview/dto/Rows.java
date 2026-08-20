package com.interview.dto;

import lombok.Data;

/**
 * Mapper 聚合查询的行对象，替代 Map&lt;String,Object&gt;，保证类型安全。
 */
public class Rows {

    @Data
    public static class ViewCountRow {
        private Long id;
        private Long viewCount;
    }

    @Data
    public static class TopArticleRow {
        private Long id;
        private String slug;
        private String title;
        private Long viewCount;
        private String categoryName;
    }

    @Data
    public static class PublishedStatsRow {
        private Long articleCount;
        private Long viewCount;
    }

    @Data
    public static class CategoryAggRow {
        private Long categoryId;
        private Integer articleCount;
        private Long viewCount;
    }

    @Data
    public static class TagNameRow {
        private Long articleId;
        private String tagName;
    }

    @Data
    public static class HotTagRow {
        private String name;
        private Long count;
    }
}
