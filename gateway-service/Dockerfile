FROM gradle:8.14.3-jdk17 AS base
WORKDIR /workspace

COPY gradlew ./gradlew
COPY gradle.properties ./
COPY settings.gradle ./
COPY gradle/wrapper gradle/wrapper

RUN chmod +x gradlew

RUN for i in 1 2 3; do \
      ./gradlew --version && break || \
      (echo "Retry $i..." && sleep 10); \
    done

COPY common common
COPY gateway-service gateway-service

# dev Build Stage - Use pre-built docs from local build
FROM base AS build-dev
COPY gateway-service/build/generated-snippets gateway-service/build/generated-snippets
RUN ./gradlew :common:jar :gateway-service:asciidoctor :gateway-service:bootJar -Pprofile=dev --no-daemon -x test -x restDocsTest

# prod Build Stage - Use pre-built docs from local build
FROM base AS build-prod
COPY gateway-service/build/generated-snippets gateway-service/build/generated-snippets
RUN for i in 1 2 3; do \
      ./gradlew :common:jar :gateway-service:asciidoctor :gateway-service:bootJar \
        -Pprofile=prod --no-daemon -x test -x restDocsTest && break || \
      (echo "Build retry $i..." && sleep 15); \
    done
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