# 生产镜像采用多阶段构建：第一阶段编译 Spring Boot，第二阶段只保留运行时 JRE 和 Jar。
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && groupadd -r openagentflow \
    && useradd -r -g openagentflow -d /app -s /usr/sbin/nologin openagentflow \
    && chown -R openagentflow:openagentflow /app \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build --chown=openagentflow:openagentflow /workspace/target/openagentflow-backend-0.1.0-SNAPSHOT.jar /app/openagentflow-backend.jar
USER openagentflow
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/openagentflow-backend.jar"]
