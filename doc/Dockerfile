# 使用官方 OpenJDK 17 镜像作为基础镜像
FROM eclipse-temurin:17-jdk-jammy

# 设置工作目录
WORKDIR /app

# 拷贝构建好的 jar 包到容器中
COPY GeoFlow-0.0.1-SNAPSHOT.jar app.jar

# 开放应用使用的端口（根据你的项目配置来，默认 8080）
EXPOSE 8080

# 设置启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
