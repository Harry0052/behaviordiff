# BehaviorDiff

Compare database writes and in-process shipping and email effects for two checked-in builds of a demo shop. The builds share a seed, a fixed clock, and a deterministic id generator. Reports stay on the contract fields in [docs/data-contract.md](docs/data-contract.md).

This repository is the narrow MVP described in [docs/project-contract.md](docs/project-contract.md). It does not call a paid service, and it does not ship production records or credentials.

## Prerequisites

- Java 21 (the build refuses to download a JDK; use the JDK already on `PATH`)
- Git

Gradle is provided by the wrapper. No global Gradle or Maven install is required.

## Install and test

From a clean checkout:

```bash
./gradlew test --no-daemon
```

Install the command and validate the labeled fixtures. The installed script is the public entry point, so its exit code is the contract code. `./gradlew run` prints the same text and then reports a failed build whenever the app exits non-zero.

```bash
./gradlew installDist --quiet --no-daemon
./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/cancel-o1.valid.json
./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/unknown-action.invalid.json
```

The valid command exits 0 and prints:

```text
accepted fixture cancel-o1 seed seed-cancel-o1 actions 1
```

The invalid command exits 3 and prints an `UNKNOWN_ACTION` error that names `teleport_order`. The hand check is [fixtures/oracle/cancel-o1.md](fixtures/oracle/cancel-o1.md).

## Review a demo case

The sample journey cancels order `O-1`. Amounts are integer USD cents. Quantity is an item count.

```bash
./build/install/behaviordiff/bin/behaviordiff demo --case duplicate-cancel
```

Exit 1 means the builds differ. The text on stdout names the earliest changed step, `cancel-order`, the retained reservation `R-1`, and the shipping effect `ship-cancel:O-1` with counts 1 and 2. The cause is the shared idempotency key. Units and the coverage limitation are printed with that finding.

The machine-readable export defaults to `out/duplicate-cancel.json`. It is canonical JSON and is separate from the text. The finding's `actionId` is the action id in [fixtures/journeys/cancel-o1.valid.json](fixtures/journeys/cancel-o1.valid.json). The hand calculation is [fixtures/oracle/duplicate-cancel.md](fixtures/oracle/duplicate-cancel.md).

Compare the good build with itself:

```bash
./build/install/behaviordiff/bin/behaviordiff demo --case identical-control
```

Exit 0 and status `identical`.

Approve one documented field. This does not approve any other change:

```bash
./build/install/behaviordiff/bin/behaviordiff demo --case volatile-destination --approve orders.destination
```

Exit 0 and status `approved`. The report still shows destination `dock-b` and omits volatile request ids.

A bad case stays readable and does not claim coverage:

```bash
./build/install/behaviordiff/bin/behaviordiff demo --case not-a-case
./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/malformed.invalid.json
./build/install/behaviordiff/bin/behaviordiff demo --case partial-tables
```

`not-a-case` and the malformed file exit 3. `partial-tables` exits 3 with `UNOBSERVED_TABLE` because `reservations` was not observed. The exported JSON has `"coverage": null`.

Exit codes: 0 identical or fully approved, 1 unapproved changes, 2 incomplete observation, 3 rejected input or setup. `--help` lists `validate`, `compare`, and `demo`.

Fixture provenance, checksums, and dependency licenses are in [fixtures/manifest.json](fixtures/manifest.json). Journeys and the seed are authored in this repo and are not fetched at run time.

## License

[MIT](LICENSE)
