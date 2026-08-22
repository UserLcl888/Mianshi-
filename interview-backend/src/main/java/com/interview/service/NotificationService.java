package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.PageResult;
import com.interview.dto.VOs;
import com.interview.entity.Notification;
import com.interview.entity.User;
import com.interview.entity.UserUpload;
import com.interview.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_UPLOAD_REPLY = "UPLOAD_REPLY";
    public static final String TYPE_NEW_UPLOAD = "NEW_UPLOAD";
    public static final String ROLE_ADMIN = "ADMIN";

    private final NotificationMapper notificationMapper;

    /** 管理员回复后，给上传者发通知。 */
    public void notifyUploadReplied(UserUpload upload, Long ownerUserId) {
        Notification n = new Notification();
        n.setTargetUserId(ownerUserId);
        n.setType(TYPE_UPLOAD_REPLY);
        n.setContent("您上传的内容《" + upload.getTitle() + "》已收到管理员回复");
        n.setUploadId(upload.getId());
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    /**
     * 用户上传新内容后，给所有管理员发通知。
     * 内容格式：昵称（邮箱/手机号）在「分类 / 分组」新增了标题为《标题》的文档提交
     */
    public void notifyAdminNewUpload(UserUpload upload, User user) {
        String nickname = user != null && StringUtils.hasText(user.getNickname()) ? user.getNickname() : "用户";
        String account = user != null && StringUtils.hasText(user.getEmail()) ? user.getEmail()
                : (user != null && StringUtils.hasText(user.getPhone()) ? "手机：" + user.getPhone() : "");
        String who = nickname + (StringUtils.hasText(account) ? "（" + account + "）" : "");
        String where = "「" + upload.getCategoryName() + "」";
        if (StringUtils.hasText(upload.getGroupName())) {
            where += " / " + upload.getGroupName();
        }
        Notification n = new Notification();
        n.setTargetRole(ROLE_ADMIN);
        n.setType(TYPE_NEW_UPLOAD);
        n.setContent(who + " 在 " + where + " 新增了标题为《" + upload.getTitle() + "》的文档提交");
        n.setUploadId(upload.getId());
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    /** 删除上传内容时，清理关联通知，避免出现无效跳转。 */
    @Transactional
    public void deleteByUploadId(Long uploadId) {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUploadId, uploadId));
    }

    public PageResult<VOs.NotificationVO> userList(Long userId, long page, long size) {
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetUserId, userId)
                        .orderByDesc(Notification::getId));
        return PageResult.of(page, size, result.getTotal(), toVOList(result));
    }

    public PageResult<VOs.NotificationVO> adminList(long page, long size) {
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetRole, ROLE_ADMIN)
                        .orderByDesc(Notification::getId));
        return PageResult.of(page, size, result.getTotal(), toVOList(result));
    }

    public long userUnreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    public long adminUnreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetRole, ROLE_ADMIN)
                .eq(Notification::getIsRead, 0));
    }

    public void markUserRead(Long userId, Long id) {
        Notification n = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getTargetUserId, userId));
        if (n == null) {
            return;
        }
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    public void markAdminRead(Long id) {
        Notification n = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getTargetRole, ROLE_ADMIN));
        if (n == null) {
            return;
        }
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    public void markUserAllRead(Long userId) {
        Notification n = new Notification();
        n.setIsRead(1);
        notificationMapper.update(n, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    public void markAdminAllRead() {
        Notification n = new Notification();
        n.setIsRead(1);
        notificationMapper.update(n, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetRole, ROLE_ADMIN)
                .eq(Notification::getIsRead, 0));
    }

    private java.util.List<VOs.NotificationVO> toVOList(Page<Notification> result) {
        return result.getRecords().stream().map(n -> VOs.NotificationVO.builder()
                .id(n.getId())
                .type(n.getType())
                .content(n.getContent())
                .uploadId(n.getUploadId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build()).toList();
    }
}
