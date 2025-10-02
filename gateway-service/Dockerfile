FROM eclipse-temurin:17-jdk-jammy AS base
WORKDIR /workspace
COPY gradlew settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
COPY common common
COPY gateway-service gateway-service

# dev Build Stage
FROM base AS build-dev
RUN ./gradlew :common:jar :gateway-service:bootJar -Pprofile=dev -x test --no-daemon

# prod Build Stage
FROM base AS build-prod
RUN ./gradlew :common:jar :gateway-service:bootJar -Pprofile=prod -x test --no-daemon

# dev Runtime Stage
FROM bellsoft/liberica-runtime-container:jre-17.0.12-musl AS dev
WORKDIR /app
COPY --from=build-dev /workspace/gateway-service/build/libs/*-dev.jar app.jar
ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

# prod Runtime Stage
FROM bellsoft/liberica-runtime-container:jre-17.0.12-musl AS prod
WORKDIR /app
COPY --from=build-prod /workspace/gateway-service/build/libs/*-prod.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]