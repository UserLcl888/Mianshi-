package com.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Integer sortOrder;
    private String description;
    /** PUBLIC=公开 APPLY=需申请 */
    private String accessLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
