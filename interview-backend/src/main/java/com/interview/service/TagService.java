package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.dto.Rows;
import com.interview.dto.VOs;
import com.interview.entity.ArticleTag;
import com.interview.entity.Tag;
import com.interview.enums.AdminLogAction;
import com.interview.mapper.ArticleTagMapper;
import com.interview.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ContentCacheService contentCacheService;
    private final AdminLogService adminLogService;

    public VOs.TagVO create(String name) {
        String trimmed = trimName(name);
        if (tagMapper.selectByNames(List.of(trimmed)).stream().anyMatch(t -> t.getName().equals(trimmed))) {
            throw new BizException(ErrorCode.CONFLICT, "标签已存在");
        }
        Tag tag = new Tag();
        tag.setName(trimmed);
        tagMapper.insert(tag);
        contentCacheService.bump();
        Tag saved = tagMapper.selectById(tag.getId());
        adminLogService.write(AdminLogAction.TAG_CREATE, saved.getId(), "新增标签 " + saved.getName());
        return toVO(saved);
    }

    public VOs.TagVO update(Long id, String name) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        String trimmed = trimName(name);
        boolean duplicate = tagMapper.selectByNames(List.of(trimmed)).stream()
                .anyMatch(t -> t.getName().equals(trimmed) && !t.getId().equals(id));
        if (duplicate) {
            throw new BizException(ErrorCode.CONFLICT, "标签已存在");
        }
        tag.setName(trimmed);
        tagMapper.updateById(tag);
        contentCacheService.bump();
        adminLogService.write(AdminLogAction.TAG_UPDATE, tag.getId(), "编辑标签 " + tag.getName());
        return toVO(tag);
    }

    public void delete(Long id) {
        articleTagMapper.deleteByTagId(id);
        tagMapper.deleteById(id);
        contentCacheService.bump();
        adminLogService.write(AdminLogAction.TAG_DELETE, id, "删除标签");
    }

    public List<String> namesByArticleId(Long articleId) {
        return namesByArticleIds(List.of(articleId)).getOrDefault(articleId, List.of());
    }

    /**
     * 批量取多篇文章的标签名（一条 SQL JOIN），消除列表/详情逐行查询的 N+1。
     */
    public Map<Long, List<String>> namesByArticleIds(Collection<Long> articleIds) {
        Map<Long, List<String>> result = new HashMap<>();
        if (articleIds == null || articleIds.isEmpty()) {
            return result;
        }
        articleTagMapper.selectTagsByArticleIds(articleIds).forEach(row -> {
            result.computeIfAbsent(row.getArticleId(), k -> new ArrayList<>()).add(row.getTagName());
        });
        return result;
    }

    @Transactional
    public void replaceArticleTags(Long articleId, List<String> tagNames) {
        articleTagMapper.deleteByArticleId(articleId);
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String name : tagNames) {
            String trimmed = name == null ? "" : name.trim();
            if (StringUtils.hasText(trimmed)) {
                names.add(trimmed);
            }
        }
        if (names.isEmpty()) {
            return;
        }

        // 1) 批量查已存在标签
        Map<String, Long> existing = tagMapper.selectByNames(names).stream()
                .collect(Collectors.toMap(Tag::getName, Tag::getId));

        // 2) 缺失标签批量插入
        List<String> missing = names.stream().filter(n -> !existing.containsKey(n)).toList();
        if (!missing.isEmpty()) {
            List<Tag> newTags = missing.stream().map(name -> {
                Tag tag = new Tag();
                tag.setName(name);
                return tag;
            }).toList();
            tagMapper.insertBatch(newTags);
            tagMapper.selectByNames(missing).forEach(t -> existing.putIfAbsent(t.getName(), t.getId()));
        }

        // 3) 批量写入关联表
        List<ArticleTag> pairs = names.stream()
                .map(n -> {
                    ArticleTag pair = new ArticleTag();
                    pair.setArticleId(articleId);
                    pair.setTagId(existing.get(n));
                    return pair;
                })
                .filter(p -> p.getTagId() != null)
                .toList();
        if (!pairs.isEmpty()) {
            articleTagMapper.insertIgnoreBatch(pairs);
        }
    }

    public List<VOs.TagVO> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId))
                .stream().map(this::toVO).toList();
    }

    private String trimName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "标签名不能为空");
        }
        return trimmed;
    }

    private VOs.TagVO toVO(Tag tag) {
        return VOs.TagVO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
