# Data contract

Schema version `1` for journeys and reports. This file is the bound the validator and, from P03 onward, the comparator implement. Units and limits are normative.

## Journey input

UTF-8 JSON object. Unknown fields are rejected. Duplicate keys are rejected.

| Field | Type | Bound |
| --- | --- | --- |
| `schemaVersion` | string | exactly `1` |
| `name` | string | `^[a-z][a-z0-9-]{0,63}$` |
| `seed` | string | exactly `seed-cancel-o1` |
| `clock` | string | exactly `2026-01-15T12:00:00Z` |
| `actions` | array | length 1..32, unique `id` values |

Each action:

| Field | Type | Bound |
| --- | --- | --- |
| `id` | string | `^[a-z][a-z0-9-]{0,63}$` |
| `name` | string | `cancel_order` |
| `input.orderId` | string | `^O-[0-9]{1,6}$` |

File size limit: 65536 bytes, enforced before the parser trusts the document. Each string is 1..256 characters. A string containing `://` is an external endpoint and is rejected with `EXTERNAL_SINK`.

`amount_cents` is not an input. It lives on the seeded order and is measured in integer USD cents. `quantity` is an integer item count.

## Seed `seed-cancel-o1`

Hand-authored. No remote fetch and no PRNG.

| Table | Primary key | Row |
| --- | --- | --- |
| `orders` | `order_id` | `O-1`, status `placed`, `amount_cents` 2500, destination `dock-a`, `request_id` `req-seed`, `note` SQL NULL |
| `reservations` | `reservation_id` | `R-1`, order `O-1`, sku `SKU-1`, quantity 1 |

`orders.request_id` is volatile and sensitive. Reports omit it. A difference in that column alone is not a change.

## Report output

UTF-8 canonical JSON: object keys sorted, no insignificant whitespace, U+003C U+003E and U+0026 escaped as `\u003c` `\u003e` `\u0026`, one trailing LF.

Successful statuses are `identical`, `changes`, and `approved`. They carry:

- `schemaVersion`, `journey`, `seed`, `clock`, `leftBuild`, `rightBuild`, `status`
- `earliestChangedStep` (string or null)
- `responseChanged` (boolean)
- `approvals` (array of `table.field` strings)
- `units.amountCents` = `USD cents`, `units.quantity` = `item count`
- `rowChanges` sorted by action id, table, key. Each entry has `leftOp` and `rightOp` of `insert`, `update`, `delete`, or `none`, plus `leftBefore`, `leftAfter`, `rightBefore`, `rightAfter`. A missing row is JSON null. Present rows use camelCase contract fields and omit `requestId`. `changedFields` lists the non-volatile fields that differ. `*` means the row appeared or disappeared and cannot be approved as a field change. `approved` is true only when every changed field was named with `--approve`.
- `effectChanges` sorted by action id, sink, effect type, business key. Counts are the observed multiplicity. `cause` is one of `duplicate-idempotency-key`, `missing-effect`, `extra-effect`, `payload`, `null-vs-absent`.
- `coverage.steps`, `coverage.tables`, `coverage.sinks`, `coverage.seed`, and `coverage.limitation`.

`rejected` and `incomplete` reports are `{schemaVersion, status, category, message, coverage: null}`. They are not successful coverage.

## Error categories and exit codes

| Category | When | Exit |
| --- | --- | --- |
| `MALFORMED_INPUT` | Syntax, types, missing or unknown fields, clock, identifier shape | 3 |
| `OVERSIZED_INPUT` | File or buffer above 65536 bytes | 3 |
| `UNKNOWN_ACTION` | Action name outside `cancel_order` | 3 |
| `UNKNOWN_SEED` | Seed other than `seed-cancel-o1` | 3 |
| `UNKNOWN_BUILD` | Build id is not checked in | 3 |
| `UNOBSERVED_TABLE` | A run does not observe both `orders` and `reservations` | 3 |
| `EXTERNAL_SINK` | A URL or a sink other than in-process `shipping` and `email` | 3 |
| `INCOMPLETE_EVIDENCE` | Adapter failure, unknown mapping, missing snapshot, or truncation after setup | 2 |

Compare exit codes: `0` identical or fully approved, `1` at least one unapproved change, `2` incomplete, `3` rejected. Validation uses `0` and `3` only.

## Null and absence

SQL NULL is JSON null inside a present row. A deleted row is a null before/after value, not a row of null fields. An effect payload key that is present and null differs from a key that is absent (`null-vs-absent`).
