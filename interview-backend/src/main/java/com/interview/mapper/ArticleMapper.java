package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.dto.Rows;
import com.interview.entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 批量取题目实时浏览量（DB 已落库部分），用于列表/详情/首页展示时叠加 Redis 未落库增量。
     */
    @Select("<script>" +
            "SELECT id, view_count AS viewCount FROM article WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Rows.ViewCountRow> selectViewCounts(@Param("ids") Collection<Long> ids);

    /**
     * 已发布题目浏览量 Top N，一次 JOIN 取分类名，避免逐行查分类。
     */
    @Select("SELECT a.id, a.slug, a.title, a.view_count AS viewCount, COALESCE(c.name, '') AS categoryName " +
            "FROM article a LEFT JOIN category c ON c.id = a.category_id " +
            "WHERE a.status = 1 AND a.column_type NOT IN ('topic', 'learn') " +
            "ORDER BY a.view_count DESC, a.id DESC " +
            "LIMIT #{limit}")
    List<Rows.TopArticleRow> selectTopPublished(@Param("limit") int limit);

    /**
     * 首页站点统计：已发布题目数 + 浏览量总和，单条 SQL 聚合。
     */
    @Select("SELECT COUNT(*) AS articleCount, COALESCE(SUM(view_count), 0) AS viewCount " +
            "FROM article WHERE status = 1 AND column_type NOT IN ('topic', 'learn')")
    Rows.PublishedStatsRow selectPublishedStats();

    /**
     * 各分类题目数/浏览量聚合（含草稿），供管理端分类统计使用。
     */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS articleCount, " +
            "COALESCE(SUM(view_count), 0) AS viewCount " +
            "FROM article WHERE column_type NOT IN ('topic', 'learn') GROUP BY category_id")
    List<Rows.CategoryAggRow> selectCategoryAgg();

    /**
     * 浏览量累加落库（定时任务批量调用）。
     */
    @Update("UPDATE article SET view_count = view_count + #{delta} WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 批量累加浏览量：一条 UPDATE 对多个 id 分别加上各自的增量，减少逐条 UPDATE。
     */
    @Update("<script>" +
            "UPDATE article SET view_count = view_count + (CASE id " +
            "<foreach collection='deltas' index='id' item='d'>WHEN #{id} THEN #{d} </foreach>" +
            "ELSE 0 END) WHERE id IN " +
            "<foreach collection='deltas' index='id' item='d' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int batchIncrementViewCount(@Param("deltas") Map<Long, Long> deltas);
}
