FROM gradle:jdk24 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon || true

COPY .env .env
COPY src src

RUN gradle build --no-daemon -x test

FROM eclipse-temurin:24-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/.env .env

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
