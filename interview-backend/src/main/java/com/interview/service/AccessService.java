package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.PageResult;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.AccessApply;
import com.interview.entity.Article;
import com.interview.entity.Category;
import com.interview.entity.User;
import com.interview.mapper.AccessApplyMapper;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessService {

    /** 受限内容未授权错误码：前端据此渲染“需申请访问”页面，不弹普通错误提示 */
    public static final int CODE_NEED_APPLY = 40301;
    private static final Duration GRANT_CACHE_TTL = Duration.ofMinutes(10);

    private final AccessApplyMapper accessApplyMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redis;

    // ===================== 权限判断 =====================

    /** 文章详情权限校验：未授权抛 40301。 */
    public void checkArticleAccess(User viewer, Article article, Category category) {
        Long restrictId = restrictedAncestorId(category);
        if (restrictId == null) {
            return;
        }
        if (viewer != null && isGlobalGranted(viewer)) {
            return;
        }
        if (viewer == null) {
            throw new BizException(CODE_NEED_APPLY, "请先登录后申请访问");
        }
        switch (status(viewer.getId(), restrictId)) {
            case "GRANTED" -> {
                return;
            }
            case "PENDING" -> throw new BizException(CODE_NEED_APPLY, "该分类正在审核中，请耐心等待");
            case "REJECTED" -> throw new BizException(CODE_NEED_APPLY, "申请未通过（" + rejectedRemark(viewer.getId(), restrictId) + "）");
            default -> throw new BizException(CODE_NEED_APPLY, "该分类需申请后访问");
        }
    }

    /** 用户对某受限分类的申请状态：NONE / PENDING / REJECTED / GRANTED */
    public String status(Long userId, Long categoryId) {
        GrantInfo grants = grants(userId);
        if (grants.all || grants.categoryIds.contains(categoryId)) {
            return "GRANTED";
        }
        List<AccessApply> applies = appliesOf(userId);
        boolean pending = applies.stream().anyMatch(a -> a.getStatus() != null && a.getStatus() == 0
                && ("ALL".equals(a.getScope()) || categoryId.equals(a.getCategoryId())));
        if (pending) {
            return "PENDING";
        }
        boolean rejected = applies.stream().anyMatch(a -> a.getStatus() != null && a.getStatus() == 2
                && ("ALL".equals(a.getScope()) || categoryId.equals(a.getCategoryId())));
        return rejected ? "REJECTED" : "NONE";
    }

    /** 按 slug 查文章或分类的访问状态（前端分类页/文章详情页共用）。 */
    public VOs.AccessStatusVO statusBySlug(Long userId, String slug) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, slug).eq(Article::getStatus, 1));
        if (article != null) {
            Category category = categoryService.getById(article.getCategoryId());
            return buildStatusVO("ARTICLE", article.getId(), article.getTitle(), userId, category);
        }
        Category category = categoryService.getBySlug(slug);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "内容不存在");
        }
        return buildStatusVO("CATEGORY", category.getId(), category.getName(), userId, category);
    }

    private VOs.AccessStatusVO buildStatusVO(String type, Long id, String title, Long userId, Category category) {
        Long restrictId = restrictedAncestorId(category);
        User user = userMapper.selectById(userId);
        String status;
        String remark = null;
        Long categoryId = restrictId;
        String categoryName = restrictId == null ? null : categoryName(restrictId);
        if (restrictId == null || (user != null && isGlobalGranted(user))) {
            status = "GRANTED";
        } else {
            status = status(userId, restrictId);
            if ("REJECTED".equals(status)) {
                remark = rejectedRemark(userId, restrictId);
            }
        }
        return VOs.AccessStatusVO.builder()
                .type(type)
                .id(id)
                .title(title)
                .status(status)
                .reviewRemark(remark)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .build();
    }

    /** 当前用户没有权限的受限顶级分类清单（“申请全部”弹窗只展示分类，不含分类内文档）。 */
    public List<VOs.LockedArticleVO> lockedCategories(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && isGlobalGranted(user)) {
            return List.of();
        }
        GrantInfo grant = grants(userId);
        List<VOs.LockedArticleVO> result = new ArrayList<>();
        for (VOs.CategoryVO node : categoryService.tree()) {
            if ("APPLY".equals(node.getAccessLevel()) && !grant.all && !grant.categoryIds.contains(node.getId())) {
                result.add(VOs.LockedArticleVO.builder()
                        .id(node.getId())
                        .name(node.getName())
                        .slug(node.getSlug())
                        .build());
            }
        }
        return result;
    }

    // ===================== 申请 =====================

    @Transactional
    public void apply(Long userId, Requests.AccessApplyDTO dto) {
        String scope = dto.getScope() == null ? "" : dto.getScope().trim();
        String reason = dto.getReason() == null ? "" : dto.getReason().trim();
        if (!"CATEGORY".equals(scope) && !"ALL".equals(scope)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "申请范围不合法");
        }
        AccessApply apply = new AccessApply();
        apply.setUserId(userId);
        apply.setScope(scope);
        apply.setReason(reason);
        apply.setStatus(0);
        apply.setAdminReply("");
        apply.setReviewRemark("");
        String categoryName = "";
        if ("CATEGORY".equals(scope)) {
            if (dto.getCategoryId() == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "请选择要申请的分类");
            }
            Category category = categoryService.getById(dto.getCategoryId());
            Long restrictId = restrictedAncestorId(category);
            if (restrictId == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "该分类无需申请即可访问");
            }
            if (hasActiveApply(userId, scope, restrictId)) {
                throw new BizException(ErrorCode.CONFLICT, "该分类已申请过，请等待审批");
            }
            apply.setCategoryId(restrictId);
            categoryName = categoryName(restrictId);
        } else {
            if (hasActiveApply(userId, scope, null)) {
                throw new BizException(ErrorCode.CONFLICT, "已申请全部受限分类，请等待审批");
            }
            categoryName = "全部受限分类";
        }
        accessApplyMapper.insert(apply);
        User user = userMapper.selectById(userId);
        String nickname = user != null && StringUtils.hasText(user.getNickname()) ? user.getNickname()
                : (user != null && StringUtils.hasText(user.getEmail()) ? user.getEmail() : "用户");
        notificationService.notifyAdminNewApply(nickname, scope, categoryName);
    }

    public List<VOs.AccessApplyVO> myList(Long userId) {
        List<AccessApply> applies = appliesOf(userId);
        return applies.stream().map(a -> toVO(a, null)).toList();
    }

    // ===================== 管理端 =====================

    public PageResult<VOs.AccessApplyVO> adminList(String keyword, Integer status, String scope, long page, long size) {
        LambdaQueryWrapper<AccessApply> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getNickname, kw).or().like(User::getEmail, kw).or().like(User::getPhone, kw)
                            .select(User::getId))
                    .stream().map(User::getId).toList();
            List<Long> categoryIds = categoryService.listByLikeName(kw).stream()
                    .map(Category::getId).toList();
            if (userIds.isEmpty() && categoryIds.isEmpty()) {
                return PageResult.of(page, size, 0, List.of());
            }
            qw.and(w -> {
                boolean first = true;
                if (!userIds.isEmpty()) {
                    w.in(AccessApply::getUserId, userIds);
                    first = false;
                }
                if (!categoryIds.isEmpty()) {
                    if (!first) {
                        w.or();
                    }
                    w.in(AccessApply::getCategoryId, categoryIds);
                }
            });
        }
        if (status != null) {
            qw.eq(AccessApply::getStatus, status);
        }
        if (StringUtils.hasText(scope)) {
            qw.eq(AccessApply::getScope, scope.trim());
        }
        qw.orderByDesc(AccessApply::getId);

        Page<AccessApply> result = accessApplyMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, User> userMap = loadUsers(result.getRecords());
        List<VOs.AccessApplyVO> list = result.getRecords().stream()
                .map(a -> {
                    VOs.AccessApplyVO vo = toVO(a, userMap.get(a.getUserId()));
                    return vo;
                })
                .toList();
        return PageResult.of(page, size, result.getTotal(), list);
    }

    public VOs.AccessApplyVO adminDetail(Long id) {
        AccessApply apply = require(id);
        User user = userMapper.selectById(apply.getUserId());
        return toVO(apply, user);
    }

    @Transactional
    public void approve(Long id, String remark) {
        AccessApply apply = require(id);
        if (apply.getStatus() != null && apply.getStatus() == 1) {
            throw new BizException(ErrorCode.CONFLICT, "该申请已通过");
        }
        apply.setStatus(1);
        apply.setReviewRemark(remark == null ? "" : remark.trim());
        apply.setReviewedAt(LocalDateTime.now());
        accessApplyMapper.updateById(apply);
        invalidate(apply.getUserId());
        // 申请全部受限分类并审批通过：直接置 user.full_access=1，权限判断第一步即可快速放行
        if ("ALL".equals(apply.getScope())) {
            User user = userMapper.selectById(apply.getUserId());
            if (user != null && (user.getFullAccess() == null || user.getFullAccess() != 1)) {
                user.setFullAccess(1);
                userMapper.updateById(user);
            }
        }
        String content = "ALL".equals(apply.getScope())
                ? "你已开通全部受限分类的访问权限"
                : "你申请的分类《" + categoryName(apply.getCategoryId()) + "》已开通访问";
        notificationService.notifyUser(apply.getUserId(), NotificationService.TYPE_ACCESS_APPROVED, content, apply.getCategoryId());
    }

    @Transactional
    public void reject(Long id, String remark) {
        AccessApply apply = require(id);
        if (apply.getStatus() != null && apply.getStatus() == 1) {
            throw new BizException(ErrorCode.CONFLICT, "该申请已通过，不能拒绝");
        }
        apply.setStatus(2);
        apply.setReviewRemark(remark == null ? "" : remark.trim());
        apply.setReviewedAt(LocalDateTime.now());
        accessApplyMapper.updateById(apply);
        invalidate(apply.getUserId());
        String target = "ALL".equals(apply.getScope()) ? "全部受限分类" : "分类《" + categoryName(apply.getCategoryId()) + "》";
        String reason = StringUtils.hasText(apply.getReviewRemark()) ? apply.getReviewRemark() : "未说明原因";
        notificationService.notifyUser(apply.getUserId(), NotificationService.TYPE_ACCESS_REJECTED,
                "你申请的" + target + "未通过（原因：" + reason + "）", apply.getCategoryId());
    }

    @Transactional
    public void reply(Long id, String content) {
        AccessApply apply = require(id);
        apply.setAdminReply(content.trim());
        accessApplyMapper.updateById(apply);
        notificationService.notifyUser(apply.getUserId(), NotificationService.TYPE_ACCESS_REPLY,
                "管理员回复：" + content.trim(), apply.getCategoryId());
    }

    @Transactional
    public void delete(Long id) {
        AccessApply apply = require(id);
        accessApplyMapper.deleteById(id);
        invalidate(apply.getUserId());
    }

    // ===================== 内部实现 =====================

    private boolean isGlobalGranted(User user) {
        return "ADMIN".equals(user.getRole()) || (user.getFullAccess() != null && user.getFullAccess() == 1);
    }

    /** 从当前分类向上找最近/最顶层的 APPLY 祖先分类ID；null=不受限。 */
    private Long restrictedAncestorId(Category category) {
        Map<Long, VOs.CategoryVO> map = categoryMap();
        Long id = category.getId();
        Long restrictId = null;
        while (id != null && id != 0L) {
            VOs.CategoryVO node = map.get(id);
            if (node == null) {
                break;
            }
            if ("APPLY".equals(node.getAccessLevel())) {
                restrictId = id;
            }
            id = node.getParentId();
        }
        return restrictId;
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return "";
        }
        try {
            return categoryService.getById(categoryId).getName();
        } catch (BizException e) {
            return "未知分类";
        }
    }

    private Map<Long, VOs.CategoryVO> categoryMap() {
        Map<Long, VOs.CategoryVO> map = new HashMap<>();
        for (VOs.CategoryVO node : categoryService.tree()) {
            collectCategoryNode(node, map);
        }
        return map;
    }

    private void collectCategoryNode(VOs.CategoryVO node, Map<Long, VOs.CategoryVO> map) {
        map.put(node.getId(), node);
        for (VOs.CategoryVO child : node.getChildren()) {
            collectCategoryNode(child, map);
        }
    }

    private List<AccessApply> appliesOf(Long userId) {
        return accessApplyMapper.selectList(new LambdaQueryWrapper<AccessApply>()
                .eq(AccessApply::getUserId, userId)
                .orderByDesc(AccessApply::getId));
    }

    private boolean hasActiveApply(Long userId, String scope, Long categoryId) {
        return accessApplyMapper.selectCount(new LambdaQueryWrapper<AccessApply>()
                .eq(AccessApply::getUserId, userId)
                .eq(AccessApply::getScope, scope)
                .eq(categoryId != null, AccessApply::getCategoryId, categoryId)
                .in(AccessApply::getStatus, 0, 1)) > 0;
    }

    private String rejectedRemark(Long userId, Long categoryId) {
        return appliesOf(userId).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 2
                        && ("ALL".equals(a.getScope()) || categoryId.equals(a.getCategoryId())))
                .findFirst()
                .map(a -> StringUtils.hasText(a.getReviewRemark()) ? a.getReviewRemark() : "未说明原因")
                .orElse("未说明原因");
    }

    private AccessApply require(Long id) {
        AccessApply apply = accessApplyMapper.selectById(id);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "申请记录不存在");
        }
        return apply;
    }

    private Map<Long, User> loadUsers(List<AccessApply> applies) {
        List<Long> ids = applies.stream().map(AccessApply::getUserId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private VOs.AccessApplyVO toVO(AccessApply a, User user) {
        boolean all = "ALL".equals(a.getScope());
        String categoryName = all ? "全部受限分类"
                : (a.getCategoryId() == null ? "未知分类" : categoryName(a.getCategoryId()));
        String categorySlug = null;
        if (!all && a.getCategoryId() != null) {
            try {
                categorySlug = categoryService.getById(a.getCategoryId()).getSlug();
            } catch (BizException ignored) {
            }
        }
        return VOs.AccessApplyVO.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .scope(a.getScope())
                .categoryId(a.getCategoryId())
                .categoryName(categoryName)
                .categorySlug(categorySlug)
                .reason(a.getReason())
                .status(a.getStatus())
                .adminReply(a.getAdminReply())
                .reviewRemark(a.getReviewRemark())
                .createdAt(a.getCreatedAt())
                .reviewedAt(a.getReviewedAt())
                .nickname(user == null ? null : user.getNickname())
                .email(user == null ? null : user.getEmail())
                .phone(user == null ? null : user.getPhone())
                .build();
    }

    /** 用户已授权分类缓存：all 标记 + 已授权分类ID集合。 */
    private GrantInfo grants(Long userId) {
        String allKey = grantKey(userId) + ":all";
        String idsKey = grantKey(userId) + ":categories";
        String cachedAll = redis.opsForValue().get(allKey);
        String cachedIds = redis.opsForValue().get(idsKey);
        if (cachedAll != null && cachedIds != null) {
            return new GrantInfo("1".equals(cachedAll), parseIds(cachedIds));
        }
        boolean all = false;
        Set<Long> ids = new HashSet<>();
        for (AccessApply a : appliesOf(userId)) {
            if (a.getStatus() != null && a.getStatus() == 1) {
                if ("ALL".equals(a.getScope())) {
                    all = true;
                } else if (a.getCategoryId() != null) {
                    ids.add(a.getCategoryId());
                }
            }
        }
        redis.opsForValue().set(allKey, all ? "1" : "0", GRANT_CACHE_TTL);
        redis.opsForValue().set(idsKey, ids.stream().map(String::valueOf).collect(Collectors.joining(",")), GRANT_CACHE_TTL);
        return new GrantInfo(all, ids);
    }

    private String grantKey(Long userId) {
        return "access:grant:" + userId;
    }

    private Set<Long> parseIds(String value) {
        Set<Long> ids = new HashSet<>();
        if (StringUtils.hasText(value)) {
            for (String part : value.split(",")) {
                try {
                    ids.add(Long.valueOf(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    private void invalidate(Long userId) {
        redis.delete(grantKey(userId) + ":all");
        redis.delete(grantKey(userId) + ":categories");
    }

    private record GrantInfo(boolean all, Set<Long> categoryIds) {
    }
}
