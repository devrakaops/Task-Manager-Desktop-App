# Task Manager Application — On-Premise Server Deployment Guide

This guide outlines the complete, step-by-step process to build, configure, and run the **Task Manager** Spring Boot application directly on a bare-metal or virtual **Ubuntu 24.04 LTS** server without using containers.

## 📋 Prerequisites

Ensure the following software packages are installed on your Ubuntu server:

* **Operating System:** Ubuntu 24.04 LTS

* **Java Development Kit:** OpenJDK 21

* **Build Tool:** Apache Maven 3.9+ (Global CLI)

* **Database:** MySQL Server 8.0+

## 🛠️ Step 1: Install Required Stack

Update your package index and install OpenJDK 21, Maven, and MySQL Server:

```
sudo apt update
sudo apt install -y openjdk-21-jdk maven mysql-server

```

### Verify Installations

```
java -version
mvn -version
mysql --version

```

## 🗄️ Step 2: Configure MySQL Database

1. **Start and Enable MySQL Service:**

   ```
   sudo systemctl start mysql
   sudo systemctl enable mysql
   
   ```

2. **Create Database and Dedicated User:**
   Log into the MySQL shell as root:

   ```
   sudo mysql -u root
   
   ```

   Execute the following SQL commands to create the `taskdb` database and a dedicated user (`taskuser`):

   ```
   CREATE DATABASE IF NOT EXISTS taskdb;
   CREATE USER 'taskuser'@'localhost' IDENTIFIED BY 'taskpassword';
   GRANT ALL PRIVILEGES ON taskdb.* TO 'taskuser'@'localhost';
   FLUSH PRIVILEGES;
   EXIT;
   
   ```

## 📦 Step 3: Build the Application

Navigate to your project's root directory where `pom.xml` is located and build the executable `.jar` file using global Maven:

```
cd /path/to/task-manager
mvn clean package -DskipTests

```

*Verification:* Confirm that the artifact `task-manager-0.0.1-SNAPSHOT.jar` has been generated inside the `target/` directory:

```
ls -lh target/task-manager-0.0.1-SNAPSHOT.jar

```

## 🔐 Step 4: Configure Environment Variables

To keep credentials secure and out of source code, export the required database connection variables in your server session:

```
export DB_URL="jdbc:mysql://localhost:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME="taskuser"
export DB_PASSWORD="taskpassword"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
export JPA_DIALECT="org.hibernate.dialect.MySQLDialect"

```

## 🚀 Step 5: Run the Application

### Option A: Foreground Execution (Testing & Debugging)

Run the `.jar` file directly in your terminal session:

```
java -jar target/task-manager-0.0.1-SNAPSHOT.jar

```

### Option B: Production Background Service (`systemd`)

For a true on-premise production setup, configure `systemd` so the application runs in the background, logs properly, and starts automatically on server reboot.

1. **Create a `systemd` Service File:**

   ```
   sudo nano /etc/systemd/system/task-manager.service
   
   ```

2. **Add the Following Configuration:**
   *(Adjust the `User` and `WorkingDirectory`/`ExecStart` paths to match your server setup)*

   ```
   [Unit]
   Description=Task Manager Spring Boot Application
   After=syslog.target network.target mysql.service
   
   [Service]
   User=root
   WorkingDirectory=/root/task-manager
   Environment="DB_URL=jdbc:mysql://localhost:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   Environment="DB_USERNAME=taskuser"
   Environment="DB_PASSWORD=taskpassword"
   Environment="DB_DRIVER=com.mysql.cj.jdbc.Driver"
   Environment="JPA_DIALECT=org.hibernate.dialect.MySQLDialect"
   ExecStart=/usr/bin/java -jar /root/task-manager/target/task-manager-0.0.1-SNAPSHOT.jar
   SuccessExitStatus=143
   Restart=always
   RestartSec=10
   
   [Install]
   WantedBy=multi-user.target
   
   ```

3. **Reload `systemd`, Start, and Enable the Service:**

   ```
   sudo systemctl daemon-reload
   sudo systemctl start task-manager
   sudo systemctl enable task-manager
   
   ```

## ✅ Step 6: Verification

1. **Check Service Status:**

   ```
   sudo systemctl status task-manager
   
   ```

   *Look for `Active: active (running)`.*

2. **View Application Logs:**

   ```
   sudo journalctl -u task-manager -f
   
   ```

   *Confirm you see `Tomcat started on port 8080` and `Started TaskManagerApplication`.*

3. **Test the Endpoint:**

   ```
   curl http://localhost:8080/tasks
   
   ```

   Or access `http://<YOUR_SERVER_IP>:8080/tasks` in your web browser.
