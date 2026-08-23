package com.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("access_apply")
public class AccessApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** CATEGORY=单分类 ALL=全部分类 */
    private String scope;
    /** scope=CATEGORY 时申请的分类ID */
    private Long categoryId;
    private String reason;
    /** 0待审批 1已通过 2已拒绝 */
    private Integer status;
    private String adminReply;
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
