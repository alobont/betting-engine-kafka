# Progress Notes

Use this file as a short implementation log.

Entry format:
- `YYYY-MM-DD HH:MM` - what changed, why, and how it was verified

Notes:
- keep entries short
- record blockers and assumptions explicitly
- record test or verification commands when relevant
- use `LESSONS_LEARNED.md` for generalized takeaways rather than repeating them here

- `2026-03-11 22:53` - Updated `.ai/SPEC_CHECKLIST.md` to align with Protobuf internal messaging, local `kind` Kubernetes integration testing, and explicit end-to-end cluster execution. Added root `README.md` documenting one-line local cluster startup, test workflow, namespace cleanup, and full cluster teardown for Windows PowerShell and Linux.
- `2026-03-11 23:50` - Implemented the Gradle-based Spring Boot service, Protobuf messaging flow, H2 seed data, Dockerfile, `kind` Kubernetes manifests, Gradle local-cluster tasks, and cluster-facing integration/e2e tests. Verification is in progress, starting with test isolation fixes and then the full local Kubernetes flow.
- `2026-03-12 00:30` - Completed verification. Ran `.\gradlew.bat integrationTest --no-daemon`, `.\gradlew.bat test e2eTest --no-daemon`, and `.\gradlew.bat test --rerun-tasks --no-daemon` with `JAVA_HOME` set to Java 21 and `KIND_BIN` set to the local `kind.exe`. The local `kind` deployment handled the full HTTP -> Kafka -> bet lookup -> RocketMQ flow through `nginx`, and the checklist is now fully complete.
- `2026-03-12 10:47` - Reworked the follow-up architecture toward Redis as the in-memory bet store and cluster-wide idempotency backend. Added `docs/AGENTS_REDIS_READY.md` as a copy-paste-ready replacement body for `AGENTS.md` and updated `.ai/SPEC_CHECKLIST.md` so Redis, dedicated Redis deployment, and process-safe local locking are now the outstanding implementation targets.
- `2026-03-12 12:52` - Replaced H2/JPA with Redis-backed bet storage and Redis settlement claims, removed the local duplicate guard from the correctness path, added a dedicated Redis deployment/service plus Redis wait wiring in Kubernetes, and updated the root `README.md` for the Redis-backed local workflow. Verified with `.\gradlew.bat test --no-daemon`, `.\gradlew.bat integrationTest --no-daemon`, and `.\gradlew.bat e2eTest --no-daemon` using Java 21 and the local `kind.exe`; the deployed stack passed HTTP -> Kafka -> Redis bet lookup/idempotency -> RocketMQ through `nginx`.
- `2026-03-12 13:11` - Renamed the Java and generated Protobuf package root from `com.sporty.bettingengine` to `com.bettingengine`, moved the source/test directory tree to match, updated Gradle coordinates, and removed remaining `com.sporty` references from tracked text files. Verified with `.\gradlew.bat localK8sVerify --no-daemon` using Java 21 and the local `kind.exe`.
- `2026-03-12 13:23` - Added Gradle-managed `kind` bootstrapping so `localClusterUp` now downloads the pinned platform-specific binary into `.tools/` when missing and uses it by default. Also updated `README.md` to document the managed tool flow and optional overrides.
