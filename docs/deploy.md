# BUDDYS Server Deployment Guide

## 서버 구조

Client -> HTTPS -> Nginx(80/443) -> Spring Boot(8080) -> RDS MySQL

## EC2 경로

- 프로젝트 경로: `/home/ubuntu/BUDDYS-SERVER`
- 환경변수 파일: `/home/ubuntu/BUDDYS-SERVER/.env`
- 실행 JAR: `/home/ubuntu/BUDDYS-SERVER/app.jar`
- Nginx 설정: `/etc/nginx/sites-available/buddys`
- systemd 서비스: `/etc/systemd/system/buddys.service`

## 배포 명령어

```bash
cd ~/BUDDYS-SERVER
git pull origin main

source .env
./gradlew clean bootJar -x test

JAR=$(ls build/libs/*.jar | grep -v plain | head -n 1)
cp "$JAR" app.jar

sudo systemctl restart buddys
sudo systemctl status buddys
```

## 로그 확인

```bash
journalctl -u buddys -f
```

## Nginx 설정 확인

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## HTTPS 인증서 발급

도메인의 DNS A 레코드가 EC2 탄력적 IP를 바라보는 상태에서 실행한다.

```bash
sudo certbot --nginx -d YOUR_DOMAIN_OR_PUNYCODE_DOMAIN
```

## 도메인 변경 시 수정할 것

- DNS A 레코드
- `/etc/nginx/sites-available/buddys`의 `server_name`
- Certbot 인증서 도메인
- EC2 `.env`의 `KAKAO_REDIRECT_URL`
- 카카오 디벨로퍼스 Redirect URI
- 프론트 API Base URL
- `CORS_ALLOWED_ORIGINS`

## 주의사항

- `.env`는 절대 커밋하지 않는다.
- `.pem` 키 파일은 절대 커밋하지 않는다.
- 외부 인바운드 포트는 22, 80, 443만 열어둔다.
- 8080은 Nginx가 내부에서만 접근한다.
```