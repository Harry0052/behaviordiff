# Oracle: duplicate-cancel

Reference method: apply seed `seed-cancel-o1` by hand, then apply each build's rules. This file was written before comparing it with program output.

Seed before either build runs:

- Order `O-1`: status `placed`, amount 2500 USD cents, destination `dock-a`, note NULL.
- Reservation `R-1`: order `O-1`, sku `SKU-1`, quantity 1.

Both builds answer HTTP 200 with body `{"orderId":"O-1","status":"canceled"}`.

## Build `good`

- Set order status to `canceled`.
- Delete reservation `R-1`.
- Emit one shipping effect, type `cancel_shipment`, business key `ship-cancel:O-1`, sequence 1.

## Build `duplicate-cancel`

- Set order status to `canceled`.
- Leave reservation `R-1` in place.
- Emit two shipping effects, both type `cancel_shipment`, both business key `ship-cancel:O-1`, sequences 1 and 2.

## Expected difference

- Responses match, so a response-only check would not explain the failure.
- Earliest changed step is `cancel-order`, the only step.
- Reservation `R-1` is a delete on `good` and unchanged on `duplicate-cancel`. The retained row is `R-1`, order `O-1`, sku `SKU-1`, quantity 1.
- Effect multiplicity for `ship-cancel:O-1` is 1 versus 2. The cause is the shared idempotency key, not two different keys.
- Order `request_id` values differ by build and are volatile. They are not a finding.
- Comparing `good` with `good` has no row changes and no effect changes.

## Other seeded mutations

| Left | Right | Expected |
| --- | --- | --- |
| `retain-reservation` | `drop-reservation` | Same response, same single email `email:O-1:canceled`, reservation `R-1` deleted only on the right |
| `good` | `reroute` | Destination `dock-a` versus `dock-b`. Amount stays 2500 cents. No `requestId` in the report |
| `good` | `reprice` | Amount 2500 versus 1999 USD cents |
| `email-cancel` | `silent-cancel` | Email effect missing on the right, count 1 versus 0 |
| `null-template` | `absent-template` | Same email key, count 1 versus 1, cause `null-vs-absent` because `template` is null on the left and absent on the right |
