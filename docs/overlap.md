# Overlap review

Access date for every primary source below: 2026-11-23.

Decision: **proceed**, with a narrow local MVP. No surveyed workflow already produces the acceptance artifact: one step-aligned report of row operations and outbound-effect multiplicity for two checked-in builds, on disposable SQLite, with in-process sinks only.

ShopOutage, named in the playbook as a neighboring checkout-recovery contract, did not appear as a public repository in the search on this date. It is a boundary, not a candidate to extend. BehaviorDiff compares observed writes and effects of two builds on one journey. It does not inject outages or assert a recovery contract.

## Shared fixture and metric

The fixture is synthetic and authored for this review. Seed `seed-cancel-o1` inserts order `O-1` (`placed`, 2500 cents, destination `dock-a`) and reservation `R-1`. The only step is `cancel-order` / `cancel_order`. Both sides return HTTP 200 and body `{"orderId":"O-1","status":"canceled"}`.

Held constant across comparators:

| Item | Value |
| --- | --- |
| Input | `docs/research/p01_probe.py` fixture `cancel-O-1` |
| Expected artifact | Earliest step, before/after reservation state, effect multiplicity, and a destination change with `request_id` removed |
| Acceptance metric | Hidden row deletion is detected when the response and the effect count agree; duplicate key `ship-cancel:O-1` stays 1 versus 2; identical rerun change count is 0 |

Command and host evidence:

```text
python3 docs/research/p01_probe.py
```

Python 3.12.3, stdlib `sqlite3` (SQLite 3.45.1). Result ends in `PROBE_OK`. Maven and Docker were not installed, so Karate, WireMock, goldenbox, and abheeshtroy/BehaviorDiff were not executed. Those rows are documentation evidence plus this local model of the comparison their docs describe.

Observed probe results:

| Case | Response and effect count | Journal key counts | Step-aligned rows and effects |
| --- | --- | --- | --- |
| Hidden reservation delete | No finding. Counts are both 1 and the response matches. | No count difference. Does not see the row. | Change count 1. Reservation `R-1` delete. Step `cancel-order`. |
| Duplicate shipping cancel, reservation retained | Finding, because counts are 1 and 2. Does not name the row or the shared key. | Counts differ for `ship-cancel:O-1`. Does not state a shared-key cause and does not see the row. | Effect 1 versus 2 for `ship-cancel:O-1`, plus the retained reservation. |
| Volatile `request_id` and destination `dock-b` | No finding. | No finding. | Destination `dock-a` to `dock-b` only. `request_id` absent from the diff. |
| Identical control | No finding. | No finding. | Change count 0. Two encodings of the report were byte-identical. |

## Capability matrix

| Tool | Primary URL | Version or commit | License | Dataset terms | Evidence | What already matches | Gap for this acceptance case |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Karate database testing | https://docs.karatelabs.io/advanced/database-testing/ and https://github.com/karatelabs/karate | v2.1.4, commit `d90cc1b49eff345808c09faac5973a9cd9eb6ecb` | MIT | User-supplied JDBC data. Docs warn not to put credentials in feature files. | Documentation. JAR not run. | Java interop can query rows and `match` a hand-written JSON oracle beside an HTTP status. | The match is a second oracle per build. Docs do not emit one typed two-build delta, effect multiplicity, or a rejection of an external sink before coverage. The local probe's response-and-count check missed the hidden delete. |
| WireMock verification | https://wiremock.org/docs/verifying/ and https://github.com/wiremock/wiremock | 3.13.2 (Maven coordinate documented on https://wiremock.org/docs/download-and-installation/). OpenAPI text in tree `32e3995e` says API version 3.12.1. | Apache-2.0 | Request journal of whatever the test double received. No dataset shipped for this case. | Documentation. JAR not run. | `verify(exactly(n), ...)` and `POST /__admin/requests/count` record multiplicity of matching HTTP calls. The journal can be fetched. | The journal does not snapshot SQLite. A matching count accepts the hidden deletion. Duplicate calls are not joined to the reservation row of the same step, and an idempotency key is not a separate cause. WireMock is an HTTP test double, so using it as the shipping sink would be an external process rather than an in-process sink that setup can refuse. |
| abheeshtroy/BehaviorDiff | https://github.com/abheeshtroy/BehaviorDiff | commit `8f2e2e0ab5e16cd525fc9483a5e35bc3399eff22` (2026-11-06) | MIT | Demo traffic described as application workflows. Postgres DSNs are user-supplied. | Documentation, including the README example removed in that commit (still visible in the commit patch): step-scoped HTTP findings and Postgres insert/delete findings. Not executed. Docker is absent. | Closest product. Two versions, separate databases, mutation deltas, outbound HTTP mocks, step id on findings. | App database is Postgres, started as Docker HTTP services. SQLite in that repo is the tool's own run store. Noise suppression is repeated measurement, not a registered volatile field. No documented gate that rejects an unknown action, an unobserved required table, and an external sink before coverage. No byte-stable canonical JSON contract for an in-process shop. |
| goldenbox | https://github.com/mustard-mh/goldenbox | commit `9243a448812bbef39afbb067d60844c50a4574cc` (2026-07-06) | Apache-2.0 | Reviewer-approved YAML goldens. | Documentation. Not executed. | Per-step store diffs, including cell-level updates, and byte-reproducible goldens. | One build against a hand-reviewed golden. That is the second outcome file this hypothesis is trying to avoid. Stores are MySQL, Postgres, or Redis containers, not two SQLite builds and in-process effect journals. |
| Careo | https://github.com/AnvitDevadiga/careo | commit `9bdbf43d266cf8b7ec48d50bd821eeb295a0e7e5` (2026-11-02, message "Merge Careo 1.0 release"). README cites engine notes for 0.2.0 and 1.0.0. | MIT | Local fixtures. README says trusted programs only and loopback HTTP unless opted in. | Documentation. | Two implementations, verified SQLite reset, per-step state, explicit normalization, failures kept distinct from matches. | Searches for a counterexample with Hypothesis. It does not replay one declared shop journey through in-process shipping and email sinks, and it does not report idempotency-key multiplicity joined to a reservation key. |
| ordeal | https://github.com/teilomillet/ordeal and https://docs.byordeal.com/guides/differential-state-and-effects/ | commit `4bdb3b7f7869c89d32a8417d009a4ccc3b5fa270` (v0.3.43, 2026-07-11) | Apache-2.0 | Generated inputs over the caller's Python objects. | Documentation. | Selected side effects can be part of a differential result, and unselected effects stay outside the claim. | Object and pytest findings, not a relational snapshot of two tables plus ordered effect journals for a fixed journey. |
| traffic-diff | https://github.com/Jiangxianze/traffic-diff | README retrieved 2026-11-23. Release tag was not required for the gap. | Not re-fetched beyond the repository page. Treat license as unresolved. | Sanitized HAR/JSONL tapes. README says replay can cause side effects and defaults to GET, HEAD, and OPTIONS. | Documentation. | Replays one tape against two origins and writes a JSON result with ignore rules. | Response and latency gates only. No SQLite row diff and no effect-journal multiplicity. Unsafe to point at a live sink. |
| BERT (paper) | https://taoxie.cs.illinois.edu/publications/woda08.pdf | WODA 2008 | Publication, not a reusable license for code. | None. | Documentation. | Runs generated tests on two versions and logs field state. | In-memory object fields and return values. No relational tables, no outbound effect journal, no current artifact to install. |

Query notes:

- `differential integration testing database state outbound side effects` led to abheeshtroy/BehaviorDiff, Careo, goldenbox, and the Speedscale mock-lab differential-testing write-up (recorded HTTP only).
- `compare HTTP request journals SQLite database test version regression` led to harlite (`harlite diff before.db after.db` compares captured HAR rows, not application tables) and traffic-diff / the PyPI `traffic-diff` package (response replay).
- `behavioral regression testing observable side effects two builds relational state` led to the ordeal guides and the BERT paper.

## Remaining uncertainties

- abheeshtroy/BehaviorDiff might already flag a duplicate HTTP call beside a Postgres row if a manifest were written. That run was not observed here because Docker is not installed. The README that was retrieved does not describe idempotency-key multiplicity, registered volatile fields, or a pre-coverage rejection of external sinks.
- WireMock's fetched journal bodies could show two identical idempotency keys to a human. The verified API that was read compares counts and request patterns. It still cannot see `reservations`.
- traffic-diff's license was not opened from the repository LICENSE file in this pass.
- No public project named ShopOutage was found.

## Why a separate repository still earns the MVP

Karate can assert the hidden delete only if someone writes the expected rows. WireMock can count the duplicate only as HTTP traffic. goldenbox can pin one build's delta only against a reviewed file. Careo can search two SQLite services, but it does not ship this journey, these sinks, or this report. abheeshtroy/BehaviorDiff is the nearest product and already has a step-scoped database finding, yet meeting this acceptance case there would replace its Postgres/Docker/HTTP-mock setup with checked-in in-process builds, a closed action vocabulary, and a canonical JSON contract. That is a different product surface, not a small contribution.

The local probe is the hypothesis worth testing: the same seed, two builds, and one comparator caught the row deletion that response-and-count missed, kept effect multiplicity for a shared key, ignored `request_id`, and stayed byte-stable on an identical control.

Narrowing that stands for the rest of the build:

- Java 21 CLI. Picocli, SQLite JDBC, and JUnit from Maven Central. No Docker, no Postgres, no paid service, no model calls.
- Two checked-in demo-shop builds per case, in-memory SQLite, fixed clock, deterministic ids.
- In-process sinks `shipping` and `email` only. An external sink fails setup.
- Two tables, `orders` and `reservations`. Reports list contract fields only.
- Do not build a dashboard, an AI classifier, or a general HTTP replay proxy.

Provenance path: fixtures are authored in this repo under MIT. Runtime dependencies resolve from Maven Central. Versions current on 2026-11-23, to be pinned in the build: `info.picocli:picocli:4.7.7` (Apache-2.0), `org.xerial:sqlite-jdbc:3.54.1.0` (Apache-2.0, bundles public-domain SQLite), `com.fasterxml.jackson.core:jackson-databind:2.23.0` (Apache-2.0), `org.junit.jupiter:junit-jupiter:6.1.3` (EPL-2.0, test scope). Gradle 9.9.2 is the wrapper candidate.
