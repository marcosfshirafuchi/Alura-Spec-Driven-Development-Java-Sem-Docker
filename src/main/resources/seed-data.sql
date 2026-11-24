-- Seed data para execução local (H2)
DELETE FROM order_items;
DELETE FROM payments;
DELETE FROM orders;
DELETE FROM products;
DELETE FROM categories;
DELETE FROM users;

-- Categorias
INSERT INTO categories (id, name) VALUES ('cat-01', 'Eletrônicos');

-- Usuários (senha padrão: '123456')
-- BCrypt hash para '123456': $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (id, name, email, password, role, tenant_id, created_at, updated_at)
VALUES ('usr-titular-01', 'Cliente Titular', 'titular@teste.com', '$2a$10$bjodOAeFNvIs2hrAjEtE6eBTXAnDRYF69Vd7fjg5ezKdfGcvAywFi', 'USER', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (id, name, email, password, role, tenant_id, created_at, updated_at)
VALUES ('usr-outro-01', 'Outro Cliente', 'outro@teste.com', '$2a$10$bjodOAeFNvIs2hrAjEtE6eBTXAnDRYF69Vd7fjg5ezKdfGcvAywFi', 'USER', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (id, name, email, password, role, tenant_id, created_at, updated_at)
VALUES ('usr-admin-01', 'Administrador', 'admin@teste.com', '$2a$10$bjodOAeFNvIs2hrAjEtE6eBTXAnDRYF69Vd7fjg5ezKdfGcvAywFi', 'ADMIN', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Produtos
INSERT INTO products (id, name, description, sku, price, stock, status, tenant_id, category_id, created_at, updated_at)
VALUES ('prod-01', 'Mouse Gamer', 'Mouse óptico 16000 DPI', 'MOUSE-01', 100.00, 10, 'ACTIVE', 'default', 'cat-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (id, name, description, sku, price, stock, status, tenant_id, category_id, created_at, updated_at)
VALUES ('prod-02', 'Teclado Mecânico', 'Teclado switch blue', 'TECLADO-01', 250.00, 5, 'ACTIVE', 'default', 'cat-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (id, name, description, sku, price, stock, status, tenant_id, category_id, created_at, updated_at)
VALUES ('prod-03', 'Monitor 4K', 'Monitor 27 pol IPS', 'MONITOR-01', 1500.00, 3, 'INACTIVE', 'default', 'cat-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Pedidos
INSERT INTO orders (id, user_id, status, total_value, tenant_id, created_at, updated_at)
VALUES ('ord-pending-01', 'usr-titular-01', 'PENDING', 200.00, 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO orders (id, user_id, status, total_value, tenant_id, created_at, updated_at)
VALUES ('ord-shipped-01', 'usr-titular-01', 'SHIPPED', 250.00, 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Itens de Pedido
INSERT INTO order_items (id, order_id, product_id, quantity, price)
VALUES ('item-01', 'ord-pending-01', 'prod-01', 2, 100.00);

INSERT INTO order_items (id, order_id, product_id, quantity, price)
VALUES ('item-02', 'ord-shipped-01', 'prod-02', 1, 250.00);
