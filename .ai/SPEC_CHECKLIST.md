# Spec Checklist

This checklist is complete only when the requirements in `REQUIREMENTS.md` and the architecture constraints in `AGENTS.md` are fully implemented.

Status note:
- checklist is complete and verified against both non-HA and HA local Kubernetes modes

## 1. Project bootstrap
- [x] Create a Java 21 + Spring Boot project
- [x] Add required dependencies: Web, Validation, Kafka, RocketMQ Spring Boot starter, Ignite client, Actuator, Protobuf runtime
- [x] Add Protobuf code generation for the chosen build tool
- [x] Set the root package to `com.bettingengine`
- [x] Create the required package structure:
  - [x] `com.bettingengine.config`
  - [x] `com.bettingengine.controller`
  - [x] `com.bettingengine.dto.api`
  - [x] `com.bettingengine.dto.messaging`
  - [x] `com.bettingengine.entity`
  - [x] `com.bettingengine.exception`
  - [x] `com.bettingengine.messaging.kafka`
  - [x] `com.bettingengine.messaging.rocketmq`
  - [x] `com.bettingengine.repository`
  - [x] `com.bettingengine.service`
- [x] Create `BettingEngineApplication` in the root package

## 2. Configuration
- [x] Add application configuration for HTTP, Kafka, RocketMQ, Ignite, and Actuator
- [x] Centralize topic names for `event-outcomes` and `bet-settlements`
- [x] Configure local-development-friendly defaults for Ignite-backed local deployment
- [x] Configure JSON serialization for HTTP payloads
- [x] Configure Protobuf serialization for Kafka and RocketMQ payloads
- [x] Configure Ignite settings for in-memory mode, transactions, partitioning, and settlement-key expiry
- [x] Disable Ignite native persistence explicitly

## 3. In-memory bet storage
- [x] Implement the Ignite-backed bet record with:
  - [x] `betId`
  - [x] `userId`
  - [x] `eventId`
  - [x] `eventMarketId`
  - [x] `eventWinnerId`
  - [x] `betAmount`
- [x] Create the Ignite-backed repository for bets
- [x] Seed a deterministic in-memory Ignite dataset for bets
- [x] Verify lookup by `eventId`
- [x] Configure bets and settlement claims for ACID-safe transactional access
- [x] Define deliberate partitioning, sharding, and affinity for event-driven access patterns

## 4. API contract
- [x] Implement `POST /event-outcomes`
- [x] Create API request DTO with:
  - [x] `eventId`
  - [x] `eventName`
  - [x] `eventWinnerId`
- [x] Validate required fields strictly
- [x] Return clear client errors for invalid requests
- [x] Return a success response when publish to Kafka succeeds

## 5. Kafka publish flow
- [x] Implement Kafka producer for topic `event-outcomes`
- [x] Map API request DTO to the Protobuf Kafka message type
- [x] Serialize Kafka messages with Protobuf
- [x] Publish only valid event outcomes
- [x] Log request receipt and Kafka publish result
- [x] Fail the request clearly if Kafka publish fails

## 6. Kafka consume and bet matching
- [x] Implement Kafka consumer for topic `event-outcomes`
- [x] Deserialize the Protobuf message into a typed event outcome model
- [x] Log consume start and consume result
- [x] Find bets by `eventId` from Ignite
- [x] Verify that matching identifies the bets that require downstream settlement messages
- [x] Handle invalid consumed messages clearly and safely

## 7. RocketMQ settlement publish flow
- [x] Define the minimal RocketMQ settlement Protobuf payload
- [x] Implement RocketMQ producer for topic `bet-settlements`
- [x] Serialize RocketMQ messages with Protobuf
- [x] Publish one RocketMQ message per matched bet
- [x] Log RocketMQ publish attempts and results
- [x] Do not silently drop publish failures

## 8. Idempotency and failure behavior
- [x] Implement duplicate-protection logic for repeated Kafka deliveries across pods, processes, and Ignite nodes
- [x] Derive a stable settlement key from `betId` and `eventId`
- [x] Claim settlement keys atomically in Ignite with transactional safety and bounded expiry
- [x] Ensure duplicate delivery does not produce inconsistent duplicate side effects across pods, processes, and shards
- [x] Ensure any local guards or locks are only process-local optimizations and are safe under concurrent execution
- [x] Keep retry behavior controlled and understandable
- [x] If distributed locks are used, ensure they are Ignite-backed and safe across nodes and shards

## 9. Error handling and observability
- [x] Implement global exception handling
- [x] Add structured logs for:
  - [x] request receipt
  - [x] Kafka publish
  - [x] Kafka consume
  - [x] bet match count
- [x] Ignite idempotency claim
  - [x] RocketMQ publish
- [x] Expose health endpoints
- [x] Add basic metrics if cheap in the chosen implementation

## 10. Testing
- [x] Add unit tests for request validation
- [x] Add unit tests for matching by `eventId` from Ignite
- [x] Add unit tests for duplicate handling behavior with Ignite-backed idempotency
- [x] Add unit tests for safe concurrent behavior of any remaining local guards or locks
- [x] Add integration tests for API to Kafka publish
- [x] Add integration tests for Kafka consume to Ignite bet lookup
- [x] Add integration tests for bet lookup to Ignite idempotency claim
- [x] Add integration tests for successful Ignite claim to RocketMQ publish
- [x] Add integration tests that simulate competing process or pod settlement attempts against shared Ignite
- [x] Add integration tests for transactional safety, shard safety, and any Ignite-backed lock behavior that participates in correctness
- [x] Ensure integration tests start or target a local `kind` Kubernetes cluster and execute against deployed Kubernetes resources using Ignite
- [x] Verify message schemas remain explicit and stable

## 11. Containerization and Kubernetes
- [x] Create a Dockerfile for the service
- [x] Provide Kubernetes manifests for the betting engine service
- [x] Ensure Kafka is reachable inside the cluster
- [x] Ensure RocketMQ is reachable inside the cluster
- [x] Add a dedicated Apache Ignite deployment and service for bet storage and settlement idempotency
- [x] Ensure Ignite is reachable inside the cluster
- [x] Add an `nginx` deployment in front of externally exposed HTTP traffic
- [x] Do not expose the application directly through `NodePort`
- [x] Standardize local Kubernetes development on `kind` so Windows with Docker Desktop and Linux both support one-line cluster creation and deletion

## 12. Final verification
- [x] Confirm every requirement in `REQUIREMENTS.md` is implemented under the Ignite architecture
- [x] Confirm implementation still follows `AGENTS.md`
- [x] Confirm the service is intentionally small and easy to reason about
- [x] Record final verification notes in `.ai/PROGRESS_NOTES.md`
- [x] Record any generalizable mistakes or durable implementation lessons in `.ai/LESSONS_LEARNED.md`

## 13. AI working discipline
- [x] Review `.ai/SPEC_CHECKLIST.md` before implementation work
- [x] Keep `.ai/SPEC_CHECKLIST.md` updated as tasks are completed
- [x] Keep `.ai/PROGRESS_NOTES.md` updated during meaningful progress
- [x] Capture generalized mistakes and reusable lessons in `.ai/LESSONS_LEARNED.md`

## 14. Local Kubernetes workflow and end-to-end execution
- [x] Add a root `README.md` that explains how to run the local Kubernetes environment, run tests, and remove the local cluster
- [x] Document one-line local cluster startup for Windows PowerShell with Docker Desktop and Linux
- [x] Document one-line local namespace or deployment teardown and full cluster deletion for Windows PowerShell with Docker Desktop and Linux
- [x] Run end-to-end tests against the locally running Ignite-backed Kubernetes deployment
- [x] Update the root `README.md` so the local workflow reflects Ignite rather than Redis

## 15. Optional HA local mode
- [x] Keep default local mode non-HA
- [x] Add an opt-in HA mode enabled with `-Plocal.ha=true`
- [x] Use a dedicated HA cluster name, namespace, context, and host port so HA and non-HA local modes do not conflict
- [x] Provide a real Kafka HA topology for local HA mode
- [x] Provide a real Ignite HA topology for local HA mode in pure in-memory mode
- [x] Provide a real RocketMQ HA topology for local HA mode
- [x] Scale the betting-engine and `nginx` deployments to `2` replicas in HA mode
- [x] Add HA-focused integration coverage for Ignite node recovery, transactional correctness, and shard-safe duplicate protection
- [x] Verify the HA integration and end-to-end flow against the deployed local HA cluster using Ignite
