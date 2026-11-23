# Validation

Recorded 2026-11-23 on the implementation host after `./gradlew check --offline --no-daemon`.

## Host

| Item | Observed |
| --- | --- |
| OS | Linux 6.12.113+ x86_64 |
| CPU | 4 processors |
| Memory | 16780623872 bytes |
| JDK | OpenJDK 21.0.13.0.3 (Ubuntu) |
| Gradle | 9.9.2, wrapper |
| Formatter | Spotless 8.12.0 with google-java-format 1.25.2 |

This is a synthetic fixture measured locally. It is not a validated external dataset. Comparisons are exact. There is no numeric tolerance: amounts are integer USD cents and quantities are integer item counts.

## Commands

| Command | Result |
| --- | --- |
| `./gradlew check --offline --no-daemon` | Exit 0. `spotlessCheck` passed. 29 tests passed. |
| `behaviordiff demo --case duplicate-cancel` | Three fresh runs, output deleted before each. Exit 1. Wall clock 0.398 s, 0.378 s, 0.378 s. All three files sha256 `39f4252950f23a59bf31dbdb1e653bec273d8dc9e4cdb726c8e34293939cb138`, matching `docs/examples/duplicate-cancel.json`. |
| `SafeguardTest.inProcessComparisonStaysInsideTheBudget` | Passed. Budget is 1000 ms for one in-process comparison inside the test JVM. |

## Acceptance metric

Observed surfaces: action `cancel_order`; tables `orders` and `reservations`; sinks `shipping` and `email`.

Fixture count: 1 valid journey (253 bytes), 2 invalid journeys (252 and 2 bytes), 1 seed SQL (569 bytes), 2 hand oracles, 10 named demo cases (6 mutations, 1 identical control, 3 setup failures).

Documented mutations detected: 6 of 6. Identical control changes: 0.

| Case | Expected | Observed |
| --- | --- | --- |
| `unexpected-delete` | Row deletion while the response and the effect count agree | `responseChanged` false, no effect changes, reservation `R-1` `rightOp` `delete` |
| `duplicate-cancel` | Multiplicity kept for one idempotency key, reservation retained | Counts 1 and 2 for `ship-cancel:O-1`, cause `duplicate-idempotency-key`, `R-1` absent on `good` and present on `duplicate-cancel` |
| `volatile-destination` | Ignore `requestId`, keep destination | Report has destination `dock-b` and does not contain `requestId` or `req-`. Amount stays 2500 cents |
| `amount-change` | Keep the amount | 2500 versus 1999 USD cents |
| `missing-email` | Missing effect, count preserved | Cause `missing-effect`, counts 1 and 0, key `email:O-1:canceled` |
| `null-vs-absent` | Null is not the same as a missing payload field | Cause `null-vs-absent`, counts 1 and 1 |
| `identical-control` | No changes, byte-stable | Status `identical`, empty change lists, two encodings equal |

## Boundary and failure inputs

| Input | Observed |
| --- | --- |
| Unknown action `teleport_order` | Exit 3, `UNKNOWN_ACTION`, rejected report has `coverage: null` |
| Malformed `{` | Exit 3, `MALFORMED_INPUT` |
| Duplicate JSON keys | `MALFORMED_INPUT` |
| 65537-byte buffer | `OVERSIZED_INPUT` before the document is trusted |
| URL inside a fixture | `EXTERNAL_SINK` |
| Build sink `https://carrier.example/cancel` | `EXTERNAL_SINK` before `run`. The test build's run counter stayed 0 |
| Build that does not observe `reservations` | `UNOBSERVED_TABLE` before `run` |
| Adapter that throws | Status `incomplete`, exit 2, `coverage` null |
| Truncated snapshot | Status `incomplete`, message contains `truncated`, `coverage` null |
| Output path outside the working directory | Exit 3, file not created |
| `--approve orders.destination` on the reroute case | Exit 0, status `approved` |
| The same approval on `duplicate-cancel` | Exit 1, because the reservation change is not that field |

## Performance budget

The supported demo is one journey step and two in-memory SQLite databases. Budget: the installed `demo --case duplicate-cancel` command finishes in under 2 seconds of wall clock on Java 21, and `Comparison.run` finishes in under 1 second inside a warm test JVM. Both passed. The 2 second bound leaves room for JVM startup, which was most of the observed 0.40 seconds. A regression that opens a socket or sleeps would miss it.

## Trust boundaries

- Journey files are untrusted. The validator caps the read at 65536 bytes, rejects unknown fields, duplicate keys, and strings that contain `://`.
- SQL uses bound parameters. The seed is a classpath resource, not the contents of the journey file.
- Build ids are an allowlist. Unknown ids exit 3.
- Sinks must be exactly `email` or `shipping`. A URL or any other name fails setup before the build runs.
- The shop opens `jdbc:sqlite::memory:` and no other connection. There is no child process and no outbound HTTP client.
- Reports are written only to the one `--output` path, and only when that path is a file inside the working directory.
- `requestId` is volatile and sensitive. It is removed before comparison and is not written to the report. Canonical JSON escapes `<`, `>`, and `&`.
- Tests use the authored fixtures only.

## Dependencies and licenses

OSV (`https://api.osv.dev/v1/query`, Maven ecosystem, 2026-11-23) returned no vulnerabilities for Picocli 4.7.7, sqlite-jdbc 3.54.1.0, jackson-databind 2.23.0, jackson-core 2.23.0, jackson-annotations 2.23, junit-jupiter 6.1.3, or junit-platform-launcher 6.1.3.

Runtime licenses are Apache-2.0. Test libraries are EPL-2.0. The project license is MIT. Versions are pinned in `build.gradle` and `gradle.lockfile`. See `NOTICE`.

No project source is native code. sqlite-jdbc ships its own prebuilt libraries. This repo does not compile them, so there is no address or undefined-behavior sanitizer job.

## Residual limitations

Coverage is the one observed journey and the two configured tables and sinks. The tool does not load an arbitrary application jar, and it does not talk to Postgres, Docker, or a live HTTP service. A difference outside `orders`, `reservations`, `shipping`, and `email` is outside the claim, and the report says so.
