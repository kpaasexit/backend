FROM gradle:8.14.3-jdk17 AS base
LABEL authors="sang-yoon"

WORKDIR /workspace

COPY notification-service/gradlew ./gradlew
COPY notification-service/gradle gradle
COPY settings.gradle ./
RUN chmod +x gradlew

RUN sed -i 's/timeout = 10000/timeout = 60000/g' gradle/wrapper/gradle-wrapper.properties || true

COPY common common
COPY notification-service notification-service


# dev Build Stage
FROM base AS build-dev
RUN ./gradlew :common:jar notification-service:bootJar -Pprofile=dev -x test --no-daemon -Dsun.net.client.defaultReadTimeout=600000 -Dsun.net.client.defaultConnectTimeout=600000


# prod Build Stage
FROM base AS build-prod
RUN ./gradlew :common:jar notification-service:bootJar -Pprofile=prod -x test --no-daemon -Dsun.net.client.defaultReadTimeout=600000 -Dsun.net.client.defaultConnectTimeout=600000

# dev Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS dev
WORKDIR /app

COPY --from=build-dev /workspace/notification-service/build/libs/*-dev.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


# prod Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS prod
WORKDIR /app

COPY --from=build-prod /workspace/notification-service/build/libs/*-prod.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 9090

ENTRYPOINT ["java","-jar","/app/app.jar"]