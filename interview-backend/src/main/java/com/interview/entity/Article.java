package com.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String docUrl;
    /** tech=技术问题专栏 topic=文章专栏(原专题分享) learn=学习专题 */
    private String columnType;
    /** 学习专题分类ID（仅 column_type=learn 使用） */
    private Long learnCategoryId;
    private Long categoryId;
    private String difficulty;
    private Integer status;
    private Integer sortOrder;
    /** 0=普通 1=置顶（文章专栏内生效） */
    private Integer isPinned;
    /** 封面图 URL（文章/学习可选，空则前端显示占位） */
    private String coverUrl;
    private String contentMd;
    private String contentHtml;
    private Long viewCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
