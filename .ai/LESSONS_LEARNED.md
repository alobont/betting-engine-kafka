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
