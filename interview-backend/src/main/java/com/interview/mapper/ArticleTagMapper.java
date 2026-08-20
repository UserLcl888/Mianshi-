package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.dto.Rows;
import com.interview.entity.ArticleTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

    @Insert("INSERT IGNORE INTO article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    int insertIgnore(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Delete("DELETE FROM article_tag WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);

    @Select("SELECT tag_id FROM article_tag WHERE article_id = #{articleId}")
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);

    /**
     * 批量取多篇文章的标签名（一条 SQL JOIN），返回 articleId + tagName，供列表页消除 N+1。
     */
    @Select("<script>" +
            "SELECT at.article_id AS articleId, t.name AS tagName " +
            "FROM article_tag at JOIN tag t ON t.id = at.tag_id " +
            "WHERE at.article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY at.article_id, t.id" +
            "</script>")
    List<Rows.TagNameRow> selectTagsByArticleIds(@Param("articleIds") Collection<Long> articleIds);

    /**
     * 批量插入题目-标签关联（INSERT IGNORE 去重）。
     */
    @Insert("<script>" +
            "INSERT IGNORE INTO article_tag (article_id, tag_id) VALUES " +
            "<foreach collection='pairs' item='p' separator=','>(#{p.articleId}, #{p.tagId})</foreach>" +
            "</script>")
    int insertIgnoreBatch(@Param("pairs") List<ArticleTag> pairs);

    /**
     * 热门标签 Top N：统计已发布题目下的标签数，一条 SQL 聚合。
     */
    @Select("SELECT t.name AS name, COUNT(at.article_id) AS count " +
            "FROM tag t " +
            "LEFT JOIN article_tag at ON at.tag_id = t.id " +
            "LEFT JOIN article a ON a.id = at.article_id AND a.status = 1 " +
            "GROUP BY t.id, t.name " +
            "ORDER BY count DESC, t.id ASC " +
            "LIMIT #{limit}")
    List<Rows.HotTagRow> selectHotTags(@Param("limit") int limit);
}
