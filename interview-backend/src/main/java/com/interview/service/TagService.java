package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.entity.Tag;
import com.interview.mapper.ArticleTagMapper;
import com.interview.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    public List<String> namesByArticleId(Long articleId) {
        List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(articleId);
        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return tagMapper.selectBatchIds(tagIds).stream().map(Tag::getName).toList();
    }

    @Transactional
    public void replaceArticleTags(Long articleId, List<String> tagNames) {
        articleTagMapper.deleteByArticleId(articleId);
        if (tagNames == null) {
            return;
        }
        for (String name : tagNames) {
            String trimmed = name == null ? "" : name.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, trimmed));
            if (tag == null) {
                tag = new Tag();
                tag.setName(trimmed);
                tagMapper.insert(tag);
            }
            articleTagMapper.insertIgnore(articleId, tag.getId());
        }
    }

    public List<Tag> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
    }
}
