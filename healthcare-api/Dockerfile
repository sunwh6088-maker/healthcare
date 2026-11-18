FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# 非 root 运行，并准备头像等上传目录
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/data/uploads && chown -R app:app /app
COPY --from=build /app/target/healthcare-api-0.0.1-SNAPSHOT.jar app.jar
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
