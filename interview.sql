-- =====================================================================
-- 面试题知识库 · 数据库设计 + Mock 数据
-- 说明：
--   1) 本文档包含全部表结构（含用户/管理员头像设计）与种子数据。
--   2) 账号密码均为 123456（BCrypt 哈希已预生成）。
--      管理员：admin / 123456；普通用户：demo / 123456
--   3) 头像设计：user.avatar 存头像 URL（OSS 或本地静态资源），
--      空字符串表示使用默认头像；种子数据使用 /static/avatars/*.png
--      占位路径，实际部署时替换为真实图片地址。
--   4) 直接执行本文件即可创建库、表和 mock 数据（可重复执行）。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `interview` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `interview`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 建表（先删后建，便于重复执行）
-- =====================================================================

DROP TABLE IF EXISTS `admin_log`;
DROP TABLE IF EXISTS `article_tag`;
DROP TABLE IF EXISTS `tag`;
DROP TABLE IF EXISTS `article`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;

-- ---------------------------------------------------------------------
-- 账号表（管理员 + 普通用户）
-- avatar 头像设计：
--   - 存完整 URL（OSS 域名或 /static/avatars/xxx.png 静态路径）
--   - 空字符串 = 使用默认头像（前端兜底展示）
--   - 后续如需"头像库/上传头像"，可扩展 upload 表或对象存储引用
-- ---------------------------------------------------------------------
CREATE TABLE `user` (
  `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `username`      VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `rootpassword`  VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '原始密码（演示/联调用，生产环境勿存明文）',
  `nickname`      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL（OSS或静态资源），空=默认头像',
  `email`         VARCHAR(100) NOT NULL DEFAULT '' COMMENT '邮箱',
  `role`          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `last_login_at` DATETIME     NULL COMMENT '最后登录时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_status` (`status`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表（管理员/普通用户）';

-- ---------------------------------------------------------------------
-- 分类表（支持多级：MySQL → 索引/事务/日志/查询优化/锁/架构与高可用）
-- ---------------------------------------------------------------------
CREATE TABLE `category` (
  `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `name`        VARCHAR(50)  NOT NULL COMMENT '分类名',
  `slug`        VARCHAR(80)  NOT NULL UNIQUE COMMENT 'URL标识',
  `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=顶级分类',
  `sort_order`  INT NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
  `description` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '分类描述',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表（多级）';

-- ---------------------------------------------------------------------
-- 面试题表（一道题 = 一篇文章）
-- ---------------------------------------------------------------------
CREATE TABLE `article` (
  `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `slug`         VARCHAR(100) NOT NULL UNIQUE COMMENT 'URL标识',
  `title`        VARCHAR(200) NOT NULL COMMENT '题目',
  `summary`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '摘要',
  `category_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属分类（可为子分类）',
  `difficulty`   VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' COMMENT 'EASY/MEDIUM/HARD',
  `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布',
  `content_md`   LONGTEXT NOT NULL COMMENT 'Markdown 原文',
  `content_html` LONGTEXT NOT NULL COMMENT '渲染并消毒后的 HTML',
  `view_count`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览量',
  `created_by`   BIGINT UNSIGNED NULL COMMENT '创建人（管理员ID）',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_category` (`category_id`, `status`),
  KEY `idx_status` (`status`),
  KEY `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题表';

-- ---------------------------------------------------------------------
-- 标签表
-- ---------------------------------------------------------------------
CREATE TABLE `tag` (
  `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `name`       VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- ---------------------------------------------------------------------
-- 题目-标签关联表
-- ---------------------------------------------------------------------
CREATE TABLE `article_tag` (
  `article_id` BIGINT UNSIGNED NOT NULL,
  `tag_id`     BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-标签关联';

-- ---------------------------------------------------------------------
-- 管理员操作日志表
-- ---------------------------------------------------------------------
CREATE TABLE `admin_log` (
  `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `admin_id`    BIGINT UNSIGNED NOT NULL COMMENT '操作管理员ID',
  `action`      VARCHAR(50)  NOT NULL COMMENT '动作：USER_DISABLE/ARTICLE_CREATE/ARTICLE_UPDATE...',
  `target_type` VARCHAR(20)  NOT NULL COMMENT '对象类型：USER/CATEGORY/ARTICLE/TAG',
  `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '对象ID',
  `detail`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '操作详情',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_admin` (`admin_id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志';

-- =====================================================================
-- Mock 数据
-- =====================================================================

-- ---------------------------------------------------------------------
-- 账号（密码均为 123456，BCrypt 哈希已生成）
-- ---------------------------------------------------------------------
INSERT INTO `user` (`id`, `username`, `password_hash`, `rootpassword`, `nickname`, `avatar`, `email`, `role`, `status`, `last_login_at`, `created_at`) VALUES
  (1, 'admin', '$2b$10$eJnRBiBzHzWH1GV.Ecei7.DRp9OGDIGFX4Uu1rAMrIJVlc5Plo4mm', '123456', '管理员', '/static/avatars/avatar-admin.png', 'admin@example.com', 'ADMIN', 1, '2026-08-08 10:00:00', '2026-01-01 00:00:00'),
  (2, 'demo',  '$2b$10$eJnRBiBzHzWH1GV.Ecei7.DRp9OGDIGFX4Uu1rAMrIJVlc5Plo4mm', '123456', '演示用户', '/static/avatars/avatar-01.png',  'demo@example.com',  'USER',  1, '2026-08-08 09:30:00', '2026-06-01 09:00:00'),
  (3, 'zhangsan', '$2b$10$eJnRBiBzHzWH1GV.Ecei7.DRp9OGDIGFX4Uu1rAMrIJVlc5Plo4mm', '123456', '张三', '/static/avatars/avatar-02.png', 'zhangsan@example.com', 'USER', 1, '2026-08-07 20:15:00', '2026-06-10 10:00:00'),
  (4, 'lisi',     '$2b$10$eJnRBiBzHzWH1GV.Ecei7.DRp9OGDIGFX4Uu1rAMrIJVlc5Plo4mm', '123456', '李四', '/static/avatars/avatar-03.png', 'lisi@example.com',     'USER', 1, '2026-08-06 18:40:00', '2026-06-15 11:00:00'),
  (5, 'wangwu',   '$2b$10$eJnRBiBzHzWH1GV.Ecei7.DRp9OGDIGFX4Uu1rAMrIJVlc5Plo4mm', '123456', '王五', '/static/avatars/avatar-04.png', 'wangwu@example.com',   'USER', 0, '2026-07-20 12:00:00', '2026-06-20 12:00:00'),
  (6, 'zhaoliu',  '$2b$10$eJnRBiBzHzWH1GV.Ecei7.DRp9OGDIGFX4Uu1rAMrIJVlc5Plo4mm', '123456', '赵六', '/static/avatars/avatar-05.png', 'zhaoliu@example.com',  'USER', 1, '2026-08-05 08:20:00', '2026-07-01 09:00:00'),
  (7, 'xiaoming', '$2b$10$eJnRBiBzHzWH1GV.Ecei7.DRp9OGDIGFX4Uu1rAMrIJVlc5Plo4mm', '123456', '小明', '',                                'xiaoming@example.com', 'USER', 1, '2026-08-08 08:00:00', '2026-07-10 14:00:00');

-- ---------------------------------------------------------------------
-- 分类（6 顶级 + MySQL 6 子分类）
-- ---------------------------------------------------------------------
INSERT INTO `category` (`id`, `name`, `slug`, `parent_id`, `sort_order`, `description`) VALUES
  (1,  '计算机网络',   'computer-network',  0, 9,  'TCP/IP、HTTP、HTTPS 等网络高频面试题'),
  (2,  '操作系统',     'operating-system', 0, 10, '进程、线程、内存管理、文件系统'),
  (3,  'MySQL',        'mysql',            0, 6,  '索引、事务、MVCC、锁、日志'),
  (4,  'Redis',        'redis',            0, 7,  '数据结构、持久化、缓存、高可用'),
  (6,  '系统设计',     'system-design',    0, 12, '高并发、分布式、架构设计'),
  (7,  'Java基础',     'java-basic',       0, 1,  'Java 基础语法与核心对象，如 String、==/equals 等'),
  (8,  'Java集合',     'java-collection',  0, 2,  'ArrayList、HashMap、HashSet 等集合框架高频题'),
  (9,  'JVM',          'jvm',              0, 3,  '内存区域、类加载、垃圾回收等 JVM 面试题'),
  (10, 'JUC',          'juc',              0, 4,  '并发编程：线程池、锁、volatile、CAS'),
  (11, '微服务',       'microservice',     0, 5,  '微服务架构、注册发现、网关、配置中心'),
  (12, 'MQ',           'mq',               0, 8,  '消息队列：异步解耦、削峰、可靠性'),
  (13, '数据结构',     'data-structure',   0, 11, '数组、链表、树、哈希等数据结构'),
  (31, '索引',         'mysql-index',          3, 1, 'B+ 树、回表、覆盖索引等高频索引题'),
  (32, '事务',         'mysql-transaction',    3, 2, '隔离级别、MVCC、幻读'),
  (33, '日志',         'mysql-log',            3, 3, 'redo log、binlog、两阶段提交'),
  (34, '查询优化',     'mysql-query',          3, 4, '慢查询、EXPLAIN、SQL 优化'),
  (35, '锁',           'mysql-lock',           3, 5, '表锁、行锁、间隙锁、死锁'),
  (36, '架构与高可用', 'mysql-architecture',   3, 6, '主从、分库分表、存储引擎');

-- ---------------------------------------------------------------------
-- 面试题（种子 54 道，正文为占位/简版答案，完整答案由管理端录入）
-- 内容为占位文本，完整答案由管理端后台录入
-- ---------------------------------------------------------------------
INSERT INTO `article`
  (`id`, `slug`, `title`, `summary`, `category_id`, `difficulty`, `status`, `content_md`, `content_html`, `view_count`, `created_by`, `created_at`, `updated_at`) VALUES
  (1,  'computer-network-1', 'TCP 为什么需要三次握手？',            '三次握手的必要性、防止历史连接、序列号同步。', 1, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 1337, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (2,  'computer-network-2', 'TCP 与 UDP 的区别是什么？',          '面向连接 vs 无连接、可靠性、传输效率与适用场景。', 1, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 1474, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (3,  'operating-system-1', '进程和线程的区别是什么？',            '资源分配与调度的基本单位，切换开销、共享资源。', 2, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 1611, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (4,  'operating-system-2', '什么是死锁？如何避免？',              '四个必要条件、银行家算法、破坏条件。', 2, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 1748, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (5,  'mysql-1',  'MySQL 索引为什么使用 B+ 树？',          'B+ 树结构与 B 树的对比，适合范围查询与磁盘 IO。', 31, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 1885, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (6,  'mysql-2',  'MVCC 是如何实现的？',                     '隐藏列、undo log、ReadView，快照读与当前读。', 32, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2022, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (7,  'mysql-3',  'MySQL 事务的隔离级别有哪些？',            '读未提交/读已提交/可重复读/串行化，脏读幻读的区别。', 32, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2159, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (8,  'mysql-4',  '什么是索引覆盖？如何避免回表？',          '覆盖索引的原理与使用场景，减少回表 IO。', 31, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2296, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (9,  'mysql-5',  '最左前缀原则是什么？',                   '联合索引的匹配顺序，为什么查询要遵循最左前缀。', 31, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2433, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (10, 'mysql-6',  '慢查询如何优化？',                       '慢日志、EXPLAIN、索引优化、SQL 改写。', 34, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2570, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (11, 'mysql-7',  'InnoDB 和 MyISAM 存储引擎的区别？',       '事务、行锁、外键、崩溃恢复等维度对比。', 36, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2707, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (12, 'mysql-8',  '什么是回表查询？',                       '非聚簇索引查询主键后再查聚簇索引的过程。', 31, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2844, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (13, 'mysql-9',  '联合索引在哪些场景下会失效？',            '跳过最左列、范围查询、函数运算等场景。', 31, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 2981, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (14, 'mysql-10', 'MySQL 是如何执行一条 SQL 的？',          '连接器、分析器、优化器、执行器全流程。', 34, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 3118, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (15, 'mysql-11', 'Buffer Pool 的作用是什么？',             '缓存数据页、减少磁盘 IO，LRU 淘汰策略。', 36, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 3255, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (16, 'mysql-12', 'redo log 和 binlog 的区别？',            '物理日志 vs 逻辑日志，作用与写入时机。', 33, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 3392, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (17, 'mysql-13', '什么是两阶段提交？',                     'redo log 与 binlog 的一致性保证流程。', 33, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 3529, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (18, 'mysql-14', '脏读、不可重复读、幻读的区别？',          '三种一致性问题与隔离级别的对应关系。', 32, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 3666, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (19, 'mysql-15', '如何解决幻读？',                         'MVCC 快照读 + 当前读的间隙锁机制。', 32, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 3803, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (20, 'mysql-16', 'MySQL 的锁有哪些类型？',                 '表锁/行锁、共享锁/排他锁、意向锁。', 35, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 3940, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (21, 'mysql-17', '什么是间隙锁？什么时候会加？',            '可重复读下对区间加锁，防止幻读。', 35, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 4077, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (22, 'mysql-18', '死锁是如何产生的？如何排查和避免？',      '加锁顺序不一致导致，show engine innodb status 排查。', 35, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 4214, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (23, 'mysql-19', '主键为什么建议用自增？',                 'B+ 树顺序插入、页分裂概率低、索引空间小。', 31, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 4351, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (24, 'mysql-20', '什么是分库分表？有哪些方案？',            '垂直/水平拆分、取模/范围分片、中间件方案。', 36, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 4488, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (25, 'mysql-21', '什么是主从复制？有哪些复制方式？',        '异步、半同步、组复制的原理与延迟问题。', 36, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 4625, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (26, 'mysql-22', '半同步复制和异步复制的区别？',            'ack 等待机制、数据可靠性 vs 性能。', 36, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 4762, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (27, 'mysql-23', '如何保证主从一致？',                     '主从延迟监控、半同步、binlog 校验。', 36, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 4899, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (28, 'mysql-24', 'COUNT(*) 和 COUNT(1) 的区别？',         'InnoDB 下两者性能一致，COUNT(字段) 忽略 NULL。', 34, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5036, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (29, 'mysql-25', '为什么 WHERE 条件里不要对索引列做函数运算？', '破坏索引有序性导致无法走索引。', 31, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5173, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (30, 'mysql-26', 'EXPLAIN 中的 type 有哪些取值？',         'const/ref/range/index/all 的含义与优化方向。', 34, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5310, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (31, 'mysql-27', '什么是索引下推？',                       'ICP 优化，存储引擎层过滤减少回表。', 31, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5447, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (32, 'mysql-28', '大表加索引的正确姿势？',                 '在线 DDL、pt-online-schema-change、低峰期执行。', 31, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5584, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (33, 'mysql-29', '什么是冷热数据分离？',                   '按访问频率拆分存储，热数据入缓存。', 36, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5721, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (34, 'mysql-30', '如何定位慢 SQL？',                       '开启慢查询日志、EXPLAIN 分析、监控工具。', 34, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5858, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (35, 'redis-1', 'Redis 有哪些数据结构？',                  'String/Hash/List/Set/ZSet 及底层编码。', 4, 'EASY', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 5995, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (36, 'redis-2', '缓存穿透、击穿、雪崩如何解决？',           '布隆过滤器、互斥锁、随机过期时间。', 4, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 6132, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (37, 'java-1', 'HashMap 的底层实现原理？',                 '数组 + 链表 + 红黑树，扩容与哈希扰动。', 5, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 6269, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (38, 'java-2', 'synchronized 与 ReentrantLock 的区别？',  '锁的实现、公平性、可中断、条件变量。', 5, 'MEDIUM', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 6406, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (39, 'system-design-1', '如何设计一个高并发短链接系统？',   '发号器、重定向、缓存、数据分片。', 6, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 6543, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
  (40, 'system-design-2', '分布式事务有哪些方案？',           '2PC、TCC、本地消息表、Seata。', 6, 'HARD', 1, '（mock 占位，完整答案由管理端录入）', '<p>（mock 占位，完整答案由管理端录入）</p>', 6680, 1, '2026-07-20 10:00:00', '2026-07-20 10:00:00');

-- 新分类 mock 题目（Java基础/Java集合/JVM/JUC/微服务/MQ/数据结构 各 2 道）
INSERT INTO `article`
  (`id`, `slug`, `title`, `summary`, `category_id`, `difficulty`, `status`, `content_md`, `content_html`, `view_count`, `created_by`, `created_at`, `updated_at`) VALUES
  (47, 'java-basic-1', 'String、StringBuilder、StringBuffer 的区别？', '不可变性、线程安全与性能的对比。', 7, 'MEDIUM', 1, '## 一、核心区别\n\n- String：不可变，每次修改都会创建新对象\n- StringBuilder：可变，线程不安全，单线程下性能最好\n- StringBuffer：可变，方法加 synchronized，线程安全，性能略低\n\n## 二、使用建议\n\n单线程拼接字符串优先用 StringBuilder；需要线程安全才用 StringBuffer。', '<h2 id="java-basic-1-1">一、核心区别</h2><ul><li>String：不可变，每次修改都会创建新对象</li><li>StringBuilder：可变，线程不安全，单线程下性能最好</li><li>StringBuffer：可变，方法加 synchronized，线程安全，性能略低</li></ul><h2 id="java-basic-1-2">二、使用建议</h2><p>单线程拼接字符串优先用 StringBuilder；需要线程安全才用 StringBuffer。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (48, 'java-basic-2', '== 和 equals 的区别？', '引用比较与内容比较，equals 的重写规则。', 7, 'EASY', 1, '## 一、区别\n\n- ==：比较基本类型的值，或引用类型的地址\n- equals：默认等价于 ==，重写后比较内容\n\n## 二、重写约定\n\n重写 equals 必须重写 hashCode，保证相等对象哈希一致。', '<h2 id="java-basic-2-1">一、区别</h2><p>== 比较基本类型的值，或引用类型的地址；equals 默认等价于 ==，重写后比较内容。</p><h2 id="java-basic-2-2">二、重写约定</h2><p>重写 equals 必须重写 hashCode，保证相等对象哈希一致。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (49, 'java-collection-1', 'ArrayList 和 LinkedList 的区别？', '底层结构、随机访问与插入删除的性能差异。', 8, 'EASY', 1, '## 一、底层结构\n\n- ArrayList：动态数组，随机访问 O(1)，插入删除可能搬移元素\n- LinkedList：双向链表，头尾插入删除 O(1)，随机访问 O(n)\n\n## 二、选择建议\n\n读多写少用 ArrayList；频繁头尾增删用 LinkedList。', '<h2 id="java-collection-1-1">一、底层结构</h2><p>ArrayList 是动态数组，随机访问 O(1)；LinkedList 是双向链表，头尾增删 O(1)，随机访问 O(n)。</p><h2 id="java-collection-1-2">二、选择建议</h2><p>读多写少用 ArrayList；频繁头尾增删用 LinkedList。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (50, 'java-collection-2', 'HashSet 的底层实现原理？', '基于 HashMap 的去重实现与注意事项。', 8, 'MEDIUM', 1, '## 一、实现原理\n\nHashSet 内部持有 HashMap，元素作为 key 存入，value 为固定占位对象。\n\n## 二、去重规则\n\n依赖元素的 hashCode 与 equals：哈希值相同且 equals 相等则视为重复，不重复插入。', '<h2 id="java-collection-2-1">一、实现原理</h2><p>HashSet 内部持有 HashMap，元素作为 key 存入，value 为固定占位对象。</p><h2 id="java-collection-2-2">二、去重规则</h2><p>依赖元素的 hashCode 与 equals：哈希值相同且 equals 相等则视为重复，不重复插入。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (51, 'jvm-1', 'JVM 内存区域有哪些？', '堆、虚拟机栈、本地方法栈、方法区、程序计数器。', 9, 'MEDIUM', 1, '## 一、内存区域\n\n- 程序计数器：当前线程执行字节码的行号\n- 虚拟机栈：栈帧存放局部变量、操作数栈等\n- 本地方法栈：服务 native 方法\n- 堆：对象实例分配的主要区域\n- 方法区：类信息、常量、静态变量\n\n## 二、OOM 排查\n\n堆内存不足优先排查对象泄漏与 GC 配置。', '<h2 id="jvm-1-1">一、内存区域</h2><p>程序计数器、虚拟机栈、本地方法栈、堆、方法区，各司其职。</p><h2 id="jvm-1-2">二、OOM 排查</h2><p>堆内存不足优先排查对象泄漏与 GC 配置。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (52, 'jvm-2', '什么是类加载机制？双亲委派模型？', '类加载五阶段与双亲委派的作用。', 9, 'HARD', 1, '## 一、类加载过程\n\n加载、验证、准备、解析、初始化五个阶段。\n\n## 二、双亲委派\n\n类加载请求先交给父加载器，父加载器找不到才由自己加载，避免核心类被重复加载和篡改。', '<h2 id="jvm-2-1">一、类加载过程</h2><p>加载、验证、准备、解析、初始化五个阶段。</p><h2 id="jvm-2-2">二、双亲委派</h2><p>类加载请求先交给父加载器，父加载器找不到才由自己加载，避免核心类被重复加载和篡改。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (53, 'juc-1', '线程池的核心参数有哪些？', 'corePoolSize、maximumPoolSize、队列与拒绝策略。', 10, 'MEDIUM', 1, '## 一、核心参数\n\n- corePoolSize：核心线程数\n- maximumPoolSize：最大线程数\n- workQueue：任务队列\n- 拒绝策略：Abort、CallerRuns、Discard 等\n\n## 二、执行流程\n\n核心线程满 -> 进队列 -> 队列满 -> 扩到最大线程 -> 仍满则触发拒绝策略。', '<h2 id="juc-1-1">一、核心参数</h2><p>corePoolSize、maximumPoolSize、workQueue 与拒绝策略。</p><h2 id="juc-1-2">二、执行流程</h2><p>核心线程满则进队列，队列满则扩到最大线程，仍满触发拒绝策略。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (54, 'juc-2', 'volatile 关键字的作用？', '保证可见性与有序性，不保证原子性。', 10, 'MEDIUM', 1, '## 一、作用\n\n- 可见性：写操作立即刷新到主内存\n- 有序性：禁止指令重排序\n\n## 二、局限\n\n不保证原子性，复合操作（如 i++）仍需加锁或使用原子类。', '<h2 id="juc-2-1">一、作用</h2><p>volatile 保证可见性与禁止指令重排序。</p><h2 id="juc-2-2">二、局限</h2><p>不保证原子性，复合操作仍需加锁或使用原子类。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (55, 'microservice-1', '什么是微服务？与单体架构的区别？', '服务拆分、独立部署、通信与治理。', 11, 'EASY', 1, '## 一、什么是微服务\n\n将单体应用按业务拆分为多个独立部署的小服务，服务间通过 HTTP/RPC 通信。\n\n## 二、与单体对比\n\n单体开发简单但难以扩展；微服务独立伸缩、故障隔离，但带来分布式复杂度。', '<h2 id="microservice-1-1">一、什么是微服务</h2><p>将单体应用按业务拆分为多个独立部署的小服务，服务间通过 HTTP/RPC 通信。</p><h2 id="microservice-1-2">二、与单体对比</h2><p>单体开发简单但难以扩展；微服务独立伸缩、故障隔离，但带来分布式复杂度。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (56, 'microservice-2', '服务注册与发现是怎么实现的？', '注册中心、心跳续约与负载均衡。', 11, 'MEDIUM', 1, '## 一、实现原理\n\n服务启动时向注册中心注册，定期心跳续约；调用方从注册中心拉取实例列表并负载均衡。\n\n## 二、常见组件\n\nNacos、Eureka、Consul 等。', '<h2 id="microservice-2-1">一、实现原理</h2><p>服务启动时向注册中心注册并心跳续约，调用方拉取实例列表做负载均衡。</p><h2 id="microservice-2-2">二、常见组件</h2><p>Nacos、Eureka、Consul 等。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (57, 'mq-1', '消息队列的作用是什么？', '异步、解耦、削峰填谷。', 12, 'EASY', 1, '## 一、核心作用\n\n- 异步：非关键路径并行处理\n- 解耦：生产与消费方不直接依赖\n- 削峰：缓冲瞬时流量\n\n## 二、代价\n\n引入一致性、顺序与可靠性问题。', '<h2 id="mq-1-1">一、核心作用</h2><p>异步、解耦、削峰填谷。</p><h2 id="mq-1-2">二、代价</h2><p>引入一致性、顺序与可靠性问题。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (58, 'mq-2', '如何保证消息不丢失？', '生产端、Broker、消费端三段的可靠性。', 12, 'HARD', 1, '## 一、生产端\n\n确认机制：发送方确认（confirm/ack）失败重发。\n\n## 二、Broker\n\n持久化到磁盘，主从同步。\n\n## 三、消费端\n\n手动确认（ack），处理成功后才提交，失败重试。', '<h2 id="mq-2-1">一、生产端</h2><p>发送方确认机制，失败重发。</p><h2 id="mq-2-2">二、Broker</h2><p>消息持久化到磁盘，主从同步。</p><h2 id="mq-2-3">三、消费端</h2><p>手动确认，处理成功后才提交，失败重试。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (59, 'data-structure-1', '数组和链表的区别？', '内存布局、查询与增删的时间复杂度。', 13, 'EASY', 1, '## 一、区别\n\n- 数组：连续内存，随机访问 O(1)，插入删除 O(n)\n- 链表：分散内存，随机访问 O(n)，已知位置增删 O(1)\n\n## 二、选择\n\n读多写少用数组，频繁增删用链表。', '<h2 id="data-structure-1-1">一、区别</h2><p>数组连续内存随机访问 O(1)；链表分散内存增删灵活。</p><h2 id="data-structure-1-2">二、选择</h2><p>读多写少用数组，频繁增删用链表。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00'),
  (60, 'data-structure-2', '二叉树有哪些遍历方式？', '前序、中序、后序与层序遍历。', 13, 'MEDIUM', 1, '## 一、深度优先\n\n- 前序：根左右\n- 中序：左根右\n- 后序：左右根\n\n## 二、广度优先\n\n层序遍历：借助队列逐层输出。', '<h2 id="data-structure-2-1">一、深度优先</h2><p>前序根左右、中序左根右、后序左右根。</p><h2 id="data-structure-2-2">二、广度优先</h2><p>层序遍历借助队列逐层输出。</p>', 0, 1, '2026-08-09 00:00:00', '2026-08-09 00:00:00');

-- ---------------------------------------------------------------------
-- 标签
-- ---------------------------------------------------------------------
INSERT INTO `tag` (`id`, `name`) VALUES
  (1, 'TCP'), (2, '传输层'), (3, 'UDP'), (4, '进程'), (5, '线程'),
  (6, '死锁'), (7, '索引'), (8, 'MVCC'), (9, '事务'), (10, '日志'),
  (11, '优化'), (12, '存储引擎'), (13, '锁'), (14, '架构'), (15, '主从'),
  (16, '数据结构'), (17, '缓存'), (18, '集合'), (19, '并发'), (20, '分布式'),
  (21, '架构设计'),
  (22, 'Java'), (23, 'String'), (24, 'JVM'), (25, '微服务'), (26, '消息队列');

-- ---------------------------------------------------------------------
-- 题目-标签关联
-- ---------------------------------------------------------------------
INSERT INTO `article_tag` (`article_id`, `tag_id`) VALUES
  (1, 1), (1, 2),
  (2, 1), (2, 3),
  (3, 4), (3, 5),
  (4, 6),
  (5, 7), (6, 8), (7, 9), (8, 7), (9, 7),
  (10, 11), (11, 12), (12, 7), (13, 7), (14, 14),
  (15, 12), (16, 10), (17, 10), (17, 9),
  (18, 9), (19, 9), (19, 13),
  (20, 13), (21, 13), (22, 13), (23, 7),
  (24, 14), (25, 15), (26, 15), (27, 15),
  (28, 11), (29, 7), (30, 11), (31, 7), (32, 11),
  (33, 14), (34, 11),
  (35, 16), (36, 17),
  (37, 18), (38, 19),
  (39, 21), (40, 20),
  (47, 22), (47, 23),
  (48, 22),
  (49, 18), (50, 18),
  (51, 24), (52, 24),
  (53, 19), (54, 19),
  (55, 25), (56, 25),
  (57, 26), (58, 26),
  (59, 16), (60, 16);

-- ---------------------------------------------------------------------
-- 管理员操作日志（示例）
-- ---------------------------------------------------------------------
INSERT INTO `admin_log` (`id`, `admin_id`, `action`, `target_type`, `target_id`, `detail`, `created_at`) VALUES
  (1, 1, 'USER_CREATE',      'USER',      3, '创建用户 zhangsan',          '2026-06-10 10:05:00'),
  (2, 1, 'USER_DISABLE',     'USER',      5, '禁用用户 wangwu',            '2026-07-20 12:10:00'),
  (3, 1, 'ARTICLE_CREATE',   'ARTICLE',  16, '新增题目：redo log 和 binlog 的区别？', '2026-07-20 10:30:00'),
  (4, 1, 'ARTICLE_UPDATE',   'ARTICLE',   5, '编辑题目：MySQL 索引为什么使用 B+ 树？', '2026-07-21 09:00:00'),
  (5, 1, 'CATEGORY_CREATE',  'CATEGORY', 31, '新增子分类：索引（MySQL 下）', '2026-07-18 15:00:00');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 常见查询示例
-- =====================================================================
-- 1) 某顶级分类及其所有子分类的题目数
-- SELECT c.id, c.name, COUNT(a.id) AS cnt
-- FROM category c
-- LEFT JOIN article a ON a.category_id = c.id AND a.status = 1
-- WHERE c.id = 3 OR c.parent_id = 3
-- GROUP BY c.id, c.name;

-- 2) 浏览量 Top 10
-- SELECT id, title, view_count FROM article ORDER BY view_count DESC LIMIT 10;
