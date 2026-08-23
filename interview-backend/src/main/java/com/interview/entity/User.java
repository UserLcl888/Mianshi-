package com.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String passwordHash;
    @TableField("rootpassword")
    private String rootPassword;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String role;
    private Integer status;
    /** 1=老用户默认全权限，0=新用户需申请 */
    private Integer fullAccess;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
