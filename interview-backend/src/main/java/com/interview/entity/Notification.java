package com.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接收用户ID（普通用户通知） */
    private Long targetUserId;
    /** 接收角色（ADMIN=发给所有管理员） */
    private String targetRole;
    /** UPLOAD_REPLY / NEW_UPLOAD */
    private String type;
    private String content;
    /** 关联的上传内容ID，点击通知可跳转 */
    private Long uploadId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
