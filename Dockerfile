FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle* settings.gradle* ./

RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

RUN JAR_FILE=$(ls build/libs/*.jar | grep -v plain | head -n 1) && cp "$JAR_FILE" app.jar


FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=builder --chown=app:app /app/app.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD ["bash", "-ec", "exec 3<>/dev/tcp/127.0.0.1/8080; printf 'GET /actuator/health HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n' >&3; response=$(cat <&3); printf '%s' \"$response\" | grep -q ' 200 ' && printf '%s' \"$response\" | grep -q '\"status\":\"UP\"'"]

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
