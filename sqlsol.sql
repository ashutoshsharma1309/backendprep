SELECT * FROM orders WHERE customer_id = 1;

SELECT o.id, o.order_date, oi.product_name, oi.quantity, oi.unit_price
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
WHERE o.customer_id = 1;

SELECT c.name, SUM(oi.quantity * oi.unit_price) AS total_spent
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.name;

SELECT c.name
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.id IS NULL;

SELECT c.name, COUNT(o.id) AS order_count
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.name
ORDER BY order_count DESC;

SELECT oi.product_name, SUM(oi.quantity) AS units_sold
FROM order_items oi
GROUP BY oi.product_name
HAVING SUM(oi.quantity) > 1;

INSERT INTO customers (id, name, email) VALUES (4, 'Dan', 'dan@example.com');
INSERT INTO orders (id, customer_id, order_date) VALUES (4, 4, '2026-04-02');
INSERT INTO order_items (id, order_id, product_name, quantity, unit_price)
VALUES (5, 4, 'Widget', 3, 9.99);

SELECT * FROM customers WHERE id = 4;
UPDATE customers SET email = 'dan.new@example.com' WHERE id = 4;
SELECT * FROM customers WHERE id = 4;

SELECT * FROM order_items WHERE order_id = 4;
DELETE FROM order_items WHERE order_id = 4;
SELECT COUNT(*) AS items_left FROM order_items WHERE order_id = 4;

EXPLAIN QUERY PLAN SELECT * FROM orders WHERE customer_id = 2;

CREATE INDEX idx_orders_customer_id ON orders(customer_id);

EXPLAIN QUERY PLAN SELECT * FROM orders WHERE customer_id = 2;
