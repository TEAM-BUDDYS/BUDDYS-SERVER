# BUDDYS Server Deployment Guide

## Server Structure

Client -> HTTPS -> Nginx(80/443) -> Spring Boot Blue/Green(127.0.0.1:8081 or 127.0.0.1:8082) -> RDS MySQL / ElastiCache Valkey

## EC2 Paths

- Docker directory: `/home/ubuntu/BUDDYS-SERVER/docker`
- Compose file: `/home/ubuntu/BUDDYS-SERVER/docker/docker-compose.yml`
- Environment file: `/home/ubuntu/BUDDYS-SERVER/docker/.env`
- Deploy script: `/home/ubuntu/BUDDYS-SERVER/docker/deploy-blue-green.sh`
- Nginx site config: `/etc/nginx/sites-available/buddys`
- Nginx backend snippet: `/etc/nginx/snippets/buddys-backend.conf`

## First-Time EC2 Setup

Set `REDIS_MODE=cluster`, `REDIS_HOST` to the ElastiCache Serverless endpoint, and
`REDIS_SSL_ENABLED=true` in `/home/ubuntu/BUDDYS-SERVER/docker/.env`. If RBAC is
enabled, also set `REDIS_USERNAME` and `REDIS_PASSWORD`. The ElastiCache security
group must allow ports 6379 and 6380 from the EC2 security group.

For local development, start the optional Valkey container and use the default
standalone connection (`localhost:6379`, TLS disabled).

```bash
docker compose -f docker/docker-compose.yml --profile local up -d valkey
```

Create the shared `buddys-monitoring` Docker network first. Prometheus and the Blue/Green `app` containers run as separate Compose projects (separate default networks), so this external network is what lets Prometheus reach `app-8081`/`app-8082` by name instead of depending on host-published ports. `deploy-blue-green.sh` also creates it automatically if missing, but creating it upfront avoids a first-run ordering issue with whichever stack (monitoring or app) starts first.

```bash
sudo docker network create buddys-monitoring
```

Create the backend snippet with the current legacy 8080 backend first. This lets the deploy script detect the initial legacy state and move traffic to Blue on 8081 only after the new container is healthy.

```bash
sudo mkdir -p /etc/nginx/snippets
printf 'proxy_pass http://127.0.0.1:8080;\n' | sudo tee /etc/nginx/snippets/buddys-backend.conf
```

In `/etc/nginx/sites-available/buddys`, replace the old backend line:

```nginx
proxy_pass http://127.0.0.1:8080;
```

with:

```nginx
include /etc/nginx/snippets/buddys-backend.conf;
```

Validate and reload Nginx while it still points to the old 8080 container.

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Run the deploy script with the Docker image to start Blue on 8081, check `/actuator/health`, and switch Nginx to 8081 automatically.

```bash
cd /home/ubuntu/BUDDYS-SERVER/docker
chmod +x ./deploy-blue-green.sh
./deploy-blue-green.sh YOUR_DOCKER_IMAGE
```

Verify that Nginx now points to 8081 and Blue is healthy.

```bash
cat /etc/nginx/snippets/buddys-backend.conf
curl -sS http://127.0.0.1:8081/actuator/health
```

The old 8080 container must remain running until the script switches Nginx to 8081 successfully and production traffic is healthy. After that, stop and remove the legacy 8080 container so it does not keep consuming memory.

```bash
sudo docker ps --filter name=buddys-server-app
sudo docker stop buddys-server-app
sudo docker rm buddys-server-app
```

## Automatic Deployment

On `develop` push, GitHub Actions:

1. Builds the Spring Boot app.
2. Builds and pushes Docker images tagged with `latest` and `${{ github.sha }}`.
3. Copies `.env`, `docker-compose.yml`, and `deploy-blue-green.sh` to EC2.
4. Runs `deploy-blue-green.sh DOCKER_USERNAME/buddys-server:${{ github.sha }}`.

The deploy script starts the inactive color, checks `/actuator/health`, switches Nginx only after success, and keeps the previous color running for rollback. If the current backend is the legacy 8080 container, the first Blue-Green deployment targets 8081 and leaves the 8080 container for manual cleanup after traffic is verified.

## Manual Deploy

```bash
cd /home/ubuntu/BUDDYS-SERVER/docker
chmod +x ./deploy-blue-green.sh
./deploy-blue-green.sh DOCKER_USERNAME/buddys-server:IMAGE_TAG
```

## Manual Rollback

Check the current backend and verify the rollback target is healthy before switching.

```bash
cat /etc/nginx/snippets/buddys-backend.conf
curl -sS http://127.0.0.1:8081/actuator/health
curl -sS http://127.0.0.1:8082/actuator/health
```

If 8081 returns `"status":"UP"`, switch to 8081.

```bash
printf 'proxy_pass http://127.0.0.1:8081;\n' | sudo tee /etc/nginx/snippets/buddys-backend.conf
sudo nginx -t
sudo systemctl reload nginx
```

If 8082 returns `"status":"UP"`, switch to 8082.

```bash
printf 'proxy_pass http://127.0.0.1:8082;\n' | sudo tee /etc/nginx/snippets/buddys-backend.conf
sudo nginx -t
sudo systemctl reload nginx
```

## Status Checks

```bash
sudo docker ps
sudo docker compose -p buddys-blue -f /home/ubuntu/BUDDYS-SERVER/docker/docker-compose.yml ps
sudo docker compose -p buddys-green -f /home/ubuntu/BUDDYS-SERVER/docker/docker-compose.yml ps
curl -sS http://127.0.0.1:8081/actuator/health
curl -sS http://127.0.0.1:8082/actuator/health
cat /etc/nginx/snippets/buddys-backend.conf
sudo nginx -t
```

## Operational Notes

- Do not open 8081 or 8082 in the security group. They are bound to `127.0.0.1` and should only be reachable through Nginx.
- Prometheus scrapes `app` over the shared `buddys-monitoring` Docker network (`app-8081`/`app-8082` aliases on container port 8080), not the host-published 127.0.0.1 ports. If a color's container isn't running or hasn't joined `buddys-monitoring`, its Prometheus target reports DOWN — check `docker network inspect buddys-monitoring` and `docker compose -p buddys-blue|buddys-green ps` when investigating.
- Keep inbound ports limited to 22, 80, and 443 unless there is a separate operational reason.
- Blue and Green run at the same time during deployment, so check EC2 memory and disk usage before enabling this flow.
- Flyway migrations must remain backward compatible with the previous running version until rollback is no longer needed.
- This single-EC2 Blue-Green setup reduces deploy downtime, but it does not protect against EC2 instance failure.
