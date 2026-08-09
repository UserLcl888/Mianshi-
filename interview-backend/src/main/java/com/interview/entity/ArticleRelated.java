package com.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("article_related")
public class ArticleRelated {
    private Long articleId;
    private Long relatedArticleId;
}
