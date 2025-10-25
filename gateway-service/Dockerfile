FROM gradle:8.14.3-jdk17 AS base
WORKDIR /workspace

COPY gateway-service/gradlew ./gradlew
COPY gateway-service/gradle gradle
COPY settings.gradle ./

RUN chmod +x gradlew
COPY common common
COPY gateway-service gateway-service

# dev Build Stage - Use pre-built docs from local build
FROM base AS build-dev
COPY gateway-service/build/generated-snippets gateway-service/build/generated-snippets
RUN ./gradlew :common:jar :gateway-service:asciidoctor :gateway-service:bootJar -Pprofile=dev --no-daemon -x test -x restDocsTest

# prod Build Stage - Use pre-built docs from local build
FROM base AS build-prod
COPY gateway-service/build/generated-snippets gateway-service/build/generated-snippets
RUN ./gradlew :common:jar :gateway-service:asciidoctor :gateway-service:bootJar -Pprofile=prod --no-daemon -x test -x restDocsTest

# dev Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS dev
WORKDIR /app
COPY --from=build-dev /workspace/gateway-service/build/libs/*-dev.jar app.jar
ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

# prod Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS prod
WORKDIR /app
COPY --from=build-prod /workspace/gateway-service/build/libs/*-prod.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]