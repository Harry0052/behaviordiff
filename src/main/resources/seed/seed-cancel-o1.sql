CREATE TABLE orders (
  order_id TEXT PRIMARY KEY,
  status TEXT NOT NULL,
  amount_cents INTEGER NOT NULL,
  destination TEXT NOT NULL,
  request_id TEXT NOT NULL,
  note TEXT
);

CREATE TABLE reservations (
  reservation_id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL,
  sku TEXT NOT NULL,
  quantity INTEGER NOT NULL
);

INSERT INTO orders (order_id, status, amount_cents, destination, request_id, note)
VALUES ('O-1', 'placed', 2500, 'dock-a', 'req-seed', NULL);

INSERT INTO reservations (reservation_id, order_id, sku, quantity)
VALUES ('R-1', 'O-1', 'SKU-1', 1);
