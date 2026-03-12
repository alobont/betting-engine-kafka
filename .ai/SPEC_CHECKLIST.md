# Spec Checklist

This checklist is complete only when the requirements in `REQUIREMENTS.md` and the architecture constraints in `AGENTS.md` are fully implemented.

## 1. Project bootstrap
- [x] Create a Java 21 + Spring Boot project
- [x] Add required dependencies: Web, Validation, Kafka, RocketMQ Spring Boot starter, Data Redis, Actuator, Protobuf runtime
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
- [x] Add application configuration for HTTP, Kafka, RocketMQ, Redis, and Actuator
- [x] Centralize topic names for `event-outcomes` and `bet-settlements`
- [x] Configure local-development-friendly defaults
- [x] Configure JSON serialization for HTTP payloads
- [x] Configure Protobuf serialization for Kafka and RocketMQ payloads
- [x] Configure Redis settings for bet storage and settlement-key TTL

## 3. In-memory bet storage
- [x] Implement the Redis-backed bet record with:
  - [x] `betId`
  - [x] `userId`
  - [x] `eventId`
  - [x] `eventMarketId`
  - [x] `eventWinnerId`
  - [x] `betAmount`
- [x] Create the Redis-backed repository for bets
- [x] Seed a deterministic in-memory Redis dataset for bets
- [x] Verify lookup by `eventId`

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
- [x] Find bets by `eventId` from Redis
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
- [x] Implement duplicate-protection logic for repeated Kafka deliveries across pods and processes
- [x] Derive a stable settlement key from `betId` and `eventId`
- [x] Claim settlement keys atomically in Redis with bounded TTL
- [x] Ensure duplicate delivery does not produce inconsistent duplicate side effects across pods and processes
- [x] Ensure any local guards or locks are only process-local optimizations and are safe under concurrent execution
- [x] Keep retry behavior controlled and understandable

## 9. Error handling and observability
- [x] Implement global exception handling
- [x] Add structured logs for:
  - [x] request receipt
  - [x] Kafka publish
  - [x] Kafka consume
  - [x] bet match count
  - [x] Redis idempotency claim
  - [x] RocketMQ publish
- [x] Expose health endpoints
- [x] Add basic metrics if cheap in the chosen implementation

## 10. Testing
- [x] Add unit tests for request validation
- [x] Add unit tests for matching by `eventId` from Redis
- [x] Add unit tests for duplicate handling behavior
- [x] Add unit tests for safe concurrent behavior of any remaining local guards or locks
- [x] Add integration tests for API to Kafka publish
- [x] Add integration tests for Kafka consume to Redis bet lookup
- [x] Add integration tests for bet lookup to Redis idempotency claim
- [x] Add integration tests for successful Redis claim to RocketMQ publish
- [x] Add integration tests that simulate competing process or pod settlement attempts against shared Redis
- [x] Ensure integration tests start or target a local `kind` Kubernetes cluster and execute against deployed Kubernetes resources
- [x] Verify message schemas remain explicit and stable

## 11. Containerization and Kubernetes
- [x] Create a Dockerfile for the service
- [x] Provide Kubernetes manifests for the betting engine service
- [x] Ensure Kafka is reachable inside the cluster
- [x] Ensure RocketMQ is reachable inside the cluster
- [x] Add a dedicated Redis deployment and service for bet storage and settlement idempotency
- [x] Ensure Redis is reachable inside the cluster
- [x] Add an `nginx` deployment in front of externally exposed HTTP traffic
- [x] Do not expose the application directly through `NodePort`
- [x] Standardize local Kubernetes development on `kind` so Windows with Docker Desktop and Linux both support one-line cluster creation and deletion

## 12. Final verification
- [x] Confirm every requirement in `REQUIREMENTS.md` is implemented
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
- [x] Run end-to-end tests against the locally running Redis-backed Kubernetes deployment
