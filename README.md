# Betting Engine Kafka

This project uses:
- Java 21
- Spring Boot
- Gradle
- Redis for the in-memory bet store and settlement idempotency claims
- `kind` for the local Kubernetes cluster

The local Kubernetes workflow is standardized around the Gradle wrapper so Windows with Docker Desktop and Linux both use the same one-line commands.

## Prerequisites
- Java 21 available through `JAVA_HOME`
- Docker Desktop on Windows, or Docker Engine / Docker Desktop on Linux
- `kubectl`
- `kind`

If `kind` or `kubectl` are not on `PATH`, pass them explicitly:
- `-Pkind.bin=/absolute/path/to/kind`
- `-Pkubectl.bin=/absolute/path/to/kubectl`

Local defaults:
- cluster name: `betting-engine-local`
- namespace: `betting-engine-local`
- exposed HTTP entry point: `http://localhost:8080`

## Start the local cluster and deploy the stack

Windows PowerShell:

```powershell
.\gradlew.bat localClusterUp
```

Linux:

```bash
./gradlew localClusterUp
```

What this does:
- creates or reuses the `kind` cluster
- builds the local Docker image
- loads the image into `kind`
- deploys Kafka, RocketMQ, Redis, the betting engine service, and `nginx`
- refreshes the Redis-backed in-memory dataset by restarting Redis and the betting engine deployment
- creates the required `event-outcomes` Kafka topic and `bet-settlements` RocketMQ topic

## Run tests

Run the full verification flow:

Windows PowerShell:

```powershell
.\gradlew.bat localK8sVerify
```

Linux:

```bash
./gradlew localK8sVerify
```

This runs:
- unit tests
- integration tests
- local Kubernetes integration checks against the deployed resources
- end-to-end verification through `nginx`

The deployed HTTP entry point is:

```text
http://localhost:8080/event-outcomes
```

## Remove the deployed namespace

Windows PowerShell:

```powershell
.\gradlew.bat localNamespaceDown
```

Linux:

```bash
./gradlew localNamespaceDown
```

Use this when you want to clear the deployed resources but keep the cluster.

## Remove the local cluster completely

Windows PowerShell:

```powershell
.\gradlew.bat localClusterDelete
```

Linux:

```bash
./gradlew localClusterDelete
```

Use this for a full local reset.
