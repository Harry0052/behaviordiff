# Project contract

Status: accepted for implementation after the P01 overlap review. Decision recorded in `docs/overlap.md`: proceed with the narrow local MVP.

## User and task

Application engineers review what two local builds of a demo shop write and emit during one representative journey.

The first task is: cancel synthetic order `O-1` on build `good` and build `duplicate-cancel`, then read a typed report. Both builds return HTTP 200 and set the order status to `canceled`. `duplicate-cancel` emits a second shipping cancellation with the same idempotency key and leaves reservation `R-1` in place. The report names step `cancel-order`, the retained reservation, and effect multiplicity 1 versus 2. Comparing `good` with itself reports no changes.

## Minimum dependencies

| Dependency | Role | Provenance | License |
| --- | --- | --- | --- |
| Java 21 | Runtime and test toolchain | Local JDK or the CI Temurin distribution | GPL-2.0 with Classpath Exception for OpenJDK builds |
| Gradle wrapper | Build | services.gradle.org, wrapper committed | Apache-2.0 |
| Picocli 4.7.7 | CLI | Maven Central `info.picocli:picocli` | Apache-2.0 |
| sqlite-jdbc 3.54.1.0 | Disposable databases | Maven Central `org.xerial:sqlite-jdbc` | Apache-2.0; bundled SQLite is public domain |
| jackson-databind 2.23.0 | JSON contracts and canonical reports | Maven Central | Apache-2.0 |
| JUnit Jupiter 6.1.3 | Tests only | Maven Central | EPL-2.0 |

No remote fixture is fetched at run time. No service credentials are required.

## Surfaces

- Journey file, schema version `1`: seed id, fixed clock, ordered actions with ids, names, and typed inputs.
- Allowed action in this release: `cancel_order` with `orderId`.
- Tables: `orders` (primary key `order_id`) and `reservations` (primary key `reservation_id`).
- Order fields: `status`, `amount_cents` (integer USD cents), `destination`, `request_id` (volatile and sensitive), `note` (nullable text).
- Reservation fields: `order_id`, `sku`, `quantity` (integer item count).
- Sinks: in-process `shipping` and `email` only.
- Effect record: sink, effect type, stable business key, action id, sequence, and a payload limited to contract fields.
- Report, schema version `1`: status, earliest changed step, row before/after values, effect counts and cause, coverage, units, and limitation.

## One measurable acceptance case

Case `duplicate-cancel`.

Setup: seed `seed-cancel-o1`, clock `2026-01-15T12:00:00Z`, action `cancel-order`.

Pass condition, all of it:

- HTTP status and response body are equal.
- Earliest changed step is `cancel-order`.
- Reservation `R-1` is absent after the step on `good` and present after the step on `duplicate-cancel`.
- Shipping effect `cancel_shipment` with business key `ship-cancel:O-1` has count 1 on `good` and count 2 on `duplicate-cancel`, and the cause records the shared key.
- A second run of `good` against `good` has an empty change list.
- Two runs of the canonical JSON report are byte-identical.

The broader suite (hidden deletion, volatile request id, amount, missing effect, null versus absent, and the rejection gates) is required by the playbook and is in scope for later phases. This case is the one that decides whether the comparator is worth keeping.

## Out of scope

Production databases, live HTTP endpoints, recorded customer traffic, additional tables, a web UI, and automatic approval of a diff. Field approval exists only when the user names the exact field.
