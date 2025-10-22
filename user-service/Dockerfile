FROM eclipse-temurin:17-jdk-jammy AS base
LABEL authors="sang-yoon"

WORKDIR /workspace

# Gradle wrapper를 명시적으로 복사
COPY gradlew ./
COPY gradle/wrapper gradle/wrapper
COPY settings.gradle ./

# 디버깅: gradle 디렉토리 구조 확인
RUN echo "=== Checking workspace directory ===" && \
    ls -la && \
    echo "=== Checking gradle directory ===" && \
    ls -la gradle/ && \
    echo "=== Checking gradle/wrapper directory ===" && \
    ls -la gradle/wrapper/ && \
    echo "=== Checking gradle-wrapper.jar ===" && \
    file gradle/wrapper/gradle-wrapper.jar || echo "gradle-wrapper.jar not found!"

RUN chmod +x gradlew

COPY common common
COPY user-service user-service


# dev Build Stage
FROM base AS build-dev
RUN ./gradlew :common:jar user-service:bootJar -Pprofile=dev -x test --no-daemon


# prod Build Stage
FROM base AS build-prod
RUN ./gradlew :common:jar user-service:bootJar -Pprofile=prod -x test --no-daemon


# dev Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS dev
WORKDIR /app

COPY --from=build-dev /workspace/user-service/build/libs/*-dev.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


# prod Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS prod
WORKDIR /app

COPY --from=build-prod /workspace/user-service/build/libs/*-prod.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 9090

ENTRYPOINT ["java","-jar","/app/app.jar"]