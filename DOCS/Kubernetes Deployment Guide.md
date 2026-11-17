# Task Manager Application — Kubernetes Deployment Guide

This guide provides step-by-step instructions to deploy the **Task Manager** Spring Boot application and **MySQL 8.0** database onto a **Kubernetes (K8s)** cluster. It uses Kubernetes-native abstractions—`Secrets`, `ConfigMaps`, `StatefulSets`, `Deployments`, and `Services`—to handle configuration, persistence, and internal DNS networking cleanly.

---

## 📋 Prerequisites

Ensure the following are ready before deploying:
* A running Kubernetes cluster (e.g., **Minikube**, **Kind**, **K3s**, or a managed cloud cluster like EKS/GKE/AKS).
* `kubectl` CLI installed and configured to communicate with your cluster.
* Your Docker image (`task-manager-app:latest`) built and available to your cluster nodes (either pushed to a container registry like Docker Hub or loaded locally into Minikube/Kind).

Verify cluster access:
```bash
kubectl cluster-info
kubectl get nodes
```

---

## 📂 Project Manifest Structure

Create a `k8s/` directory in the root of your `task-manager` project to organize all Kubernetes manifests cleanly:

```text
task-manager/
├── Dockerfile
└── k8s/
    ├── 01-mysql-secret.yaml
    ├── 02-mysql-configmap.yaml
    ├── 03-mysql-statefulset.yaml
    ├── 04-mysql-service.yaml
    ├── 05-app-configmap.yaml
    ├── 06-app-deployment.yaml
    └── 07-app-service.yaml
```

---

## Step 1: Create MySQL Secret (Sensitive Credentials)

Store sensitive passwords securely using a Kubernetes `Secret` rather than hardcoding them in pod specifications.

**File:** `k8s/01-mysql-secret.yaml`
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysql-secret
type: Opaque
stringData:
  MYSQL_ROOT_PASSWORD: "rootpassword"
  MYSQL_PASSWORD: "taskpassword"
```

---

## Step 2: Create MySQL ConfigMap (Non-Sensitive Configuration)

Store the database name and application user in a `ConfigMap`.

**File:** `k8s/02-mysql-configmap.yaml`
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mysql-config
data:
  MYSQL_DATABASE: "taskdb"
  MYSQL_USER: "taskuser"
```

---

## Step 3: Create MySQL StatefulSet (Database + Persistent Storage)

A `StatefulSet` is used for databases because it provides stable network identities and manages `PersistentVolumeClaims` (PVC) automatically, ensuring your MySQL data survives pod restarts or rescheduling.

**File:** `k8s/03-mysql-statefulset.yaml`
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
spec:
  serviceName: "mysql-service"
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8.0
          ports:
            - containerPort: 3306
              name: mysql
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-secret
                  key: MYSQL_ROOT_PASSWORD
            - name: MYSQL_DATABASE
              valueFrom:
                configMapKeyRef:
                  name: mysql-config
                  key: MYSQL_DATABASE
            - name: MYSQL_USER
              valueFrom:
                configMapKeyRef:
                  name: mysql-config
                  key: MYSQL_USER
            - name: MYSQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-secret
                  key: MYSQL_PASSWORD
          volumeMounts:
            - name: mysql-persistent-storage
              mountPath: /var/lib/mysql
  volumeClaimTemplates:
    - metadata:
        name: mysql-persistent-storage
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
```

---

## Step 4: Create MySQL Headless Service (Internal DNS)

This internal service gives the MySQL pod a predictable DNS hostname (`mysql-service`) inside the cluster so the Java app can always reach it regardless of pod IP changes.

**File:** `k8s/04-mysql-service.yaml`
```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-service
spec:
  selector:
    app: mysql
  ports:
    - port: 3306
      targetPort: 3306
  clusterIP: None
```

---

## Step 5: Create Application ConfigMap

Define the JDBC connection string, username, driver, and Hibernate dialect for the Spring Boot application.

**File:** `k8s/05-app-configmap.yaml`
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: task-manager-config
data:
  DB_URL: "jdbc:mysql://mysql-service:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
  DB_USERNAME: "taskuser"
  DB_DRIVER: "com.mysql.cj.jdbc.Driver"
  JPA_DIALECT: "org.hibernate.dialect.MySQLDialect"
```

---

## Step 6: Create Application Deployment

Deploy the stateless Spring Boot application, injecting configuration from `task-manager-config` and the password from `mysql-secret`.

**File:** `k8s/06-app-deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: task-manager-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: task-manager
  template:
    metadata:
      labels:
        app: task-manager
    spec:
      containers:
        - name: task-manager
          image: task-manager-app:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: task-manager-config
          env:
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-secret
                  key: MYSQL_PASSWORD
```
*(Note: If using Minikube with a locally built image, load it first via `minikube image load task-manager-app:latest` or set `imagePullPolicy: Never`.)*

---

## Step 7: Create Application Service (External Access)

Expose the Task Manager application outside the cluster using a `NodePort` service (or `LoadBalancer` on cloud providers).

**File:** `k8s/07-app-service.yaml`
```yaml
apiVersion: v1
kind: Service
metadata:
  name: task-manager-service
spec:
  type: NodePort
  selector:
    app: task-manager
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30080
```

---

## 🚀 Step 8: Deploy to Kubernetes

Apply all manifests in the `k8s/` directory with a single command:

```bash
kubectl apply -f k8s/
```

---

## ✅ Step 9: Verification & Troubleshooting

1. **Check Pods Status:**
   ```bash
   kubectl get pods
   ```
   Wait until both `mysql-0` and `task-manager-deployment-xxxxx` show `STATUS: Running` and `READY: 1/1`.

2. **Verify Persistent Volume Claim (PVC):**
   ```bash
   kubectl get pvc
   ```
   Confirm `mysql-persistent-storage-mysql-0` is `Bound`.

3. **Inspect Application Logs:**
   ```bash
   kubectl logs -f deployment/task-manager-deployment
   ```
   Look for `Tomcat started on port 8080` and `Started TaskManagerApplication` to verify the database connection succeeded.

4. **Access the Application:**
   * **On bare-metal / VM node:** Open `http://<NODE_IP>:30080/tasks`
   * **On Minikube:** Run `minikube service task-manager-service --url` and append `/tasks` to the generated URL.

---

## 🧹 Cleanup Commands

To tear down the application resources while keeping your persistent database volume intact:
```bash
kubectl delete -f k8s/
```
*(Note: Kubernetes retains PVCs created by StatefulSets by default for data safety. To permanently delete the database volume, run `kubectl delete pvc mysql-persistent-storage-mysql-0`.)*
