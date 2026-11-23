#!/usr/bin/env python3
"""Smallest local comparison for the P01 acceptance fixture.

Holds one cancellation input constant and scores three comparators:
response-and-count (what a Karate status match plus a WireMock exact count
can see), a business-key journal count, and a step-aligned row-and-effect
delta. Does not execute Karate, WireMock, or any third-party product.
"""

import hashlib
import json
import sqlite3

FIXTURE = {
    "schema": "behaviordiff.probe/v1",
    "seed": "seed-cancel-o1",
    "journey": "cancel-O-1",
    "step": {"id": "cancel-order", "action": "cancel_order", "orderId": "O-1"},
    "response": {"httpStatus": 200, "body": {"orderId": "O-1", "status": "canceled"}},
}


def fresh():
    con = sqlite3.connect(":memory:")
    con.executescript(
        """
        CREATE TABLE orders (
          order_id TEXT PRIMARY KEY,
          status TEXT NOT NULL,
          amount_cents INTEGER NOT NULL,
          destination TEXT NOT NULL,
          request_id TEXT NOT NULL
        );
        CREATE TABLE reservations (
          reservation_id TEXT PRIMARY KEY,
          order_id TEXT NOT NULL,
          sku TEXT NOT NULL,
          quantity INTEGER NOT NULL
        );
        INSERT INTO orders VALUES ('O-1', 'placed', 2500, 'dock-a', 'req-seed');
        INSERT INTO reservations VALUES ('R-1', 'O-1', 'SKU-1', 1);
        """
    )
    return con


def rows(con, table):
    cur = con.execute(f"SELECT * FROM {table} ORDER BY 1")
    cols = [d[0] for d in cur.description]
    return [dict(zip(cols, row)) for row in cur.fetchall()]


def apply_good(con):
    con.execute(
        "UPDATE orders SET status='canceled', request_id='req-good' WHERE order_id='O-1'"
    )
    con.execute("DELETE FROM reservations WHERE reservation_id='R-1'")
    return [
        {
            "sink": "shipping",
            "effectType": "cancel_shipment",
            "businessKey": "ship-cancel:O-1",
            "actionId": "cancel-order",
            "sequence": 1,
            "requestId": "req-good",
        }
    ]


def apply_keep_reservation(con):
    con.execute(
        "UPDATE orders SET status='canceled', request_id='req-keep' WHERE order_id='O-1'"
    )
    return [
        {
            "sink": "email",
            "effectType": "order_canceled",
            "businessKey": "email:O-1:canceled",
            "actionId": "cancel-order",
            "sequence": 1,
            "requestId": "req-keep",
        }
    ]


def apply_hidden_delete(con):
    con.execute(
        "UPDATE orders SET status='canceled', request_id='req-hidden' WHERE order_id='O-1'"
    )
    con.execute("DELETE FROM reservations WHERE reservation_id='R-1'")
    return [
        {
            "sink": "email",
            "effectType": "order_canceled",
            "businessKey": "email:O-1:canceled",
            "actionId": "cancel-order",
            "sequence": 1,
            "requestId": "req-hidden",
        }
    ]


def apply_duplicate(con):
    con.execute(
        "UPDATE orders SET status='canceled', request_id='req-dup' WHERE order_id='O-1'"
    )
    return [
        {
            "sink": "shipping",
            "effectType": "cancel_shipment",
            "businessKey": "ship-cancel:O-1",
            "actionId": "cancel-order",
            "sequence": 1,
            "requestId": "req-dup-a",
        },
        {
            "sink": "shipping",
            "effectType": "cancel_shipment",
            "businessKey": "ship-cancel:O-1",
            "actionId": "cancel-order",
            "sequence": 2,
            "requestId": "req-dup-b",
        },
    ]


def apply_destination(con):
    con.execute(
        "UPDATE orders SET status='canceled', destination='dock-b', request_id='req-dest' "
        "WHERE order_id='O-1'"
    )
    con.execute("DELETE FROM reservations WHERE reservation_id='R-1'")
    return [
        {
            "sink": "shipping",
            "effectType": "cancel_shipment",
            "businessKey": "ship-cancel:O-1",
            "actionId": "cancel-order",
            "sequence": 1,
            "requestId": "req-dest",
        }
    ]


def snapshot(apply):
    con = fresh()
    effects = apply(con)
    return {
        "response": FIXTURE["response"],
        "orders": rows(con, "orders"),
        "reservations": rows(con, "reservations"),
        "effects": effects,
    }


def response_and_count(left, right):
    same_response = left["response"] == right["response"]
    same_count = len(left["effects"]) == len(right["effects"])
    return {
        "sameResponse": same_response,
        "sameEffectCount": same_count,
        "finding": None if same_response and same_count else "response-or-count-differ",
    }


def journal_multiset(left, right):
    def bag(effects):
        counts = {}
        for effect in effects:
            counts[effect["businessKey"]] = counts.get(effect["businessKey"], 0) + 1
        return counts

    left_bag, right_bag = bag(left["effects"]), bag(right["effects"])
    return {
        "left": left_bag,
        "right": right_bag,
        "countsDiffer": left_bag != right_bag,
        "seesRowDeletion": False,
        "statesSharedIdempotencyCause": False,
    }


def step_aligned(left, right):
    def strip_order(row):
        copied = dict(row)
        copied.pop("request_id", None)
        return copied

    order_diffs = []
    left_orders = {row["order_id"]: strip_order(row) for row in left["orders"]}
    right_orders = {row["order_id"]: strip_order(row) for row in right["orders"]}
    for key in sorted(set(left_orders) | set(right_orders)):
        if left_orders.get(key) != right_orders.get(key):
            order_diffs.append(
                {"key": key, "before": left_orders.get(key), "after": right_orders.get(key)}
            )

    left_rows = {row["reservation_id"]: row for row in left["reservations"]}
    right_rows = {row["reservation_id"]: row for row in right["reservations"]}
    row_ops = []
    for key in sorted(set(left_rows) | set(right_rows)):
        if key not in right_rows:
            row_ops.append(
                {
                    "op": "delete",
                    "table": "reservations",
                    "key": key,
                    "before": left_rows[key],
                    "after": None,
                }
            )
        elif key not in left_rows:
            row_ops.append(
                {
                    "op": "insert",
                    "table": "reservations",
                    "key": key,
                    "before": None,
                    "after": right_rows[key],
                }
            )
        elif left_rows[key] != right_rows[key]:
            row_ops.append(
                {
                    "op": "update",
                    "table": "reservations",
                    "key": key,
                    "before": left_rows[key],
                    "after": right_rows[key],
                }
            )

    def bag(effects):
        counts = {}
        for effect in effects:
            identity = (effect["sink"], effect["effectType"], effect["businessKey"])
            counts[identity] = counts.get(identity, 0) + 1
        return counts

    left_bag, right_bag = bag(left["effects"]), bag(right["effects"])
    effect_diffs = []
    for key in sorted(set(left_bag) | set(right_bag)):
        if left_bag.get(key, 0) != right_bag.get(key, 0):
            effect_diffs.append(
                {
                    "sink": key[0],
                    "effectType": key[1],
                    "businessKey": key[2],
                    "leftCount": left_bag.get(key, 0),
                    "rightCount": right_bag.get(key, 0),
                    "cause": "shared-idempotency-key",
                }
            )
    changed = bool(order_diffs or row_ops or effect_diffs)
    return {
        "earliestStep": FIXTURE["step"]["id"] if changed else None,
        "orderDiffs": order_diffs,
        "reservationOps": row_ops,
        "effectDiffs": effect_diffs,
        "changeCount": len(order_diffs) + len(row_ops) + len(effect_diffs),
    }


def main():
    good = snapshot(apply_good)
    cases = {
        "hidden-deletion": (snapshot(apply_keep_reservation), snapshot(apply_hidden_delete)),
        "duplicate-cancel": (good, snapshot(apply_duplicate)),
        "volatile-and-destination": (good, snapshot(apply_destination)),
        "identical-control": (good, snapshot(apply_good)),
    }
    print("SHARED_FIXTURE", json.dumps(FIXTURE, sort_keys=True))
    for name, (left, right) in cases.items():
        aligned = step_aligned(left, right)
        encoded = json.dumps(aligned, sort_keys=True, separators=(",", ":"))
        again = json.dumps(step_aligned(left, right), sort_keys=True, separators=(",", ":"))
        print("---", name)
        print("response_and_count", json.dumps(response_and_count(left, right), sort_keys=True))
        print("journal", json.dumps(journal_multiset(left, right), sort_keys=True))
        print("step_aligned", encoded)
        print("byte_stable", encoded == again, "sha256", hashlib.sha256(encoded.encode()).hexdigest())

    hidden_left, hidden_right = cases["hidden-deletion"]
    dup_left, dup_right = cases["duplicate-cancel"]
    dest_left, dest_right = cases["volatile-and-destination"]
    dup_effects = step_aligned(dup_left, dup_right)["effectDiffs"][0]
    dest_orders = step_aligned(dest_left, dest_right)["orderDiffs"]
    metric = {
        "hidden_deletion_caught_by_response_count": response_and_count(hidden_left, hidden_right)["finding"]
        is not None,
        "hidden_deletion_caught_by_step_aligned": step_aligned(hidden_left, hidden_right)["changeCount"] > 0,
        "duplicate_multiplicity_preserved": dup_effects["leftCount"] == 1 and dup_effects["rightCount"] == 2,
        "duplicate_shares_key": dup_effects["businessKey"] == "ship-cancel:O-1",
        "request_id_ignored": "request_id" not in json.dumps(dest_orders),
        "destination_retained": any(
            (diff.get("after") or {}).get("destination") == "dock-b" for diff in dest_orders
        ),
        "identical_change_count": step_aligned(*cases["identical-control"])["changeCount"],
    }
    print("METRIC", json.dumps(metric, sort_keys=True))
    assert metric == {
        "destination_retained": True,
        "duplicate_multiplicity_preserved": True,
        "duplicate_shares_key": True,
        "hidden_deletion_caught_by_response_count": False,
        "hidden_deletion_caught_by_step_aligned": True,
        "identical_change_count": 0,
        "request_id_ignored": True,
    }
    print("PROBE_OK")


if __name__ == "__main__":
    main()
