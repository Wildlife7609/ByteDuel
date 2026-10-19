# Deployment

The whole stack runs on a single server with Docker Compose; the server only needs Docker installed. The config files are in the repository root: `docker-compose.yml` and `.env.example`.

## Architecture

```
Browser ──HTTPS──> Cloudflare ──HTTPS (Origin certificate)──> server port 443
                                                               │
          ┌───────────────── Docker internal network ──────────┼───────────────────┐
          │                                                    ▼                   │
          │  web (Caddy): serves the frontend; forwards /api/* and /websocket/*    │
          │               to backend                                               │
          │                         │                                              │
          │                         ▼                                              │
          │  backend :8080 ──> matchingsystem :8081 (matchmaking)                  │
          │     │          ──> botrunningsystem :8082 ──> sandbox :5050 (go-judge) │
          │     ▼                                                                  │
          │  postgres :5432 (data in the pgdata volume)                            │
          └────────────────────────────────────────────────────────────────────────┘
```

- Only web publishes ports. Every other service is reachable only on the Docker internal network.
- Internal calls between services carry the shared secret in the `X-Internal-Token` header (`INTERNAL_TOKEN` in `.env`). Caddy also rejects any outside request to `/api/pk/*`.
- go-judge has no authentication. **Never** add a port mapping to sandbox.

## First deployment

### 1. Open port 443

Behind the Cloudflare proxy, the server only needs port 443. Cloudflare handles the HTTP-to-HTTPS redirect.

1. In the Oracle Cloud console, add an ingress rule to the **Security List** of the instance's subnet: source `0.0.0.0/0`, TCP destination port `443`.
2. The server's own firewall (Oracle's Ubuntu image blocks most ports by default):
   ```bash
   sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
   sudo netfilter-persistent save   # keep the rule across reboots
   ```

Do not open any other port (5432, 5050, 8080–8082).

### 2. Install the certificate

Put the Cloudflare Origin certificate on the server. Never commit it to git:

```bash
mkdir -p ~/kob/certs
nano ~/kob/certs/origin.pem    # paste the certificate
nano ~/kob/certs/origin.key    # paste the private key
chmod 600 ~/kob/certs/origin.key
```

Set the SSL/TLS mode in the Cloudflare dashboard to **Full (strict)**.

### 3. Clone the code

```bash
cd ~
git clone git@github.com:Wildlife7609/ByteDuel.git
cd ByteDuel
```

**Do not rename this directory** after deploying. Docker Compose uses the directory name as the project name, and the volume names include it (for example `byteduel_pgdata`). If you rename the directory and start again, Compose creates a new, empty database, and the old data stays in the old volume. The paths below assume `/home/ubuntu/ByteDuel`; adjust them if your directory is different (for example in the backup cron job).

If the repository is private, the server needs permission to pull it: run `ssh-keygen -t ed25519` on the server and add `~/.ssh/id_ed25519.pub` under the repository's **Settings → Deploy keys** (read-only is enough).

### 4. Fill in .env

```bash
cp .env.example .env
nano .env
```

Generate all three secrets on the spot with the commands below. **Do not reuse the ones from local development** (a JWT secret used locally was once committed to git):

```bash
openssl rand -hex 24      # DB_PASSWORD
openssl rand -base64 32   # JWT_SECRET (must be Base64)
openssl rand -hex 24      # INTERNAL_TOKEN
```

Values to change for production:

```bash
SITE_ADDRESS=byteduel.dev
TLS="tls /certs/origin.pem /certs/origin.key"
CERTS_DIR=/home/ubuntu/kob/certs
```

Keep `HTTP_PORT` and `HTTPS_PORT` at 80 / 443.

### 5. Build and start

```bash
docker compose up -d --build
```

The first build downloads the Maven and npm dependencies and the compilers, and takes about 5–15 minutes. Later builds use the cache and are much faster.

### 6. Check

```bash
docker compose ps                  # every service is Up, and postgres is healthy
docker compose logs -f backend     # "Started BackendApplication" means it is up; Ctrl+C to exit
curl -sk https://localhost --resolve byteduel.dev:443:127.0.0.1 -o /dev/null -w "%{http_code}\n"   # 200
```

Then open the domain in a browser, sign up, create a bot and play a game.

## Day-to-day operations

| Task | Command |
|---|---|
| Pull new code and redeploy | `git pull && docker compose up -d --build` |
| View a service's logs | `docker compose logs -f backend` (service names are in `docker-compose.yml`) |
| Restart a service | `docker compose restart backend` |
| Stop everything (data is kept) | `docker compose down` |
| Open a database shell | `docker compose exec postgres psql -U kob -d kob` |
| Memory and CPU per container | `docker stats` |

**Never run `docker compose down -v`**: `-v` deletes the volumes, and every row in the database is lost.

## Backup and restore

Back up the database:

```bash
mkdir -p ~/backups
docker compose exec -T postgres pg_dump -U kob -d kob | gzip > ~/backups/kob-$(date +%F).sql.gz
```

To back up automatically at 3 a.m. every day and keep only the last 14 days, add this line with `crontab -e` (adjust the paths to your setup):

```
0 3 * * * cd /home/ubuntu/ByteDuel && docker compose exec -T postgres pg_dump -U kob -d kob | gzip > /home/ubuntu/backups/kob-$(date +\%F).sql.gz && find /home/ubuntu/backups -name 'kob-*.sql.gz' -mtime +14 -delete
```

Copy the backups off the server regularly (for example to your own computer), so you can still restore if the server is reclaimed:

```bash
scp ubuntu@<server-ip>:~/backups/kob-2026-10-10.sql.gz .
```

Restore into the empty database of a fresh deployment:

```bash
gunzip -c kob-2026-10-10.sql.gz | docker compose exec -T postgres psql -U kob -d kob
```

A fresh deployment has already created the tables from `schema.sql`, so drop them before restoring: `docker compose exec postgres psql -U kob -d kob -c "DROP TABLE users, bots, records;"`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Browser shows Cloudflare error 521 / 522 | Cloudflare cannot reach the server. Check that port 443 is open (step 1) and that web is running (`docker compose ps`). |
| Browser shows Cloudflare error 526 | Certificate validation failed. Check the certificate paths, `TLS` in `.env`, and that Cloudflare's SSL mode is Full (strict). |
| Creating a bot says "The bot compiler is unavailable" | Check the sandbox with `docker compose logs sandbox`. It must have `privileged` and `cgroup: host`. |
| Database authentication fails in the backend logs | `DB_PASSWORD` was changed after the database was first initialized. The password only takes effect on first initialization: either change it back, or run `ALTER USER kob PASSWORD '<new password>';` in the database. |
| Bots never move in a game and lose on timeout | Check `docker compose logs botrunningsystem` for errors. `INTERNAL_TOKEN` in `.env` is shared by all three services; after changing it, run `docker compose up -d` so every service picks it up. |
