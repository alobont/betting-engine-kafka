# AGENTS file (Immutable)
**This file is the agent mission and architecture and must not be changed.**
**If this file is changed by the user, you are NEVER to restore it via git restore, and always accept the current version as-is**

# Mission
Build a betting engine application that simulates sports event outcome handling and bet settlement through Kafka and RocketMQ.

Implementation target: Java 21 + Spring Boot + gradle.

Required Spring Boot modules and libraries:
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-kafka`
- `org.apache.rocketmq:rocketmq-spring-boot-starter`
- `spring-boot-starter-data-jpa`
- `com.h2database:h2`
- `spring-boot-starter-actuator`
- `com.google.protobuf:protobuf-java`

The build must also include Protobuf code generation for the chosen build tool so `.proto` definitions generate Java classes during the build.

`REQUIREMENTS.md` defines the product intent and is immutable. This document translates that intent into architecture and engineering rules for implementation.

# Core Intent
The system exists to do one thing well:
1. Receive a sports event outcome through an API.
2. Publish that outcome to Kafka topic `event-outcomes`.
3. Consume that outcome from Kafka.
4. Find bets in an in-memory data store that relate to the event.
5. Produce settlement messages to RocketMQ topic `bet-settlements`.

This is a focused backend service, not a general sportsbook platform. Keep the design narrow, readable, and deterministic.

# Architectural Principles
## 1. Prefer one deployable service
Implement the application as a single backend service. Do not split into multiple microservices unless the requirements change.

## 2. Keep the business flow explicit
The system flow must remain easy to trace:
`HTTP request -> Kafka publish -> Kafka consume -> find bets affected by eventId -> publish settlement messages to RocketMQ bet-settlements topic`

Avoid hidden side effects, background magic, or overly abstract orchestration.

## 3. Core business logic must stay framework-light
Business rules such as request validation and bet matching should stay in service-layer code that is easy to test without Kafka, RocketMQ, or HTTP concerns.

## 4. Choose simple data models
Use small, explicit models. Use Java records for HTTP DTOs, Protobuf-generated message types for Kafka and RocketMQ payloads, and regular JPA entity classes for persisted bets. Use `BigDecimal` for monetary amounts in Java and preserve precision during serialization. Do not use `float` or `double` for money. Do not add fields, polymorphism, or deep inheritance unless a requirement demands it.

## 5. Optimize for correctness before throughput
The assignment is about clean flow and reliable behavior, not premature scaling. Make the happy path correct, the failure modes visible, and the tests strong.

## 6. Treat asynchronous processing as at-least-once
Kafka and RocketMQ integrations must be designed with duplicate delivery in mind. Settlement publishing should be idempotent from the application perspective.

# Required Domain Model
## Event outcome input
An event outcome contains:
- `eventId`
- `eventName`
- `eventWinnerId`

## Bet record
A bet in the in-memory database contains:
- `betId`
- `userId`
- `eventId`
- `eventMarketId`
- `eventWinnerId`
- `betAmount`

## Settlement message
The requirements only state that messages for bets to be settled must be sent to RocketMQ topic `bet-settlements`.

The internal message payload must use Protobuf and remain minimal. It must contain only the bet data required to identify the bet to be settled downstream.

# Functional Rules
## API behavior
- Expose a single endpoint to receive event outcomes.
- Validate required fields strictly.
- Reject malformed requests with clear client errors.
- Publish only valid event outcomes to Kafka.

## Kafka publishing
- Publish incoming outcomes to topic `event-outcomes`.
- Serialize internal messages with Protobuf.
- Keep producer code thin and observable.
- Log publish intent and result without logging noisy internals.

## Kafka consumption
- Consume from topic `event-outcomes`.
- Deserialize Protobuf messages into a typed event outcome model.
- Reject or dead-letter invalid messages if such handling is added later; for now, log clearly and fail safely.

## Bet matching
- Match bets by `eventId`.
- A match means the bet relates to the event outcome being processed.
- Matching identifies the bets for which a settlement message must be published.

## Settlement publishing
- Produce one RocketMQ message per matched bet to topic `bet-settlements`.
- Serialize internal messages with Protobuf.
- Publishing must be safe under retries and duplicate Kafka delivery.
- The application must not silently drop settlement failures.

# Minimal Internal Design
Structure the code under root package `com.sporty.bettingengine`. The Spring Boot application class must be `com.sporty.bettingengine.BettingEngineApplication` so component scanning naturally covers the whole application.

The package structure must be:
- `com.sporty.bettingengine.config`
- `com.sporty.bettingengine.controller`
- `com.sporty.bettingengine.dto.api`
- `com.sporty.bettingengine.dto.messaging`
- `com.sporty.bettingengine.entity`
- `com.sporty.bettingengine.exception`
- `com.sporty.bettingengine.messaging.kafka`
- `com.sporty.bettingengine.messaging.rocketmq`
- `com.sporty.bettingengine.repository`
- `com.sporty.bettingengine.service`

Package responsibilities:

## `com.sporty.bettingengine.config`
Owns Spring configuration, broker properties, topic properties, Protobuf serialization configuration, and bean wiring that is not handled by auto-configuration.

## `com.sporty.bettingengine.controller`
Owns REST controllers only.

## `com.sporty.bettingengine.dto.api`
Owns HTTP request and response models.

## `com.sporty.bettingengine.dto.messaging`
Owns generated Protobuf payload models used by Kafka and RocketMQ. The `.proto` definitions live under `src/main/proto` and generate Java classes into this package.

## `com.sporty.bettingengine.entity`
Owns JPA entities for the in-memory H2 database.

## `com.sporty.bettingengine.exception`
Owns application exceptions and global exception handling.

## `com.sporty.bettingengine.messaging.kafka`
Owns Kafka producers, consumers, and Kafka-specific support classes.

## `com.sporty.bettingengine.messaging.rocketmq`
Owns RocketMQ producers and RocketMQ-specific support classes.

## `com.sporty.bettingengine.repository`
Owns Spring Data JPA repositories.

## `com.sporty.bettingengine.service`
Owns business orchestration, validation flow, bet matching, idempotency checks, and coordination between repositories and messaging adapters.

# AI Working Files
Use the `.ai` folder as the implementation working area.

Required files:
- `.ai/SPEC_CHECKLIST.md`
- `.ai/PROGRESS_NOTES.md`
- `.ai/LESSONS_LEARNED.md`

Rules:
- the AI must review `.ai/SPEC_CHECKLIST.md` before starting implementation work
- the AI must update `.ai/SPEC_CHECKLIST.md` as checklist items are completed
- the AI must add brief progress notes to `.ai/PROGRESS_NOTES.md` during meaningful implementation progress
- the AI must capture generalized mistakes, corrections, and reusable guidance in `.ai/LESSONS_LEARNED.md`
- the AI must treat the checklist as the execution guide; when the checklist is fully completed, the full spec should be implemented

# Simplicity Rules
The following are default prohibitions unless a requirement forces them:
- no microservice split
- no CQRS split
- no event sourcing
- no ORM-driven persistence model beyond straightforward Spring Data JPA
- no generic repository hierarchy
- no custom internal messaging abstraction over Kafka and RocketMQ
- no complicated retry framework
- no speculative feature flags
- no broad configuration surface for behaviors that are currently fixed

If a design choice makes the code harder to explain than the requirement itself, it is probably too complex.

# Data Storage Guidance
Use an in-memory H2 database for bets as required.

Approach:
- model bets as JPA entities in `com.sporty.bettingengine.entity`
- represent monetary amounts in Java with `BigDecimal`
- access bets through Spring Data JPA repositories in `com.sporty.bettingengine.repository`
- seed a small deterministic bet dataset at startup with `data.sql` or a startup initializer

Do not introduce an external database.

# Delivery Semantics and Idempotency
Because messaging systems can redeliver messages, settlement processing must avoid creating inconsistent duplicate side effects.

Minimum acceptable strategy:
- derive a stable settlement key from `betId` and `eventId`
- ensure the application can detect and avoid duplicate settlement sends during a single runtime
- make duplicate detection explicit in code and tests

If a stronger guarantee is needed later, add it deliberately rather than hiding it behind abstractions.

# Error Handling
Errors must be visible, bounded, and unsurprising.

Rules:
- validation errors return client-safe responses
- Kafka publish failures fail the request path clearly
- Kafka consume failures are logged with enough context to diagnose the broken message
- settlement publish failures are retried only in a controlled and understandable way
- do not swallow exceptions
- do not add fallback behavior that changes business meaning

# Observability
Keep observability practical:
- structured logs for request receipt, Kafka publish, Kafka consume, bet match count, and RocketMQ publish
- health endpoint for service liveness/readiness
- basic metrics if the chosen stack supports them cheaply

Use correlation IDs so a single event outcome can be traced across the full flow.

# Testing Strategy
Code quality is part of the architecture.

Minimum test layers:
## Unit tests
Cover controller validation and service-layer rules:
- request validation
- matching by `eventId`
- duplicate handling behavior

## Integration tests
Cover adapters and end-to-end behavior with realistic boundaries:
- API to Kafka publish
- Kafka consume to bet lookup
- bet lookup to RocketMQ publish

## Contract-level checks
Verify message schemas remain stable and explicit.

Prefer a smaller number of high-value tests over a large number of brittle tests.

# Code Quality Rules
## General
- prefer explicit names over clever abstractions
- keep functions short and single-purpose
- make illegal states hard to represent
- use immutable DTOs where practical
- fail fast on invalid inputs

## Dependency direction
- `controller` may depend on `service` and `dto.api`
- `messaging.kafka` and `messaging.rocketmq` may depend on `service` and `dto.messaging`
- `service` may depend on `repository`, `entity`, and `dto.messaging`
- `repository` depends on `entity`
- `entity` and `dto` must not depend on controllers or messaging adapters

## Configuration
- centralize topic names and broker settings
- keep defaults local-development friendly
- avoid scattering magic strings such as `event-outcomes` and `bet-settlements`

## Maintainability
- every public package should have a clear reason to exist
- remove dead code quickly
- avoid helper classes that exist only to hide simple logic

# API and Message Contract Guidance
Use explicit versionless contracts unless requirements force versioning.

Endpoint:
- `POST /event-outcomes`

API response:
- accept valid requests with a success response that confirms the outcome was published

Message style:
- use JSON for HTTP request and response bodies
- use Protobuf for Kafka and RocketMQ message payloads
- use Protobuf-generated Java classes to keep internal contracts typed, payloads smaller, and Spring Kafka and RocketMQ integration straightforward
- preserve amount precision across messaging; represent money with `BigDecimal` in Java and serialize it without precision loss, for example as a decimal string field in Protobuf

# Deployment Guidance
The system must be containerized and runnable in Kubernetes.

Minimum deployment topology:
- one deployment for the betting engine service
- Kafka accessible inside the cluster
- RocketMQ accessible inside the cluster
- an `nginx` deployment in front of externally exposed HTTP traffic

Do not expose the application directly through `NodePort` when an nginx-based external entry point is expected.

Keep manifests straightforward. Favor readability over heavy templating unless deployment repetition justifies it.

# Non-Goals
Unless requirements change, the system does not need to implement:
- user authentication or authorization
- persistent bet storage beyond the required in-memory H2 database
- odds calculation
- payout calculation
- complex market rules
- partial settlement
- admin UI
- backoffice workflows

# Decision Standard
When multiple valid implementations exist, choose the one that:
1. keeps the end-to-end flow easiest to understand
2. minimizes moving parts
3. keeps business logic easy to test
4. makes operational failure obvious
5. satisfies the requirements without speculative extensibility

# Working Standard for Future Changes
Any future feature or refactor should be rejected if it:
- weakens the clarity of the event-to-settlement flow
- introduces infrastructure complexity without requirement value
- couples business logic to Kafka, RocketMQ, HTTP, or framework details
- reduces testability
- conflicts with the immutable requirements

This project should feel intentionally small, well-structured, and easy to reason about.
