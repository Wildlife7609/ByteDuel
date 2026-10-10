# 三个 Spring Boot 服务共用一个 Dockerfile：先一起打包，再分别生成三个运行镜像
# docker-compose.yml 里用 target 指定要哪一个：backend / matchingsystem / botrunningsystem

# 构建阶段固定用构建机器自己的架构，不走模拟器（打出来的 jar 不分 CPU 架构）
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
# Maven 依赖缓存在 BuildKit 里，重新构建时不用再下载
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# 运行阶段只需要 JRE；内存上限由 docker-compose.yml 里的 JAVA_TOOL_OPTIONS 设置
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS backend
COPY --from=build /src/backend/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

FROM runtime AS matchingsystem
COPY --from=build /src/matchingsystem/target/matchingsystem-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081

FROM runtime AS botrunningsystem
COPY --from=build /src/botrunningsystem/target/botrunningsystem-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
