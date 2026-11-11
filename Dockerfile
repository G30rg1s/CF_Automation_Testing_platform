
# ========================================
# STAGE 1 - MAVEN BUILD
# ========================================

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B clean package -DskipTests


# ========================================
# STAGE 2 - JAVA 21 RUNTIME
# ========================================

FROM eclipse-temurin:21-jre

WORKDIR /app

ARG APP_VERSION=dev

ENV APP_VERSION=${APP_VERSION}

# Enable saved SQL queries initialization.
ENV SAVED_QUERIES_SCHEMA_INIT_ENABLED=true

# ========================================
# APPLICATION DIRECTORIES
# ========================================

RUN mkdir -p /app/uploads /app/data/license /app/logs \
    && chown -R 10001:10001 /app

# ========================================
# APPLICATION JAR
# ========================================

COPY --from=build \
    --chown=10001:10001 \
    /app/target/*.jar \
    /app/app.jar

# ========================================
# NON-ROOT USER
# ========================================

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
