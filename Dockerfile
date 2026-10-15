# ============================================================
# Visual Builder Backend — Dockerfile
# ============================================================

# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY vb-common/pom.xml vb-common/
COPY vb-user/pom.xml vb-user/
COPY vb-project/pom.xml vb-project/
COPY vb-page/pom.xml vb-page/
COPY vb-component/pom.xml vb-component/
COPY vb-template/pom.xml vb-template/
COPY vb-resource/pom.xml vb-resource/
COPY vb-publish/pom.xml vb-publish/
COPY vb-system/pom.xml vb-system/
COPY vb-gateway/pom.xml vb-gateway/
# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B
# Copy source and build
COPY . .
RUN mvn package -DskipTests -B

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/vb-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
