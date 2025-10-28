FROM gradle:8.14.3-jdk17 AS base
LABEL authors="sang-yoon"

WORKDIR /workspace

COPY magazine-service/gradlew ./gradlew
COPY gradle.properties ./
COPY magazine-service/gradle gradle
COPY settings.gradle ./

RUN chmod +x gradlew

RUN for i in 1 2 3; do \
      ./gradlew --version && break || \
      (echo "Retry $i..." && sleep 10); \
    done

COPY common common
COPY magazine-service magazine-service


# dev Build Stage
FROM base AS build-dev
RUN ./gradlew :common:jar magazine-service:bootJar -Pprofile=dev -x test --no-daemon


# prod Build Stage
FROM base AS build-prod
RUN for i in 1 2 3; do \
      ./gradlew :common:jar :gateway-service:asciidoctor :gateway-service:bootJar \
        -Pprofile=prod --no-daemon -x test -x restDocsTest && break || \
      (echo "Build retry $i..." && sleep 15); \
    done

# dev Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS dev
WORKDIR /app

COPY --from=build-dev /workspace/magazine-service/build/libs/*-dev.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


# prod Runtime Stage
FROM eclipse-temurin:17-jre-jammy AS prod
WORKDIR /app

COPY --from=build-prod /workspace/magazine-service/build/libs/*-prod.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 9090

ENTRYPOINT ["java","-jar","/app/app.jar"]
