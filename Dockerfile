FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV SERVER_PORT=8080
ENV JAVA_OPTS=""

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /app/target/task-manager-0.0.1-SNAPSHOT.jar app.jar

RUN chown spring:spring /app/app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
