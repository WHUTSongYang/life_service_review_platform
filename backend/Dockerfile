# 1. 使用较小的 JDK 运行环境
FROM openjdk:17-jdk-slim

# 2. 设置工作目录
WORKDIR /app

# 3. 将打包好的 Jar 复制进镜像（根据你实际生成的 jar 名字修改）
COPY target/life-service-review-platform.jar app.jar

# 4. 设置 JVM 参数，适配你的 2核4G 服务器
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"

# 5. 暴露端口（根据你的 Spring Boot 配置修改）
EXPOSE 8080

# 6. 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]