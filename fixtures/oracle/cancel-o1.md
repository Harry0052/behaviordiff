# Oracle: cancel-o1 validation

Reference method: read the fixture by hand and apply `docs/data-contract.md`. No program output was copied into this file.

## Valid fixture `fixtures/journeys/cancel-o1.valid.json`

- Root fields are exactly `schemaVersion`, `name`, `seed`, `clock`, `actions`.
- `schemaVersion` is `1`.
- `name` is `cancel-o1` (lowercase identifier).
- `seed` is `seed-cancel-o1`, the only authored seed.
- `clock` is `2026-01-15T12:00:00Z`, the fixed demo clock.
- `actions` has length 1, which is inside 1..32.
- The only action id is `cancel-order`.
- The only action name is `cancel_order`, which is the only allowed name.
- `input.orderId` is `O-1`, which matches `O-<digits>`.
- No field contains `://`.
- The file is far below 65536 bytes.

Expected command result: exit 0, and stdout is exactly

```text
accepted fixture cancel-o1 seed seed-cancel-o1 actions 1
```

## Invalid fixture `fixtures/journeys/unknown-action.invalid.json`

The document matches the valid shape except `actions[0].name` is `teleport_order`.

Allowed names: `cancel_order` only. `teleport_order` is not in that set, so the file is rejected before any shop build runs.

Expected command result: exit 3, stderr begins with `UNKNOWN_ACTION:`, and the message names both `teleport_order` and `cancel_order`.

## Malformed fixture `fixtures/journeys/malformed.invalid.json`

The file is a single `{`, which is not a JSON value. Expected category: `MALFORMED_INPUT`. Exit 3.
