# 部署方式一（推荐）：本地 mvn package 构建出 jar 后，复制为 backend/app.jar
FROM eclipse-temurin:8-jre

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
