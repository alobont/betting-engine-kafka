# Spec Checklist

This checklist is complete only when the requirements in `REQUIREMENTS.md` and the architecture constraints in `AGENTS.md` are fully implemented.

## 1. Project bootstrap
- [ ] Create a Java 21 + Spring Boot project
- [ ] Add required dependencies: Web, Validation, Kafka, RocketMQ Spring Boot starter, Data JPA, H2, Actuator, Protobuf runtime
- [ ] Add Protobuf code generation for the chosen build tool
- [ ] Set the root package to `com.sporty.bettingengine`
- [ ] Create the required package structure:
  - [ ] `com.sporty.bettingengine.config`
  - [ ] `com.sporty.bettingengine.controller`
  - [ ] `com.sporty.bettingengine.dto.api`
  - [ ] `com.sporty.bettingengine.dto.messaging`
  - [ ] `com.sporty.bettingengine.entity`
  - [ ] `com.sporty.bettingengine.exception`
  - [ ] `com.sporty.bettingengine.messaging.kafka`
  - [ ] `com.sporty.bettingengine.messaging.rocketmq`
  - [ ] `com.sporty.bettingengine.repository`
  - [ ] `com.sporty.bettingengine.service`
- [ ] Create `BettingEngineApplication` in the root package

## 2. Configuration
- [ ] Add application configuration for HTTP, Kafka, RocketMQ, JPA, H2, and Actuator
- [ ] Centralize topic names for `event-outcomes` and `bet-settlements`
- [ ] Configure local-development-friendly defaults
- [ ] Configure JSON serialization for HTTP payloads
- [ ] Configure Protobuf serialization for Kafka and RocketMQ payloads

## 3. In-memory bet storage
- [ ] Implement the bet entity with:
  - [ ] `betId`
  - [ ] `userId`
  - [ ] `eventId`
  - [ ] `eventMarketId`
  - [ ] `eventWinnerId`
  - [ ] `betAmount`
- [ ] Create the Spring Data JPA repository for bets
- [ ] Seed a deterministic in-memory H2 dataset for bets
- [ ] Verify lookup by `eventId`

## 4. API contract
- [ ] Implement `POST /event-outcomes`
- [ ] Create API request DTO with:
  - [ ] `eventId`
  - [ ] `eventName`
  - [ ] `eventWinnerId`
- [ ] Validate required fields strictly
- [ ] Return clear client errors for invalid requests
- [ ] Return a success response when publish to Kafka succeeds

## 5. Kafka publish flow
- [ ] Implement Kafka producer for topic `event-outcomes`
- [ ] Map API request DTO to the Protobuf Kafka message type
- [ ] Serialize Kafka messages with Protobuf
- [ ] Publish only valid event outcomes
- [ ] Log request receipt and Kafka publish result
- [ ] Fail the request clearly if Kafka publish fails

## 6. Kafka consume and bet matching
- [ ] Implement Kafka consumer for topic `event-outcomes`
- [ ] Deserialize the Protobuf message into a typed event outcome model
- [ ] Log consume start and consume result
- [ ] Find bets by `eventId`
- [ ] Verify that matching identifies the bets that require downstream settlement messages
- [ ] Handle invalid consumed messages clearly and safely

## 7. RocketMQ settlement publish flow
- [ ] Define the minimal RocketMQ settlement Protobuf payload
- [ ] Implement RocketMQ producer for topic `bet-settlements`
- [ ] Serialize RocketMQ messages with Protobuf
- [ ] Publish one RocketMQ message per matched bet
- [ ] Log RocketMQ publish attempts and results
- [ ] Do not silently drop publish failures

## 8. Idempotency and failure behavior
- [ ] Implement duplicate-protection logic for repeated Kafka deliveries during a single runtime
- [ ] Derive a stable settlement key from `betId` and `eventId`
- [ ] Ensure duplicate delivery does not produce inconsistent duplicate side effects
- [ ] Keep retry behavior controlled and understandable

## 9. Error handling and observability
- [ ] Implement global exception handling
- [ ] Add structured logs for:
  - [ ] request receipt
  - [ ] Kafka publish
  - [ ] Kafka consume
  - [ ] bet match count
  - [ ] RocketMQ publish
- [ ] Expose health endpoints
- [ ] Add basic metrics if cheap in the chosen implementation

## 10. Testing
- [ ] Add unit tests for request validation
- [ ] Add unit tests for matching by `eventId`
- [ ] Add unit tests for duplicate handling behavior
- [ ] Add integration tests for API to Kafka publish
- [ ] Add integration tests for Kafka consume to bet lookup
- [ ] Add integration tests for bet lookup to RocketMQ publish
- [ ] Ensure integration tests start or target a local `kind` Kubernetes cluster and execute against deployed Kubernetes resources
- [ ] Verify message schemas remain explicit and stable

## 11. Containerization and Kubernetes
- [ ] Create a Dockerfile for the service
- [ ] Provide Kubernetes manifests for the betting engine service
- [ ] Ensure Kafka is reachable inside the cluster
- [ ] Ensure RocketMQ is reachable inside the cluster
- [ ] Add an `nginx` deployment in front of externally exposed HTTP traffic
- [ ] Do not expose the application directly through `NodePort`
- [ ] Standardize local Kubernetes development on `kind` so Windows with Docker Desktop and Linux both support one-line cluster creation and deletion

## 12. Final verification
- [ ] Confirm every requirement in `REQUIREMENTS.md` is implemented
- [ ] Confirm implementation still follows `AGENTS.md`
- [ ] Confirm the service is intentionally small and easy to reason about
- [ ] Record final verification notes in `.ai/PROGRESS_NOTES.md`
- [ ] Record any generalizable mistakes or durable implementation lessons in `.ai/LESSONS_LEARNED.md`

## 13. AI working discipline
- [ ] Review `.ai/SPEC_CHECKLIST.md` before implementation work
- [ ] Keep `.ai/SPEC_CHECKLIST.md` updated as tasks are completed
- [ ] Keep `.ai/PROGRESS_NOTES.md` updated during meaningful progress
- [ ] Capture generalized mistakes and reusable lessons in `.ai/LESSONS_LEARNED.md`

## 14. Local Kubernetes workflow and end-to-end execution
- [ ] Add a root `README.md` that explains how to run the local Kubernetes environment, run tests, and remove the local cluster
- [ ] Document one-line local cluster startup for Windows PowerShell with Docker Desktop and Linux
- [ ] Document one-line local namespace or deployment teardown and full cluster deletion for Windows PowerShell with Docker Desktop and Linux
- [ ] Run end-to-end tests against the locally running Kubernetes deployment
