-- Load this into Postgres, SQLite, or H2, then work through Module 5
-- of the Phase 0 lesson against it.

CREATE TABLE customers (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    customer_id INT REFERENCES customers(id),
    order_date DATE
);

CREATE TABLE order_items (
    id INT PRIMARY KEY,
    order_id INT REFERENCES orders(id),
    product_name VARCHAR(100),
    quantity INT,
    unit_price DECIMAL(10,2)
);

INSERT INTO customers VALUES (1, 'Alice', 'alice@example.com');
INSERT INTO customers VALUES (2, 'Bob', 'bob@example.com');
INSERT INTO customers VALUES (3, 'Carla', 'carla@example.com'); -- deliberately: no orders

INSERT INTO orders VALUES (1, 1, '2026-01-05');
INSERT INTO orders VALUES (2, 1, '2026-02-14');
INSERT INTO orders VALUES (3, 2, '2026-03-01');

INSERT INTO order_items VALUES (1, 1, 'Widget', 2, 9.99);
INSERT INTO order_items VALUES (2, 1, 'Gadget', 1, 19.99);
INSERT INTO order_items VALUES (3, 2, 'Widget', 5, 9.99);
INSERT INTO order_items VALUES (4, 3, 'Gizmo', 1, 49.99);
