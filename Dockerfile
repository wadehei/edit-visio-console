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

# 时区设为 Asia/Shanghai，与应用内的 jackson/serverTimezone 保持一致
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

COPY --from=build /app/vb-gateway/target/*.jar app.jar

# 以非 root 用户运行，降低容器逃逸风险
RUN addgroup -S app && adduser -S app -G app \
    && chown -R app:app /app
USER app

EXPOSE 8080

# 内存按容器限额比例分配，避免写死堆大小导致 OOMKilled
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom"

# 健康检查直接探测 actuator health 端点（该端点已在白名单中允许匿名访问）
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q -O - http://localhost:8080/internal/actuator/health || exit 1

# exec 形式让 java 成为 PID 1，确保收到 SIGTERM 后能优雅停机
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
