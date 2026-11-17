# Task Manager Application — Standalone Docker Deployment Guide

This guide provides step-by-step instructions to containerize and run the **Task Manager** Spring Boot application alongside a **MySQL 8.0** database using standalone Docker containers (without `docker-compose`). It incorporates custom Docker networking for DNS-based service discovery and persistent volumes for data durability.

---

## 📋 Prerequisites

Ensure the following tools are installed on your Linux host (Ubuntu 24.04 LTS):
* **Docker Engine** (CLI & Daemon running)
* **Git** (to clone or manage your project source code)

Verify Docker is running:
```bash
docker --version
sudo systemctl status docker
```

---

## 🛠️ Step 1: Create the Multi-Stage `Dockerfile`

In the root directory of your `task-manager` project (where `pom.xml` is located), create a file named `Dockerfile` with the following clean, multi-stage configuration:

```dockerfile
# Stage 1: Build the application using Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application using lightweight JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/task-manager-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 🌐 Step 2: Configure Networking & Persistent Storage

To ensure containers communicate reliably by hostname (preventing broken connections if container IPs change) and that database records survive container restarts/deletions, create a custom network and volume.

### 1. Create a Custom Bridge Network
```bash
docker network create my-custom-net
```
*Verification:* Run `docker network ls` to confirm `my-custom-net` is listed.

### 2. Create a Persistent Volume for MySQL
```bash
docker volume create mysql_data_vol
```
*Verification:* Run `docker volume ls` to confirm `mysql_data_vol` is created.

---

## 🗄️ Step 3: Run the MySQL Container

Launch the MySQL 8.0 container attached to `my-custom-net` and `mysql_data_vol`. Environment variables automatically initialize the database and dedicated user credentials:

```bash
docker run -d \
  --name mysql-container \
  --net my-custom-net \
  --restart always \
  -p 3306:3306 \
  -v mysql_data_vol:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=taskdb \
  -e MYSQL_USER=taskuser \
  -e MYSQL_PASSWORD=taskpassword \
  mysql:8.0
```

*Verification:* Check that the database container is up and running:
```bash
docker ps
```
*(Wait ~10–15 seconds for MySQL to complete its initial setup before starting the Java application.)*

---

## 📦 Step 4: Build & Run the Task Manager Container

### 1. Build the Application Docker Image
From your project root directory, build the image tagged `task-manager-app`:
```bash
docker build -t task-manager-app .
```

### 2. Run the Application Container
Start the Task Manager container on the **same custom network** (`my-custom-net`). Notice that `DB_URL` uses the container name `mysql-container` as the hostname:

```bash
docker run -d \
  --name task-manager-container \
  --net my-custom-net \
  --restart always \
  -p 8080:8080 \
  -e DB_URL="jdbc:mysql://mysql-container:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e DB_USERNAME="taskuser" \
  -e DB_PASSWORD="taskpassword" \
  -e DB_DRIVER="com.mysql.cj.jdbc.Driver" \
  -e JPA_DIALECT="org.hibernate.dialect.MySQLDialect" \
  task-manager-app
```

---

## ✅ Step 5: Verification & Troubleshooting

1. **Confirm Both Containers Are Running:**
   ```bash
   docker ps
   ```
   You should see both `mysql-container` and `task-manager-container` with status `Up`.

2. **Inspect Application Logs:**
   ```bash
   docker logs -f task-manager-container
   ```
   Look for `Tomcat started on port 8080` and `Started TaskManagerApplication` to confirm a successful database handshake.

3. **Test the Web Endpoint:**
   ```bash
   curl http://localhost:8080/tasks
   ```
   Or open `http://<YOUR_SERVER_IP>:8080/tasks` in your browser.

---

## 🧹 Useful Management Commands

* **Stop containers:** `docker stop task-manager-container mysql-container`
* **Start existing containers:** `docker start mysql-container task-manager-container`
* **Remove containers (data remains safe in `mysql_data_vol`):** `docker rm -f task-manager-container mysql-container`
