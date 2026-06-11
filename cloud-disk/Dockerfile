# 第一阶段：使用 Maven 镜像进行编译打包 (升级为 JDK 17)
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# 缓存 Maven 依赖
RUN mvn dependency:go-offline
COPY src ./src
# 编译打包项目
RUN mvn clean package -DskipTests

# 第二阶段：使用轻量级 JRE 17 运行环境
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 你的项目打包成了 war，所以这里复制 .war 文件
COPY --from=builder /build/target/*.war app.war
EXPOSE 8080
# 启动 Spring Boot
ENTRYPOINT ["java", "-jar", "app.war"]