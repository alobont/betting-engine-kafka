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

## Manually exercise the deployed stack

Examples below use the default local mode values:
- `CTX=kind-betting-engine-local`
- `NS=betting-engine-local`
- `BASE_URL=http://localhost:8080`
- `KAFKA_EXEC=deployment/kafka`
- `KAFKA_BOOTSTRAP=kafka:9092`
- `SETTLEMENT_KEY=6:4004`

If you started HA mode, replace them with:
- `CTX=kind-betting-engine-local-ha`
- `NS=betting-engine-local-ha`
- `BASE_URL=http://localhost:8081`
- `KAFKA_EXEC=kafka-0`
- `KAFKA_BOOTSTRAP=kafka-bootstrap:9092`
- `SETTLEMENT_KEY=6:4004`

Kafka and RocketMQ messages are Protobuf-encoded. The topic tools below show the stored records and metadata, while the betting-engine logs show the decoded event and settlement flow.

Windows PowerShell:

```powershell
$CTX = 'kind-betting-engine-local'
$NS = 'betting-engine-local'
$BASE_URL = 'http://localhost:8080'
$KAFKA_EXEC = 'deployment/kafka'
$KAFKA_BOOTSTRAP = 'kafka:9092'
$ROCKETMQ_NS_POD = kubectl --context $CTX -n $NS get pod -l app=rocketmq-nameserver -o jsonpath="{.items[0].metadata.name}"
$SETTLEMENT_KEY = '6:4004' # BET_ID:EVENT_ID

$correlationId = [guid]::NewGuid().ToString()
Invoke-WebRequest -Method Post -Uri "$BASE_URL/event-outcomes" -Headers @{ 'X-Correlation-Id' = $correlationId } -ContentType 'application/json' -Body '{"eventId":4004,"eventName":"League Decider","eventWinnerId":31}'
kubectl --context $CTX -n $NS logs -l app=betting-engine --tail=200 --prefix=true | Select-String $correlationId
kubectl --context $CTX -n $NS exec $KAFKA_EXEC -- /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_BOOTSTRAP --topic event-outcomes --from-beginning --max-messages 5 --property print.key=true
kubectl --context $CTX -n $NS exec ignite-0 -- docker-entrypoint.sh cli sql --jdbc-url "jdbc:ignite:thin://ignite-client:10800" --plain "SELECT BET_ID, USER_ID, EVENT_ID, EVENT_MARKET_ID, EVENT_WINNER_ID, BET_AMOUNT FROM BETS WHERE EVENT_ID = 4004 ORDER BY BET_ID"
kubectl --context $CTX -n $NS exec $ROCKETMQ_NS_POD -- sh -lc "/home/rocketmq/rocketmq-5.3.2/bin/mqadmin queryMsgByKey -n 'localhost:9876' -t bet-settlements -k '$SETTLEMENT_KEY'"
```

Linux:

```bash
CTX=kind-betting-engine-local
NS=betting-engine-local
BASE_URL=http://localhost:8080
KAFKA_EXEC=deployment/kafka
KAFKA_BOOTSTRAP=kafka:9092
ROCKETMQ_NS_POD=$(kubectl --context "$CTX" -n "$NS" get pod -l app=rocketmq-nameserver -o jsonpath='{.items[0].metadata.name}')
SETTLEMENT_KEY=6:4004 # BET_ID:EVENT_ID

correlation_id=$(uuidgen)
curl -i -X POST "$BASE_URL/event-outcomes" -H "Content-Type: application/json" -H "X-Correlation-Id: $correlation_id" -d '{"eventId":4004,"eventName":"League Decider","eventWinnerId":31}'
kubectl --context "$CTX" -n "$NS" logs -l app=betting-engine --tail=200 --prefix=true | grep "$correlation_id"
kubectl --context "$CTX" -n "$NS" exec "$KAFKA_EXEC" -- /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server "$KAFKA_BOOTSTRAP" --topic event-outcomes --from-beginning --max-messages 5 --property print.key=true
kubectl --context "$CTX" -n "$NS" exec ignite-0 -- docker-entrypoint.sh cli sql --jdbc-url "jdbc:ignite:thin://ignite-client:10800" --plain "SELECT BET_ID, USER_ID, EVENT_ID, EVENT_MARKET_ID, EVENT_WINNER_ID, BET_AMOUNT FROM BETS WHERE EVENT_ID = 4004 ORDER BY BET_ID"
kubectl --context "$CTX" -n "$NS" exec "$ROCKETMQ_NS_POD" -- sh -lc "/home/rocketmq/rocketmq-5.3.2/bin/mqadmin queryMsgByKey -n 'localhost:9876' -t bet-settlements -k '$SETTLEMENT_KEY'"
```

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
