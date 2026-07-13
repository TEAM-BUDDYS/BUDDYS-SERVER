# BUDDYS Server Deployment Guide

## Server Structure

Client -> HTTPS -> Nginx(80/443) -> Spring Boot Blue/Green(127.0.0.1:8081 or 127.0.0.1:8082) -> RDS MySQL

## EC2 Paths

- Docker directory: `/home/ubuntu/BUDDYS-SERVER/docker`
- Compose file: `/home/ubuntu/BUDDYS-SERVER/docker/docker-compose.yml`
- Environment file: `/home/ubuntu/BUDDYS-SERVER/docker/.env`
- Deploy script: `/home/ubuntu/BUDDYS-SERVER/docker/deploy-blue-green.sh`
- Nginx site config: `/etc/nginx/sites-available/buddys`
- Nginx backend snippet: `/etc/nginx/snippets/buddys-backend.conf`

## First-Time EC2 Setup

Start the first Blue container on port 8081 while the old 8080 container remains running.

```bash
cd /home/ubuntu/BUDDYS-SERVER/docker
sudo env DOCKER_IMAGE=YOUR_DOCKER_IMAGE APP_PORT=8081 \
  docker compose --env-file .env -p buddys-blue -f docker-compose.yml pull app
sudo env DOCKER_IMAGE=YOUR_DOCKER_IMAGE APP_PORT=8081 \
  docker compose --env-file .env -p buddys-blue -f docker-compose.yml up -d app
curl -sS http://127.0.0.1:8081/actuator/health
```

Create the backend snippet only after Blue is healthy.

```bash
sudo mkdir -p /etc/nginx/snippets
printf 'proxy_pass http://127.0.0.1:8081;\n' | sudo tee /etc/nginx/snippets/buddys-backend.conf
```

In `/etc/nginx/sites-available/buddys`, replace the old backend line:

```nginx
proxy_pass http://127.0.0.1:8080;
```

with:

```nginx
include /etc/nginx/snippets/buddys-backend.conf;
```

Then validate and reload Nginx.

```bash
sudo nginx -t
sudo systemctl reload nginx
```

The old 8080 container must remain running until 8081 is healthy and Nginx has switched successfully.

## Automatic Deployment

On `develop` push, GitHub Actions:

1. Builds the Spring Boot app.
2. Builds and pushes Docker images tagged with `latest` and `${{ github.sha }}`.
3. Copies `.env`, `docker-compose.yml`, and `deploy-blue-green.sh` to EC2.
4. Runs `deploy-blue-green.sh DOCKER_USERNAME/buddys-server:${{ github.sha }}`.

The deploy script starts the inactive color, checks `/actuator/health`, switches Nginx only after success, and keeps the previous color running for rollback.

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
- Keep inbound ports limited to 22, 80, and 443 unless there is a separate operational reason.
- Blue and Green run at the same time during deployment, so check EC2 memory and disk usage before enabling this flow.
- Flyway migrations must remain backward compatible with the previous running version until rollback is no longer needed.
- This single-EC2 Blue-Green setup reduces deploy downtime, but it does not protect against EC2 instance failure.
