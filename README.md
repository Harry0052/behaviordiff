# BehaviorDiff

Compare what two local builds of a demo shop write and emit during one
synthetic order cancellation. Each build gets a fresh SQLite database from the
same seed, a fixed clock, and a deterministic id generator. Shipping and email
are in-process sinks. The report lists the earliest changed step, the before
and after row values, and missing or duplicate effects.

There is no separate hardware, model, or real-data demo. The fixture demo is
the product demo.

## Supported platform

Tested on Linux x86_64 with Java 21. You need:

- Java 21 on `PATH`. The build does not download a JDK.
- Git

The Gradle wrapper downloads Gradle 9.9.2 from `services.gradle.org` the first
time. After that, the demo does not fetch remote data. sqlite-jdbc brings its
own SQLite library.

## Install

```bash
git clone https://github.com/Harry0052/behaviordiff.git
cd behaviordiff
./gradlew installDist --no-daemon
```

## Demo

```bash
./build/install/behaviordiff/bin/behaviordiff demo --case duplicate-cancel
```

Exit code 1 is the expected result: the builds differ. The text names step
`cancel-order`, reservation `R-1` retained by `duplicate-cancel`, and shipping
effect `ship-cancel:O-1` with counts 1 and 2. Amounts are integer USD cents.
Quantity is an item count. The limitation printed with the finding is the
coverage limit: only this journey and the configured tables and sinks.

The canonical JSON report is written to `out/duplicate-cancel.json`. It is
separate from the text. A recorded copy is
[docs/examples/duplicate-cancel.json](docs/examples/duplicate-cancel.json).
The hand calculation is
[fixtures/oracle/duplicate-cancel.md](fixtures/oracle/duplicate-cancel.md).

The identical control exits 0:

```bash
./build/install/behaviordiff/bin/behaviordiff demo --case identical-control
```

Its report is `out/identical-control.json` and its status is `identical`.

## Other checks

```bash
./gradlew check --no-daemon
```

`check` runs Spotless, the compiler, and the tests. Validation notes, the
measured runtime, and the six-case result are in
[docs/validation.md](docs/validation.md).

Approve one documented field without hiding anything else:

```bash
./build/install/behaviordiff/bin/behaviordiff demo \
  --case volatile-destination --approve orders.destination
```

A bad fixture or an incomplete setup does not claim coverage:

```bash
./build/install/behaviordiff/bin/behaviordiff demo --case not-a-case
./build/install/behaviordiff/bin/behaviordiff validate fixtures/journeys/malformed.invalid.json
./build/install/behaviordiff/bin/behaviordiff demo --case partial-tables
```

Exit codes: 0 identical or fully approved, 1 unapproved changes, 2 incomplete
observation, 3 rejected input or setup. `--help` lists `validate`, `compare`,
and `demo`.

## Fixtures

Journeys, the seed, and the oracles are authored in this repository under the
MIT license. They are synthetic. They contain no production records, credentials,
or personal information. Checksums and dependency coordinates are in
[fixtures/manifest.json](fixtures/manifest.json). The field contract is
[docs/data-contract.md](docs/data-contract.md).

## Package

```bash
./gradlew distZip --no-daemon
```

The zip is `build/distributions/behaviordiff-0.1.0.zip`. It contains the
application jar, including the seed SQL, and the runtime jars. It does not
contain test reports or the Gradle cache.

## License and attribution

[MIT](LICENSE). Third-party notices are in [NOTICE](NOTICE).

## Contributing and security

See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md).
Version history is in [CHANGELOG.md](CHANGELOG.md).
