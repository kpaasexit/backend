FROM eclipse-temurin:17-jdk-jammy AS base
LABEL authors="sang-yoon"

WORKDIR /workspace

RUN echo "=== Checking if gradle-wrapper.jar exists in build context ==="

COPY gradlew ./
COPY gradle/wrapper gradle/wrapper
COPY settings.gradle ./

# 복사 직후 확인
RUN echo "=== After COPY - checking gradle/wrapper ===" && \
    ls -laR gradle/ && \
    echo "=== Attempting to access gradle-wrapper.jar ===" && \
    if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then \
        echo "✅ gradle-wrapper.jar EXISTS!"; \
        ls -lh gradle/wrapper/gradle-wrapper.jar; \
    else \
        echo "❌ gradle-wrapper.jar NOT FOUND!"; \
    fi

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