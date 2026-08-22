package com.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_upload")
public class UserUpload {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String categoryName;
    private String groupName;
    private String fileName;
    private String contentMd;
    private String contentHtml;
    private Integer status;
    private String adminReply;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
