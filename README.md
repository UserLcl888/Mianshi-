# 面试题库知识分享平台

一个前后端分离的面试题知识分享网站：提供文章浏览、分类检索、Markdown 阅读、访问权限申请、个人投稿与站内通知，并内置管理后台，方便管理员维护文章、分类、标签、用户与内容审核。

## 功能特性

### 用户端

- 邮箱 / 手机号注册登录，支持密码登录与邮箱验证码登录，可随时双向切换
- 忘记密码（邮箱验证码重置）
- 首页轮播、热门文章、每日一句、分类导航
- 文章列表 / 详情：Markdown 渲染、代码高亮、目录 TOC、上一篇 / 下一篇、难度标签、浏览量统计
- 受限内容访问申请：部分文章需提交申请，管理员审核通过后可查看
- 个人中心：资料修改、修改密码、我的申请、我的投稿
- 站内消息通知

### 管理后台（`/admin`）

- 数据看板：文章、用户、访问等统计图表
- 文章管理：Markdown 编辑、发布 / 下线、浏览量统计
- 分类管理、标签管理、用户管理
- 访问申请审核、用户投稿审核
- 站内通知发布、操作日志

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.3.2 · MyBatis-Plus 3.5.7 · Sa-Token 1.37.0 · MySQL 8 · Redis 7 · QQ 邮箱 SMTP |
| 前端 | Vue 3 · Vite 5 · TypeScript · Element Plus · Pinia · Vue Router · Axios · Marked · highlight.js · ECharts |

其他：Flexmark（服务端 Markdown 转换）、OWASP HTML Sanitizer（HTML 消毒）、BCrypt 密码加密。

## 项目结构

```text
.
├── interview-backend/          # Spring Boot 后端
│   └── src/main/java/com/interview/
│       ├── controller/         # 接口层（用户端 + admin/ 管理端）
│       ├── service/            # 业务逻辑
│       ├── mapper/             # MyBatis-Plus 数据访问
│       ├── entity/             # 数据库实体
│       ├── dto/                # 请求 / 响应结构
│       ├── config/             # Sa-Token、MyBatis-Plus 等配置
│       └── common/             # 统一返回、异常处理、常量
├── user-web/                   # Vue 3 前端（用户端 + 管理后台同一 SPA）
│   └── src/
│       ├── views/              # 页面
│       ├── components/         # 公共组件
│       ├── api/                # 接口封装
│       ├── stores/             # Pinia 状态
│       ├── router/             # 路由与登录 / 权限守卫
│       └── styles/             # 全局主题（暗色 + 橙金强调色）
├── interview.sql               # 数据库初始化脚本（建库、建表、种子数据）
└── 部署上手指南.md              # 从 0 到 1 的完整部署文档
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.x（SQL 使用了 `utf8mb4_0900_ai_ci` 排序规则，不支持 MySQL 5.7）
- Redis 6+（生产环境需设置 `requirepass`）
- Node.js 18+（仅前端构建需要）

### 1. 初始化数据库

```bash
mysql -uroot -p < interview.sql
```

脚本会自动创建 `interview` 库、建表并写入种子数据（含管理员 / 普通用户测试账号）。

### 2. 启动后端

```bash
cd interview-backend

# 复制配置模板为本地配置，并填入 MySQL / Redis / 邮箱信息
copy src/main/resources/application-example.yml src/main/resources/application.yml

mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd user-web
npm install
npm run dev
```

访问 `http://localhost:5173`，开发环境已配置代理：`/api`、`/images` 转发到 `http://localhost:8080`。

## 构建产物

```bash
# 后端：可执行 jar（内含 Tomcat）
cd interview-backend && mvn clean package -DskipTests
# 产物：interview-backend/target/interview-backend-0.1.0.jar

# 前端：静态文件（dist/ 同时包含用户端与管理后台）
cd user-web && npm run build
# 产物：user-web/dist/
```

## 配置说明

- `interview-backend/src/main/resources/application.yml`：本地开发配置（不入 git，含真实开发密码）
- `application-example.yml`：配置模板（全部为占位符，不会打进 jar）
- 生产环境：在服务器上单独创建 `application-prod.yml`，用 `--spring.profiles.active=prod` 启动，配置不入库、不随代码分发

关键配置项：MySQL 连接、Redis 连接与密码、QQ 邮箱 SMTP（用于发送验证码）、文件上传大小限制、验证码策略（有效期 / 重试次数 / 每日上限）。

## 测试账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | 2090323327@qq.com | 123456 |
| 普通用户 | 2090323328@qq.com | 123456 |

> 仅用于前期本地调试，正式上线前务必修改密码并清理多余种子数据。

## 部署

完整部署流程（服务器选购、FinalShell 连接、宝塔面板、MySQL / Redis / JDK 安装、后端 systemd、前端 Nginx、ICP 备案、备份与验收）见：

- [部署上手指南.md](部署上手指南.md)

当前部署环境：雨云深圳电信 2C4G / Ubuntu 22.04 / 宝塔面板 / Nginx + systemd。

## 相关文档

- [后端设计文档](后端设计文档.md)
- [用户端设计文档](用户端设计文档.md)
- [管理后台设计文档](管理后台设计文档.md)
- [后端交接文档](后端交接文档.md)

## 后续规划

- 每日推荐（类似 Geo SEO 的运营化推荐）
- 独立面试模块（模拟面试、真题练习）
- 图片 / 附件接入 MinIO 对象存储（当前存服务器本地）
- 按业务拆分微服务

## License

本项目暂未指定开源协议；如需商用或二次分发，请联系作者。
