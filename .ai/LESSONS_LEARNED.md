# Lessons Learned

Use this file for generalized lessons that should reduce repeated mistakes.

Entry format:
- `YYYY-MM-DD` - short lesson title
- Context: brief description of the situation
- Lesson: generalized guidance stated as broadly as is still accurate
- Apply next time: concrete behavior to follow in future work

Rules:
- generalize as much as is plausible without becoming vague
- avoid project-only trivia unless it reveals a reusable pattern
- prefer lessons about decision quality, requirement reading, naming, architecture boundaries, testing, and verification
- do not duplicate routine progress updates from `PROGRESS_NOTES.md`

- `2026-03-12` - Drain command output while the process is still running
- Context: Local Kubernetes verification used `kubectl` commands from tests, and the process appeared to hang even though the cluster and application were healthy.
- Lesson: When a test shells out to a command that can emit non-trivial output, consume stdout and stderr concurrently instead of waiting for process completion first; otherwise the process can block on filled buffers and create false negatives.
- Apply next time: Build command runners that stream output during execution, especially for `kubectl logs`, `docker logs`, and similar diagnostics commands.

- `2026-03-12` - Idempotency-aware tests need unique business keys
- Context: The application intentionally suppresses duplicate settlement publishes within a runtime, which can accidentally invalidate repeated tests that reuse the same event and bet identifiers.
- Lesson: When behavior depends on idempotency keys or deduplication state, test cases must use distinct domain identifiers or explicitly reset the relevant state between scenarios.
- Apply next time: Seed enough deterministic test data to support multiple unique end-to-end cases without colliding with earlier scenarios.

- `2026-03-12` - Keep persisted money scale consistent with message assertions
- Context: Settlement message assertions initially disagreed with the database representation because the JPA column scale and the expected serialized amount format did not match.
- Lesson: Precision preservation is not only about avoiding floating point types; the storage scale, object model, and serialized representation must align or tests will reveal avoidable formatting drift.
- Apply next time: Decide the canonical money scale early, enforce it in persistence, and assert against that same representation throughout the pipeline.

- `2026-03-12` - Do not confuse local concurrency control with distributed correctness
- Context: A process-local duplicate guard worked for one runtime but would not have been sufficient once Kubernetes scaling or multiple JVM processes became relevant.
- Lesson: In-memory locks, maps, semaphores, and guards can protect correctness only inside one process; once work may compete across pods or processes, the correctness boundary must move to a shared coordination mechanism.
- Apply next time: When a design may scale beyond one process, classify each lock or guard explicitly as local optimization or shared correctness control and choose the implementation accordingly.

- `2026-03-12` - Prefer one realistic integration environment over duplicated infrastructure harnesses
- Context: Redis-backed verification briefly used a second container orchestration path in tests even though the project already standardized on a local Kubernetes deployment.
- Lesson: When a project already has one integration environment that matches the runtime architecture, adding a second infrastructure harness often increases flakiness and hides the real signal behind environment setup issues.
- Apply next time: Start with the integration environment that most closely matches production topology, and add secondary harnesses only when they cover a gap the primary environment cannot cover cleanly.

- `2026-03-12` - Reset broker state when local tests depend on domain idempotency
- Context: Redis claim cleanup alone was not enough in repeated HA verification runs because old Kafka events could be replayed after pod restarts and recreate deduplication state before the next test started.
- Lesson: When end-to-end tests combine message replay with shared idempotency keys, resetting only the persistence layer is incomplete; the message source may also need a controlled reset so the next run starts from a truly clean boundary.
- Apply next time: For reusable local clusters, decide which brokers and stores must be refreshed before verification and bake that reset into the standard environment bootstrap.

- `2026-03-12` - Document shell-specific invocation quirks at the workflow boundary
- Context: A Gradle property containing dots behaved differently in PowerShell than in POSIX shells, which made a valid HA command fail before Gradle even parsed it.
- Lesson: If a project depends on CLI flags that are shell-sensitive, the documentation must show the exact safe invocation for each supported shell instead of assuming argument parsing is uniform.
- Apply next time: When adding developer-facing command switches, test them in every documented shell and record the shell-safe form in the README immediately.

- `2026-03-12` - Choose money-critical storage semantics before scaling the implementation around them
- Context: The architecture initially scaled around Redis HA and only later revisited whether its failover and replication model was appropriate for a money-sensitive settlement flow.
- Lesson: If a system touches money or settlement correctness, validate the storage engine's consistency, transactional, sharding, and lock guarantees before building operational tooling and test infrastructure around it.
- Apply next time: Treat storage-semantics review as an early architecture gate and update the execution checklist immediately when the chosen guarantees change.

- `2026-03-13` - Do not hide independent control-plane state behind a generic service address
- Context: The HA RocketMQ setup initially placed multiple NameServers behind one Kubernetes Service, which caused producers and brokers to observe incomplete route information because NameServers do not share state the way a typical stateless HTTP layer does.
- Lesson: If a clustered component expects explicit peer addresses rather than a load-balanced virtual endpoint, preserve that model in Kubernetes instead of defaulting to one Service address.
- Apply next time: Check whether multi-node control-plane components replicate state or merely expose discovery endpoints before introducing a Service that load-balances across them.

- `2026-03-13` - Cold-start distributed dependencies sequentially when first-start work is not replica-safe
- Context: Two HA application pods starting simultaneously against a fresh Ignite cluster repeatedly collided on expensive first-start schema work and made local Kubernetes verification unstable.
- Lesson: If a workload has unavoidable one-time bootstrap behavior that is safe after initialization but expensive during a cold start, orchestrate the first replica to complete initialization before scaling out to the final replica count.
- Apply next time: For local HA harnesses, bootstrap stateful dependencies and then scale stateless workers to their steady-state count only after the first instance proves readiness.

- `2026-03-13` - Quote shell arguments that embed list delimiters
- Context: The HA RocketMQ topic-bootstrap command passed a semicolon-delimited nameserver list through `sh -lc`, and the shell split the list into separate commands instead of one `-n` argument.
- Lesson: Any command argument that contains shell metacharacters such as semicolons must be quoted or passed without an intermediate shell, especially in orchestration code that builds commands dynamically.
- Apply next time: Treat endpoint lists, JDBC URLs, and similar delimiter-heavy values as unsafe for raw shell interpolation and quote or escape them deliberately.

- `2026-03-13` - Prefer the narrowest local image build path that satisfies the workflow
- Context: The local verification flow originally built the application image with an in-container Gradle stage, which added another external image pull and made local verification fail for reasons unrelated to the application.
- Lesson: For local orchestration, build the artifact in the host toolchain first and make the containerization step do only the minimum packaging work needed for runtime parity.
- Apply next time: When the repo already standardizes on one build tool, let that tool produce the deployable artifact directly and keep the Docker build as a thin runtime wrapper.
