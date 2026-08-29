-- =====================================================================
-- 知识分享平台 · 最终数据库脚本（自包含，可重复执行）
-- 覆盖：专栏字段 + 学习分类表 + 文章/学习示例数据
-- 用法：MySQL 8.x，在 interview 库直接执行
-- =====================================================================

USE `interview`;

-- ---------------------------------------------------------------
-- 1) article 表新增专栏字段（已存在则跳过）
-- ---------------------------------------------------------------
SET @c1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'interview' AND TABLE_NAME = 'article' AND COLUMN_NAME = 'column_type');
SET @s1 := IF(@c1 = 0,
  'ALTER TABLE `article` ADD COLUMN `column_type` varchar(20) NOT NULL DEFAULT ''tech'' COMMENT ''tech=技术问题专栏 topic=文章专栏(原专题分享) learn=学习专题'' AFTER `category_id`',
  'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

SET @c2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'interview' AND TABLE_NAME = 'article' AND COLUMN_NAME = 'is_pinned');
SET @s2 := IF(@c2 = 0,
  'ALTER TABLE `article` ADD COLUMN `is_pinned` tinyint NOT NULL DEFAULT 0 COMMENT ''0=普通 1=置顶（文章专栏内生效）'' AFTER `sort_order`',
  'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;

SET @c3 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'interview' AND TABLE_NAME = 'article' AND COLUMN_NAME = 'cover_url');
SET @s3 := IF(@c3 = 0,
  'ALTER TABLE `article` ADD COLUMN `cover_url` varchar(500) NOT NULL DEFAULT '''' COMMENT ''封面图URL（文章/学习可选）'' AFTER `doc_url`',
  'SELECT 1');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;

SET @c4 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'interview' AND TABLE_NAME = 'article' AND COLUMN_NAME = 'learn_category_id');
SET @s4 := IF(@c4 = 0,
  'ALTER TABLE `article` ADD COLUMN `learn_category_id` bigint unsigned DEFAULT NULL COMMENT ''学习专题分类ID（仅 column_type=learn 使用）'' AFTER `category_id`',
  'SELECT 1');
PREPARE st4 FROM @s4; EXECUTE st4; DEALLOCATE PREPARE st4;

SET @i1 := (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = 'interview' AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_column');
SET @s5 := IF(@i1 = 0,
  'ALTER TABLE `article` ADD KEY `idx_column` (`column_type`,`status`,`is_pinned`,`sort_order`)',
  'SELECT 1');
PREPARE st5 FROM @s5; EXECUTE st5; DEALLOCATE PREPARE st5;

-- ---------------------------------------------------------------
-- 2) 存量数据归入技术问题专栏（空值兜底）
-- ---------------------------------------------------------------
UPDATE `article` SET `column_type` = 'tech'
WHERE `column_type` IS NULL OR `column_type` = '';

-- ---------------------------------------------------------------
-- 3) 学习专题分类表 + 预置分类
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `learn_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '分类名',
  `slug` varchar(80) NOT NULL COMMENT 'URL标识',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，越小越靠前',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习专题分类表';

INSERT IGNORE INTO `learn_category` (`name`, `slug`, `sort_order`) VALUES
('Java', 'learn-java', 1),
('AI', 'learn-ai', 2),
('MySQL', 'learn-mysql', 3);

-- ---------------------------------------------------------------
-- 4) 示例数据：文章专栏 2 条 + 学习专题 3 条（带学习分类映射）
-- ---------------------------------------------------------------
INSERT IGNORE INTO `article`
  (`id`, `slug`, `title`, `summary`, `doc_url`, `column_type`, `category_id`, `learn_category_id`,
   `difficulty`, `status`, `sort_order`, `is_pinned`, `cover_url`, `content_md`, `content_html`,
   `view_count`, `created_by`, `created_at`, `updated_at`)
VALUES
('9001', 'topic-codex-tips', 'Codex 高效使用技巧',
 '汇总 Codex 常用配置、模型选择与效率技巧，帮助你更快上手。',
 '', 'topic', '0', NULL, 'MEDIUM', '1', '1', '1', '',
 '## 基础配置\n\n- 设置默认模型与推理强度\n- 学会用技能 Skill 扩展能力\n\n## 日常提效\n\n- 善用注释说明意图\n- 大任务拆分成小步骤\n\n## 避坑指南\n\n- 网络受限时先本地验证\n- 重要改动前先看现有代码',
 '<h2 id="base">基础配置</h2><p>设置默认模型与推理强度，学会用技能 Skill 扩展能力。</p><h2 id="tips">日常提效</h2><p>善用注释说明意图，大任务拆分成小步骤。</p><h2 id="pits">避坑指南</h2><p>网络受限时先本地验证，重要改动前先看现有代码。</p>',
 '0', '1', '2026-08-29 10:00:00', '2026-08-29 10:00:00'),

('9002', 'topic-ai-knowledge', 'AI 知识库搭建入门',
 '从 RAG 到 Prompt 的落地实践笔记，适合作为文章分享的第一篇。',
 '', 'topic', '0', NULL, 'EASY', '1', '2', '0', '',
 '## 为什么需要知识库\n\n- 大模型本身不擅长精确检索\n- 外挂知识库可提升回答准确度\n\n## 最小可行方案\n\n- 文档切块与向量化\n- 检索 TopK 后拼接 Prompt\n\n## 常见坑\n\n- 切块粒度影响召回质量\n- 权限与内容安全要提前考虑',
 '<h2 id="why">为什么需要知识库</h2><p>大模型本身不擅长精确检索，外挂知识库可提升回答准确度。</p><h2 id="mvp">最小可行方案</h2><p>文档切块与向量化，检索 TopK 后拼接 Prompt。</p><h2 id="pits">常见坑</h2><p>切块粒度影响召回质量，权限与内容安全要提前考虑。</p>',
 '0', '1', '2026-08-29 10:00:00', '2026-08-29 10:00:00'),

('9101', 'learn-java-annotation', '@RestController 和 @RequiredArgsConstructor 是什么？怎么用？',
 'Spring 常用注解讲解：作用、原理与代码示例。',
 '', 'learn', '0', (SELECT id FROM learn_category WHERE slug = 'learn-java'),
 'MEDIUM', '1', '1', '0', '',
 '## 注解是什么\n\n注解是给类/方法打的标记，框架在运行时读取标记执行逻辑。\n\n## @RestController 怎么用\n\n```java\n@RestController\npublic class UserController {}\n```\n\n- 相当于 @Controller + @ResponseBody\n- 方法返回值直接序列化成 JSON\n\n## @RequiredArgsConstructor 怎么用\n\n```java\n@RequiredArgsConstructor\npublic class UserService {\n    private final UserMapper userMapper;\n}\n```\n\n- 自动为 final 字段生成构造方法\n- 配合构造器注入，替代 @Autowired\n\n## 小结\n\n两者都是 Spring 开发的日常注解，能显著减少样板代码。',
 '<h2 id="what">注解是什么</h2><p>注解是给类/方法打的标记，框架在运行时读取标记执行逻辑。</p><h2 id="rest">@RestController 怎么用</h2><p>@RestController 相当于 @Controller + @ResponseBody，方法返回值直接序列化成 JSON。</p><h2 id="reqargs">@RequiredArgsConstructor 怎么用</h2><p>自动为 final 字段生成构造方法，配合构造器注入，替代 @Autowired。</p><h2 id="summary">小结</h2><p>两者都是 Spring 开发的日常注解，能显著减少样板代码。</p>',
 '0', '1', '2026-08-29 10:00:00', '2026-08-29 10:00:00'),

('9102', 'learn-ai-langchain4j', 'LangChain4j 入门：从 ChatClient 开始',
 '大模型应用开发第一课：在 Java 里调用大模型。',
 '', 'learn', '0', (SELECT id FROM learn_category WHERE slug = 'learn-ai'),
 'MEDIUM', '1', '2', '0', '',
 '## 为什么用 LangChain4j\n\n- Java 生态的大模型开发框架\n- 支持对话、RAG、Function Calling\n\n## 最小示例\n\n```java\nChatClient client = ChatClient.builder(openAiChatModel).build();\nString answer = client.chat(\"你好\");\n```\n\n## 下一步\n\n- 接入 Prompt 模板\n- 把文档切块做 RAG',
 '<h2 id="why">为什么用 LangChain4j</h2><p>Java 生态的大模型开发框架，支持对话、RAG、Function Calling。</p><h2 id="demo">最小示例</h2><p>用 ChatClient 几行代码即可调用大模型。</p><h2 id="next">下一步</h2><p>接入 Prompt 模板，把文档切块做 RAG。</p>',
 '0', '1', '2026-08-29 10:00:00', '2026-08-29 10:00:00'),

('9103', 'learn-mysql-index', '索引是怎么加速查询的？B+ 树入门',
 '从数据页到 B+ 树，理解 MySQL 索引为什么快。',
 '', 'learn', '0', (SELECT id FROM learn_category WHERE slug = 'learn-mysql'),
 'MEDIUM', '1', '3', '0', '',
 '## 没有索引时\n\n全表扫描，每行都要读。\n\n## B+ 树索引\n\n- 数据按顺序组织，查找从 O(n) 降到 O(log n)\n- 叶子节点存数据/指针，适合范围查询\n\n## 使用建议\n\n- 区分度高、查询频繁的列建索引\n- 避免索引失效（函数、隐式转换等）',
 '<h2 id="noscan">没有索引时</h2><p>全表扫描，每行都要读。</p><h2 id="btree">B+ 树索引</h2><p>数据按顺序组织，查找从 O(n) 降到 O(log n)，叶子节点适合范围查询。</p><h2 id="tips">使用建议</h2><p>区分度高、查询频繁的列建索引，避免索引失效。</p>',
 '0', '1', '2026-08-29 10:00:00', '2026-08-29 10:00:00');
