package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.PageResult;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.User;
import com.interview.entity.UserUpload;
import com.interview.enums.AdminLogAction;
import com.interview.mapper.UserMapper;
import com.interview.mapper.UserUploadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserUploadService {

    private final UserUploadMapper userUploadMapper;
    private final UserMapper userMapper;
    private final AdminLogService adminLogService;
    private final NotificationService notificationService;
    private final ContentRenderService contentRenderService;

    /**
     * 普通用户上传 Markdown 内容：读取文件原文，复用 MarkdownService 渲染 HTML 后入库。
     */
    @Transactional
    public VOs.UserUploadListItemVO create(Long userId, Requests.UserUploadSaveDTO dto, MultipartFile file) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请输入标题");
        }
        if (!StringUtils.hasText(dto.getCategoryName())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择或填写主题分类");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择要上传的 Markdown 文件");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".md") && !lower.endsWith(".markdown")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持 .md 或 .markdown 文件");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文档不能超过 20MB");
        }
        String contentMd;
        try {
            contentMd = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "文件读取失败，请重试");
        }
        if (!StringUtils.hasText(contentMd)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件内容为空，请检查后重新上传");
        }

        UserUpload upload = new UserUpload();
        upload.setUserId(userId);
        upload.setTitle(dto.getTitle().trim());
        upload.setCategoryName(dto.getCategoryName().trim());
        upload.setGroupName(dto.getGroupName() == null ? "" : dto.getGroupName().trim());
        upload.setFileName(fileName);
        // md 里的图片自动上传 MinIO 并重写 URL（未配置 MinIO 时原样返回），再渲染为消毒后的 HTML
        ContentRenderService.RenderedContent rc = contentRenderService.render(contentMd, "upload");
        upload.setContentMd(rc.contentMd());
        upload.setContentHtml(rc.contentHtml());
        upload.setStatus(0);
        upload.setAdminReply("");
        userUploadMapper.insert(upload);
        User user = userMapper.selectById(userId);
        notificationService.notifyAdminNewUpload(upload, user);
        return toListItem(upload, null, null, null);
    }

    /** 当前用户自己的上传列表。 */
    public PageResult<VOs.UserUploadListItemVO> myList(Long userId, long page, long size) {
        Page<UserUpload> result = userUploadMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserUpload>()
                        .eq(UserUpload::getUserId, userId)
                        .orderByDesc(UserUpload::getId));
        List<VOs.UserUploadListItemVO> list = result.getRecords().stream()
                .map(u -> toListItem(u, null, null, null))
                .toList();
        return PageResult.of(page, size, result.getTotal(), list);
    }

    /** 当前用户查看自己的上传详情（含原文与预览，可看管理员回复）。 */
    public VOs.UserUploadDetailVO myDetail(Long userId, Long id) {
        UserUpload upload = requireById(id);
        if (!upload.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该内容");
        }
        return toDetail(upload, null);
    }

    /**
     * 管理员列表：支持按标题/主题/分组关键字或用户（昵称/邮箱/手机号）搜索，返回结果附带用户信息。
     */
    public PageResult<VOs.UserUploadListItemVO> adminList(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<UserUpload> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> userIds = findUserIdsByKeyword(kw);
            qw.and(w -> {
                w.like(UserUpload::getTitle, kw)
                        .or().like(UserUpload::getCategoryName, kw)
                        .or().like(UserUpload::getGroupName, kw)
                        .or().like(UserUpload::getFileName, kw);
                if (!userIds.isEmpty()) {
                    w.or().in(UserUpload::getUserId, userIds);
                }
            });
        }
        if (status != null) {
            qw.eq(UserUpload::getStatus, status);
        }
        qw.orderByDesc(UserUpload::getId);

        Page<UserUpload> result = userUploadMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, User> userMap = loadUsers(result.getRecords());
        List<VOs.UserUploadListItemVO> list = result.getRecords().stream()
                .map(u -> {
                    User user = userMap.get(u.getUserId());
                    return toListItem(u, user == null ? null : user.getNickname(),
                            user == null ? null : user.getEmail(), user == null ? null : user.getPhone());
                })
                .toList();
        return PageResult.of(page, size, result.getTotal(), list);
    }

    /** 管理员查看上传详情：包含用户信息、Markdown 原文与渲染预览。 */
    public VOs.UserUploadDetailVO adminDetail(Long id) {
        UserUpload upload = requireById(id);
        User user = userMapper.selectById(upload.getUserId());
        return toDetail(upload, user);
    }

    /** 管理员回复：仅可回复一次，保存回复内容并置为已回复，同时给上传者发通知。 */
    @Transactional
    public VOs.UserUploadDetailVO reply(Long id, String content) {
        UserUpload upload = requireById(id);
        if (StringUtils.hasText(upload.getAdminReply()) || upload.getRepliedAt() != null) {
            throw new BizException(ErrorCode.CONFLICT, "该内容已回复，不能重复回复");
        }
        upload.setAdminReply(content.trim());
        upload.setStatus(1);
        upload.setRepliedAt(LocalDateTime.now());
        userUploadMapper.updateById(upload);

        adminLogService.write(AdminLogAction.UPLOAD_REPLY, upload.getId(), "回复用户上传内容：" + upload.getTitle());
        notificationService.notifyUploadReplied(upload, upload.getUserId());
        User user = userMapper.selectById(upload.getUserId());
        return toDetail(upload, user);
    }

    /** 用户删除自己的上传。 */
    @Transactional
    public void deleteByUser(Long userId, Long id) {
        UserUpload upload = requireById(id);
        if (!upload.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权删除该内容");
        }
        userUploadMapper.deleteById(id);
        notificationService.deleteByUploadId(id);
    }

    /** 管理员删除任意上传。 */
    @Transactional
    public void deleteByAdmin(Long id) {
        UserUpload upload = requireById(id);
        userUploadMapper.deleteById(id);
        notificationService.deleteByUploadId(id);
        adminLogService.write(AdminLogAction.UPLOAD_DELETE, id, "删除用户上传内容：" + upload.getTitle());
    }

    private List<Long> findUserIdsByKeyword(String keyword) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .like(User::getNickname, keyword)
                .or().like(User::getEmail, keyword)
                .or().like(User::getPhone, keyword)
                .select(User::getId));
        return users.stream().map(User::getId).toList();
    }

    private Map<Long, User> loadUsers(List<UserUpload> uploads) {
        List<Long> ids = uploads.stream().map(UserUpload::getUserId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private UserUpload requireById(Long id) {
        UserUpload upload = userUploadMapper.selectById(id);
        if (upload == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "上传内容不存在");
        }
        return upload;
    }

    private VOs.UserUploadListItemVO toListItem(UserUpload u, String nickname, String email, String phone) {
        return VOs.UserUploadListItemVO.builder()
                .id(u.getId())
                .userId(u.getUserId())
                .title(u.getTitle())
                .categoryName(u.getCategoryName())
                .groupName(u.getGroupName())
                .fileName(u.getFileName())
                .status(u.getStatus())
                .adminReply(u.getAdminReply())
                .repliedAt(u.getRepliedAt())
                .createdAt(u.getCreatedAt())
                .nickname(nickname)
                .email(email)
                .phone(phone)
                .build();
    }

    private VOs.UserUploadDetailVO toDetail(UserUpload u, User user) {
        return VOs.UserUploadDetailVO.builder()
                .id(u.getId())
                .userId(u.getUserId())
                .title(u.getTitle())
                .categoryName(u.getCategoryName())
                .groupName(u.getGroupName())
                .fileName(u.getFileName())
                .status(u.getStatus())
                .adminReply(u.getAdminReply())
                .repliedAt(u.getRepliedAt())
                .createdAt(u.getCreatedAt())
                .contentMd(u.getContentMd())
                .contentHtml(u.getContentHtml())
                .nickname(user == null ? null : user.getNickname())
                .email(user == null ? null : user.getEmail())
                .phone(user == null ? null : user.getPhone())
                .build();
    }
}
