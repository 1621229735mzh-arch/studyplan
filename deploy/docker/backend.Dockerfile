# 后端镜像：使用仓库内的 Maven Wrapper 构建，运行阶段只带 JRE。
# 构建上下文是仓库根目录（见 deploy/compose.yaml），因此路径以 backend/ 开头。

FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# 先只复制构建描述与 Wrapper，利用层缓存
COPY backend/mvnw ./
COPY backend/.mvn .mvn
COPY backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q -DskipTests dependency:go-offline

COPY backend/src ./src
RUN ./mvnw -B -q -DskipTests package


FROM eclipse-temurin:21-jre AS runtime

# 健康检查需要 HTTP 客户端；镜像里只装 curl，不引入额外服务
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home study

WORKDIR /app
COPY --from=build /build/target/study-backend-*.jar app.jar
RUN chown -R study:study /app

USER study
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai" \
    SPRING_PROFILES_ACTIVE=prod

# 只探测健康端点；管理端点不通过 Nginx 对外暴露
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
