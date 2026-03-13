# Betting Engine Kafka

This project uses:
- Java 21
- Spring Boot
- Gradle
- Apache Ignite in pure in-memory mode for the bet store and settlement idempotency claims
- `kind` for the local Kubernetes cluster

The local Kubernetes workflow is standardized around the Gradle wrapper so Windows with Docker Desktop and Linux both use the same commands.

## Prerequisites
- Java 21 available through `JAVA_HOME`
- Docker Desktop on Windows, or Docker Engine / Docker Desktop on Linux
- `kubectl`

`kind` does not need to be installed globally. `localClusterUp` downloads the pinned platform-specific `kind` binary into `.tools/` automatically on first use and reuses it afterward.

Optional overrides:
- `-Pkind.bin=/absolute/path/to/kind`
- `-Pkubectl.bin=/absolute/path/to/kubectl`
- `-Pkind.version=v0.31.0`

## Local modes

Default non-HA mode:
- cluster name: `betting-engine-local`
- namespace: `betting-engine-local`
- `kubectl` context: `kind-betting-engine-local`
- HTTP entry point: `http://localhost:8080`

HA mode:
- cluster name: `betting-engine-local-ha`
- namespace: `betting-engine-local-ha`
- `kubectl` context: `kind-betting-engine-local-ha`
- HTTP entry point: `http://localhost:8081`
- enable with `-Plocal.ha=true`

In Windows PowerShell, quote the HA property so it is passed to Gradle as one argument.

## Start the local cluster and deploy the stack

Windows PowerShell, default mode:

```powershell
.\gradlew.bat localClusterUp
```

Windows PowerShell, HA mode:

```powershell
.\gradlew.bat localClusterUp '-Plocal.ha=true'
```

Linux, default mode:

```bash
./gradlew localClusterUp
```

Linux, HA mode:

```bash
./gradlew localClusterUp -Plocal.ha=true
```

`localClusterUp`:
- creates or reuses the selected `kind` cluster
- downloads `kind` into `.tools/` if the managed binary is missing
- builds the local `bootJar` and Docker image
- loads the image into `kind`
- deploys Kafka, RocketMQ, Ignite, the betting engine service, and `nginx`
- initializes the local broker and Ignite resources required for deterministic verification
- recreates the required `event-outcomes` Kafka topic and `bet-settlements` RocketMQ topic

HA mode deploys:
- Kafka KRaft `StatefulSet` with `3` broker/controller pods
- Ignite `StatefulSet` with `3` in-memory nodes
- RocketMQ with `2` nameserver stateful pods plus `2` sync-master / `2` slave broker deployments
- `2` betting-engine pods after HA bootstrap completes
- `2` `nginx` pods

In HA mode the application cold-starts at `1` replica and then scales to `2` replicas after the first pod finishes startup. This keeps the final HA shape at two pods while avoiding concurrent first-start schema work during local cluster bootstrap.

## Run tests

Windows PowerShell, default mode:

```powershell
.\gradlew.bat localK8sVerify
```

Windows PowerShell, HA mode:

```powershell
.\gradlew.bat localK8sVerify '-Plocal.ha=true'
```

Linux, default mode:

```bash
./gradlew localK8sVerify
```

Linux, HA mode:

```bash
./gradlew localK8sVerify -Plocal.ha=true
```

`localK8sVerify` runs:
- unit tests
- integration tests
- local Kubernetes verification against deployed resources
- end-to-end verification through `nginx`

## Remove the deployed namespace

Windows PowerShell, default mode:

```powershell
.\gradlew.bat localNamespaceDown
```

Windows PowerShell, HA mode:

```powershell
.\gradlew.bat localNamespaceDown '-Plocal.ha=true'
```

Linux:

```bash
./gradlew localNamespaceDown
./gradlew localNamespaceDown -Plocal.ha=true
```

Use this when you want to clear the deployed resources but keep the cluster control plane.

## Remove the local cluster completely

Windows PowerShell, default mode:

```powershell
.\gradlew.bat localClusterDelete
```

Windows PowerShell, HA mode:

```powershell
.\gradlew.bat localClusterDelete '-Plocal.ha=true'
```

Linux:

```bash
./gradlew localClusterDelete
./gradlew localClusterDelete -Plocal.ha=true
```

Use this for a full local reset, including the `kind` control plane.
