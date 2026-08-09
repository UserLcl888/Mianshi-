package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.entity.ArticleRelated;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ArticleRelatedMapper extends BaseMapper<ArticleRelated> {

    @Insert("INSERT IGNORE INTO article_related (article_id, related_article_id) VALUES (#{articleId}, #{relatedArticleId})")
    int insertIgnore(@Param("articleId") Long articleId, @Param("relatedArticleId") Long relatedArticleId);

    @Delete("DELETE FROM article_related WHERE article_id = #{articleId} OR related_article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT related_article_id FROM article_related WHERE article_id = #{articleId}")
    List<Long> selectRelatedIdsByArticleId(@Param("articleId") Long articleId);
}
