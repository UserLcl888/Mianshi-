package com.interview.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VOs {

    @Data
    @Builder
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
        private String rootPassword;
        private String avatar;
        private String email;
        private String phone;
        private String role;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class CategoryVO {
        private Long id;
        private String name;
        private String slug;
        private Long parentId;
        private Integer sortOrder;
        private String description;
        private List<CategoryVO> children = new ArrayList<>();
    }

    @Data
    @Builder
    public static class ArticleListItemVO {
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private Long categoryId;
        private String difficulty;
        private List<String> tags;
        private Long viewCount;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class TocItemVO {
        private String id;
        private String text;
        private Integer level;

        public TocItemVO() {
        }

        public TocItemVO(String id, String text, Integer level) {
            this.id = id;
            this.text = text;
            this.level = level;
        }
    }

    @Data
    @Builder
    public static class ArticleDetailVO {
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private String docUrl;
        private Long categoryId;
        private String categoryName;
        private String categorySlug;
        private String difficulty;
        private List<String> tags;
        private String contentMd;
        private String contentHtml;
        private List<TocItemVO> toc;
        private Long viewCount;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class ArticleBriefVO {
        private Long id;
        private String slug;
        private String title;
    }

    @Data
    public static class DetailRespVO {
        private ArticleDetailVO article;
        private ArticleBriefVO prev;
        private ArticleBriefVO next;
    }

    @Data
    @Builder
    public static class LoginResultVO {
        private String token;
        private UserVO userInfo;
    }

    @Data
    public static class TopArticleVO {
        private Long id;
        private String title;
        private String categoryName;
        private Long viewCount;
    }

    @Data
    @Builder
    public static class CategoryStatsVO {
        private Long id;
        private String name;
        private Long viewCount;
        private Integer articleCount;
    }
}
