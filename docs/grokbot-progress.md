# Grokbot progress

| Phase | Status | Next |
| --- | --- | --- |
| P01 | PASS | P02 |
| P02 | PASS | P03 |
| P03 | not started | |
| P04 | not started | |
| P05 | not started | |
| P06 | not started | |
| P07 | not started | |

## P01 — Research overlap and confirm the MVP

**Phase and status:** P01 PASS

**Files changed:** `docs/overlap.md`, `docs/project-contract.md`, `docs/research/p01_probe.py`, `docs/grokbot-progress.md`

**Commands and results:**

- `python3 docs/research/p01_probe.py` on Python 3.12.3 / SQLite 3.45.1. Exit 0, last line `PROBE_OK`. Hidden deletion: response-and-count finding null; step-aligned change count 1 for reservation `R-1`. Duplicate cancel: effect counts 1 and 2 for `ship-cancel:O-1`. Identical control change count 0 and byte-stable. Destination `dock-b` retained and `request_id` absent from that diff.
- `java -version` reports OpenJDK 21.0.13. `mvn` and `docker` are not on `PATH`, so Karate 2.1.4, WireMock 3.13.2, goldenbox, and abheeshtroy/BehaviorDiff were not executed.
- Maven metadata and GitHub repository metadata were read on 2026-11-23 for the versions and commits cited in `docs/overlap.md`.

**Artifact paths:** `docs/overlap.md`, `docs/project-contract.md`, `docs/research/p01_probe.py`

**Open issues:** traffic-diff's license file was not opened. abheeshtroy/BehaviorDiff was not run. Neither blocks the narrowed MVP, because the acceptance case is a local SQLite and in-process-sink report that those products do not ship.

**Next phase:** P02

Decision carried forward: proceed. Author the demo shop, schema, and scenarios in this repo. Pin Picocli, sqlite-jdbc, Jackson, and JUnit from Maven Central. Do not add Docker, Postgres, or a paid service.

## P02 — Create the repository and data contracts

**Phase and status:** P02 PASS

**Files changed:** `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle.lockfile`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `.gitignore`, `README.md`, `docs/data-contract.md`, `docs/project-contract.md`, `fixtures/manifest.json`, `fixtures/journeys/cancel-o1.valid.json`, `fixtures/journeys/unknown-action.invalid.json`, `fixtures/journeys/malformed.invalid.json`, `fixtures/oracle/cancel-o1.md`, `src/main/java/dev/behaviordiff/cli/BehaviorDiff.java`, `src/main/java/dev/behaviordiff/cli/ValidateCommand.java`, `src/main/java/dev/behaviordiff/contract/ContractException.java`, `src/main/java/dev/behaviordiff/contract/ErrorCategory.java`, `src/main/java/dev/behaviordiff/contract/Journey.java`, `src/main/java/dev/behaviordiff/contract/JourneyValidator.java`, `src/test/java/dev/behaviordiff/cli/ValidateCommandTest.java`, `src/test/java/dev/behaviordiff/contract/JourneyValidatorTest.java`, `docs/grokbot-progress.md`

**Commands and results:**

- `./gradlew test --write-locks --no-daemon` on Gradle 9.9.2 and OpenJDK 21.0.13. Exit 0. 10 tests passed, including valid fixture acceptance, `UNKNOWN_ACTION` for `teleport_order`, malformed JSON, oversized input, duplicate keys, and an embedded URL.
- `./gradlew installDist --no-daemon` exit 0.
- `./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/cancel-o1.valid.json` exit 0. Stdout: `accepted fixture cancel-o1 seed seed-cancel-o1 actions 1`.
- `./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/unknown-action.invalid.json` exit 3. Stderr begins `UNKNOWN_ACTION: action 'teleport_order'`.
- The same invalid file through `./gradlew run` prints that error and Gradle then exits 1 because the application exit is non-zero. The installed script is the public entry point.

**Artifact paths:** `fixtures/journeys/cancel-o1.valid.json`, `fixtures/journeys/unknown-action.invalid.json`, `fixtures/oracle/cancel-o1.md`, `fixtures/manifest.json`, `docs/data-contract.md`, `gradle.lockfile`

**Open issues:** none for this gate. Report JSON is specified and not produced yet.

**Next phase:** P03
