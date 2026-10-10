# 部署说明

整套服务用 Docker Compose 部署在一台服务器上，服务器只需要装 Docker。配置文件在仓库根目录：`docker-compose.yml`、`.env.example`。

## 架构

```
浏览器 ──HTTPS──> Cloudflare ──HTTPS（Origin 证书）──> 服务器 443 端口
                                                        │
          ┌──────────────── Docker 内部网络 ─────────────┼──────────────────────┐
          │                                             ▼                      │
          │  web（Caddy）：提供前端页面；/api/* 和 /websocket/* 转发给 backend   │
          │                         │                                          │
          │                         ▼                                          │
          │  backend :8080 ──> matchingsystem :8081（匹配）                     │
          │     │      ──> botrunningsystem :8082 ──> sandbox :5050（go-judge） │
          │     ▼                                                              │
          │  postgres :5432（数据存在数据卷 pgdata 里）                          │
          └────────────────────────────────────────────────────────────────────┘
```

- 只有 web 对外开放端口，其他服务都只在 Docker 内部网络里。
- 服务之间调用内部接口时，请求头里带上共享密钥 `X-Internal-Token`（`.env` 里的 `INTERNAL_TOKEN`）。另外 Caddy 会直接拒绝外部对 `/api/pk/*` 的访问。
- go-judge 没有身份验证，**绝不能**给 sandbox 加端口映射。

## 第一次部署

### 1. 开放 443 端口

经过 Cloudflare 代理后，服务器只需要开放 443，HTTP 跳转 HTTPS 由 Cloudflare 完成。

1. Oracle 网页后台：实例所在子网的 **Security List** 里添加入站规则，来源 `0.0.0.0/0`，TCP 目标端口 `443`。
2. 服务器系统的防火墙（Oracle 的 Ubuntu 镜像默认会拦截大部分端口）：
   ```bash
   sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
   sudo netfilter-persistent save   # 保存规则，重启后仍然有效
   ```

其他端口（5432、5050、8080–8082）都不要开放。

### 2. 准备证书

Cloudflare Origin 证书放在服务器上，不要提交到 git：

```bash
mkdir -p ~/kob/certs
nano ~/kob/certs/origin.pem    # 粘贴证书
nano ~/kob/certs/origin.key    # 粘贴私钥
chmod 600 ~/kob/certs/origin.key
```

Cloudflare 后台的 SSL/TLS 模式要设成 **Full (strict)**。

### 3. 拉取代码

```bash
cd ~
git clone git@github.com:Wildlife7609/KingsOfBots.git
cd KingsOfBots
```

如果仓库是私有的，服务器需要有权限拉取：在服务器上 `ssh-keygen -t ed25519` 生成密钥，把 `~/.ssh/id_ed25519.pub` 添加到 GitHub 仓库的 **Settings → Deploy keys**（只读即可）。

### 4. 填写 .env

```bash
cp .env.example .env
nano .env
```

三个密钥都用下面的命令现场生成，**不要沿用本地开发用的**（本地用过的 JWT 密钥曾经被提交到 git）：

```bash
openssl rand -hex 24      # DB_PASSWORD
openssl rand -base64 32   # JWT_SECRET（必须是 Base64 格式）
openssl rand -hex 24      # INTERNAL_TOKEN
```

线上要改的几项：

```bash
SITE_ADDRESS=byteduel.dev
TLS="tls /certs/origin.pem /certs/origin.key"
CERTS_DIR=/home/ubuntu/kob/certs
```

`HTTP_PORT` 和 `HTTPS_PORT` 保持 80 / 443。

### 5. 构建并启动

```bash
docker compose up -d --build
```

第一次构建要下载 Maven、npm 依赖和编译器，大约 5–15 分钟。之后再构建会用缓存，快很多。

### 6. 检查

```bash
docker compose ps                  # 所有服务都是 Up，postgres 是 healthy
docker compose logs -f backend     # 看到 Started BackendApplication 说明启动成功，Ctrl+C 退出
curl -sk https://localhost --resolve byteduel.dev:443:127.0.0.1 -o /dev/null -w "%{http_code}\n"   # 200
```

然后用浏览器打开域名，注册账号、创建 Bot、打一局。

## 日常操作

| 要做什么 | 命令 |
|---|---|
| 更新代码并重新部署 | `git pull && docker compose up -d --build` |
| 看某个服务的日志 | `docker compose logs -f backend`（服务名见 `docker-compose.yml`） |
| 重启某个服务 | `docker compose restart backend` |
| 停止所有服务（数据保留） | `docker compose down` |
| 进入数据库 | `docker compose exec postgres psql -U kob -d kob` |
| 看各容器占用的内存和 CPU | `docker stats` |

**不要执行 `docker compose down -v`**：`-v` 会删除数据卷，数据库里的所有数据都会丢失。

## 备份与恢复

数据库备份：

```bash
mkdir -p ~/backups
docker compose exec -T postgres pg_dump -U kob -d kob | gzip > ~/backups/kob-$(date +%F).sql.gz
```

每天凌晨 3 点自动备份，并只保留最近 14 天（`crontab -e` 添加一行，路径按实际情况改）：

```
0 3 * * * cd /home/ubuntu/KingsOfBots && docker compose exec -T postgres pg_dump -U kob -d kob | gzip > /home/ubuntu/backups/kob-$(date +\%F).sql.gz && find /home/ubuntu/backups -name 'kob-*.sql.gz' -mtime +14 -delete
```

备份文件要定期拷贝到服务器以外的地方（比如自己电脑上），服务器被回收时才能恢复：

```bash
scp ubuntu@服务器IP:~/backups/kob-2026-10-10.sql.gz .
```

恢复到一个新部署的空数据库：

```bash
gunzip -c kob-2026-10-10.sql.gz | docker compose exec -T postgres psql -U kob -d kob
```

新部署时 `schema.sql` 已经自动建好了表，恢复前先清空它们：`docker compose exec postgres psql -U kob -d kob -c "DROP TABLE users, bots, records;"`。

## 常见问题

| 现象 | 原因和处理 |
|---|---|
| 浏览器显示 Cloudflare 的 521 / 522 | Cloudflare 连不上服务器：检查 443 端口是否开放（第 1 步），`docker compose ps` 看 web 是否在运行 |
| 浏览器显示 Cloudflare 的 526 | 证书校验失败：检查证书文件路径和 `.env` 里的 `TLS`，以及 Cloudflare 的 SSL 模式是不是 Full (strict) |
| 创建 Bot 时提示“Bot 编译服务暂不可用” | `docker compose logs sandbox` 看沙箱是否正常；它必须有 `privileged` 和 `cgroup: host` |
| backend 日志里数据库认证失败 | 数据库第一次初始化之后又改了 `DB_PASSWORD`。密码只在第一次初始化时生效，要么改回原来的，要么进数据库执行 `ALTER USER kob PASSWORD '新密码';` |
| 对局时 Bot 一直不动、超时判负 | `docker compose logs botrunningsystem` 看运行报错；`.env` 里的 `INTERNAL_TOKEN` 三个服务共用，改了要 `docker compose up -d` 让所有服务重新读取 |
