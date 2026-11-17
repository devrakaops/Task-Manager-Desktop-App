FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/task-manager-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


-----------------------------------------------------------

# Stage 1: Build the app using Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the app using lightweight JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/task-manager-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
