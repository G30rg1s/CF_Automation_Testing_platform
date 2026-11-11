
# ========================================
# BUILD STAGE - PARSER SERVICE
# ========================================

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B -DskipTests clean package


# ========================================
# RUNTIME STAGE - JAVA 21
# ========================================

FROM eclipse-temurin:21-jre

WORKDIR /app

# ========================================
# UPLOADS AND LOGS DIRECTORIES
# ========================================

RUN mkdir -p /app/uploads /app/logs \
    && chown -R 10001:10001 /app

# ========================================
# PARSER APPLICATION
# ========================================

COPY --from=build \
    --chown=10001:10001 \
    /app/target/*.jar \
    /app/parser-service.jar

# ========================================
# NON-ROOT USER
# ========================================

USER 10001:10001

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/parser-service.jar"]
