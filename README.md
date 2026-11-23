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

Fixture provenance, checksums, and dependency licenses are in [fixtures/manifest.json](fixtures/manifest.json). Journeys and the seed are authored in this repo and are not fetched at run time.

## License

[MIT](LICENSE)
