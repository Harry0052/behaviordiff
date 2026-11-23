# Contributing

BehaviorDiff is a small Java 21 command line tool. Changes should keep the
demo runnable with `./gradlew installDist` and the checked-in fixtures.

1. Fork the repository and create a branch.
2. Run `./gradlew check --no-daemon`. That formats the Java sources with
   Spotless, compiles, and runs the tests.
3. If you change a fixture, update `fixtures/manifest.json` and the hand
   oracle under `fixtures/oracle/`.
4. Do not add credentials, production records, or personal information.
5. Open a pull request. The CI workflow runs the same check and the fixture
   demo without secrets.

The supported runtime is Java 21. The Gradle wrapper downloads Gradle 9.9.2
on a cold build. The demo itself does not fetch data.
