FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle* settings.gradle* ./

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

RUN JAR_FILE=$(ls build/libs/*.jar | grep -v plain | head -n 1) && cp "$JAR_FILE" app.jar


FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]