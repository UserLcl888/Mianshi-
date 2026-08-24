# 宝塔面板部署说明（0-1 知识分享平台）

## 本机环境与宝塔安装

- 系统：Ubuntu 22.04 LTS
- 宝塔：官方稳定版 `installStable_12.sh`（当前服务器已安装，安装绑定码 `5x59vxy56`）

新服务器安装宝塔（SSH 终端执行）：

```bash
url=https://download.bt.cn/install/installStable_12.sh;if [ -f /usr/bin/curl ];then curl -sSO $url;else wget -O installStable_12.sh $url;fi;bash installStable_12.sh 5x59vxy56
```

> 安装完成后会打印面板地址 / 账号 / 密码，截图保存。登录后按下面步骤安装环境。

## 目录结构

```
yun/
├── dist/                         # 前端构建产物（Nginx 站点根目录）
├── interview-backend-0.1.0.jar   # 后端可执行 jar（Java 17+）
├── interview.sql                 # 建库建表脚本（MySQL 8，utf8mb4_0900_ai_ci）
├── application-example.yml       # 后端配置模板（改好密码后随 jar 一起上传）
└── 部署上手指南.md                # 完整部署文档
```

## 服务器环境要求

- Nginx（宝塔软件商店安装）
- MySQL 8.x（SQL 用了 utf8mb4_0900_ai_ci，5.7 不支持）
- Redis 7.x
- JDK 17+（宝塔软件商店装 OpenJDK 17，或使用 Java 项目管理器）

## 部署步骤

### 1. 导入数据库

1. 宝塔 → 数据库 → 新建数据库 `interview`（utf8mb4）。
2. 导入 `interview.sql`。
3. 记住数据库用户名和密码。

### 2. 后端

1. 把 `application-example.yml` 复制为 `application.yml`，修改：
   - `spring.datasource.username / password`：填上面建的数据库账号密码
   - `spring.data.redis.password`：填宝塔 Redis 的密码（没有密码就删掉该行或留空）
   - `spring.mail.*`：填你的 QQ 邮箱和 SMTP 授权码（验证码邮件用）
2. 上传 `interview-backend-0.1.0.jar` 和 `application.yml` 到服务器同一目录（如 `/www/wwwroot/interview-backend/`）。
3. 启动（建议用宝塔「Java 项目管理器」，或命令行）：

```bash
cd /www/wwwroot/interview-backend
nohup java -Xms256m -Xmx1g -jar interview-backend-0.1.0.jar --spring.config.additional-location=./application.yml > app.log 2>&1 &
```

4. 验证：`curl http://127.0.0.1:8080/api/home/overview`（返回 JSON 即成功）。

### 3. 前端（Nginx）

1. 宝塔 → 网站 → 添加站点（域名或 IP），根目录指向 `dist` 上传后的目录（如 `/www/wwwroot/interview/dist`）。
2. 网站设置 → 配置文件，加入：

```nginx
location / {
    try_files $uri $uri/ /index.html;   # history 路由必须
}

location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location /images/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
}
```

3. 重启 Nginx，访问站点即可。

## 默认账号（上线后请立即修改密码）

- 管理员：`2090323327@qq.com / 123456`
- 普通用户：`2090323328@qq.com / 123456`
