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
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/openagentflow-backend-0.1.0-SNAPSHOT.jar /app/openagentflow-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/openagentflow-backend.jar"]
