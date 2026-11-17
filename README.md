# Task Manager

Task Manager is a production-style Spring Boot application that combines:

- Server-rendered MVC pages with Thymeleaf
- RESTful CRUD APIs
- Layered architecture: controller -> service -> repository
- H2 by default, with MySQL-ready configuration through environment variables
- Validation, global exception handling, logging, Docker support, sample seed data, pagination, and filtering

## Tech Stack

- Java 17
- Spring Boot 3.5.13
- Spring MVC + Thymeleaf
- Spring Data JPA
- H2 Database
- MySQL Connector/J
- Lombok
- Maven Wrapper
- Spring Boot Actuator
- JUnit 5 + Mockito

## Project Structure

```text
task-manager
|-- .mvn/
|-- src/
|   |-- main/
|   |   |-- java/com/example/taskmanager/
|   |   |   |-- config/
|   |   |   |-- controller/
|   |   |   |-- dto/
|   |   |   |-- entity/
|   |   |   |-- exception/
|   |   |   |-- repository/
|   |   |   `-- service/
|   |   `-- resources/
|   |       |-- static/css/styles.css
|   |       |-- templates/task-form.html
|   |       |-- templates/tasks.html
|   |       `-- application.properties
|   `-- test/java/com/example/taskmanager/
|       |-- service/TaskServiceImplTest.java
|       `-- TaskManagerApplicationTests.java
|-- .dockerignore
|-- Dockerfile
|-- mvnw
|-- mvnw.cmd
|-- pom.xml
`-- README.md
```

## Features

- Create, read, update, and delete tasks
- Search tasks by title or description
- Filter tasks by status
- Paginated task listing
- MVC web UI for task management
- REST API for all task operations
- Global JSON exception handling for API errors
- Seed data on startup for quick testing
- Actuator endpoints for operational visibility

## Running Locally

### Prerequisites

- Java 17 or newer installed
- Internet access on first Maven wrapper run so dependencies can be downloaded

### Start the application

```powershell
cd .\task-manager
.\mvnw.cmd spring-boot:run
```

### Run tests

```powershell
cd .\task-manager
.\mvnw.cmd test
```

### Build a runnable jar

```powershell
cd .\task-manager
.\mvnw.cmd clean package
```

## Access the Application

- Web UI: `http://localhost:8080/tasks`
- REST API: `http://localhost:8080/api/tasks`
- H2 Console: `http://localhost:8080/h2-console`
- Actuator Health: `http://localhost:8080/actuator/health`

H2 defaults:

- JDBC URL: `jdbc:h2:mem:taskdb`
- Username: `sa`
- Password: empty

## Example REST Requests

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Write release notes",
    "description": "Summarize completed work for this sprint",
    "status": "PENDING"
  }'
```

```bash
curl "http://localhost:8080/api/tasks?page=0&size=5&keyword=release&status=PENDING"
```

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Write final release notes",
    "description": "Include deployment details",
    "status": "DONE"
  }'
```

```bash
curl -X DELETE http://localhost:8080/api/tasks/1
```

## MySQL Configuration

The app defaults to H2, but you can switch to MySQL by setting environment variables:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
$env:JPA_DIALECT="org.hibernate.dialect.MySQLDialect"
.\mvnw.cmd spring-boot:run
```

## Linux + MySQL Guide

For a complete Linux localhost setup guide using MySQL, see:

- [docs/RUN_LINUX_MYSQL.md](c:/Users/Rakesh-PC/Desktop/Java%20Projects/task-manager/docs/RUN_LINUX_MYSQL.md)

## Docker + MySQL Guide

For a complete Docker-based localhost setup guide using MySQL, see:

- [docs/RUN_DOCKER_MYSQL.md](c:/Users/Rakesh-PC/Desktop/Java%20Projects/task-manager/docs/RUN_DOCKER_MYSQL.md)

## Running with Docker

```powershell
cd .\task-manager
docker build -t task-manager:latest .
docker run --rm -p 8080:8080 task-manager:latest
```

For MySQL in Docker:

```powershell
docker run --rm -p 8080:8080 `
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
  -e DB_USERNAME="root" `
  -e DB_PASSWORD="your-password" `
  -e DB_DRIVER="com.mysql.cj.jdbc.Driver" `
  -e JPA_DIALECT="org.hibernate.dialect.MySQLDialect" `
  task-manager:latest
```
