-- Deterministic, non-production seed data for local Fabflix development.
-- Run after createtable.sql and create_employees.sql:
--   mysql -u <user> -p < database/dev-seed.sql
--
-- Development credentials:
--   Customer: dev.customer@fabflix.local / dev-password
--   Employee: dev.admin@fabflix.local / admin-password

USE moviedb;

START TRANSACTION;

-- The legacy schema starts customer passwords at VARCHAR(20), but the
-- application authenticates against salted Jasypt StrongPasswordEncryptor hashes.
ALTER TABLE customers MODIFY COLUMN password VARCHAR(128) NOT NULL;

CREATE TABLE IF NOT EXISTS employees (
    email VARCHAR(50) PRIMARY KEY,
    password VARCHAR(128) NOT NULL,
    fullname VARCHAR(100)
);

-- Remove only the reserved development records, making this script repeatable.
DELETE FROM sales
WHERE customer_id = 900001 OR movie_id LIKE 'tt900000%';
DELETE FROM ratings WHERE movie_id LIKE 'tt900000%';
DELETE FROM genres_in_movies WHERE movie_id LIKE 'tt900000%' OR genre_id BETWEEN 9001 AND 9006;
DELETE FROM stars_in_movies WHERE movie_id LIKE 'tt900000%' OR star_id LIKE 'nm900000%';
DELETE FROM customers WHERE id = 900001 OR email = 'dev.customer@fabflix.local';
DELETE FROM credit_cards WHERE id = 'DEV-CARD-0001';
DELETE FROM movies WHERE id LIKE 'tt900000%';
DELETE FROM stars WHERE id LIKE 'nm900000%';
DELETE FROM genres WHERE id BETWEEN 9001 AND 9006;
DELETE FROM employees WHERE email = 'dev.admin@fabflix.local';

INSERT INTO genres (id, name) VALUES
    (9001, 'Action'),
    (9002, 'Comedy'),
    (9003, 'Drama'),
    (9004, 'Horror'),
    (9005, 'Science Fiction'),
    (9006, 'Thriller');

INSERT INTO movies (id, title, year, director) VALUES
    ('tt9000001', 'Neon Horizon', 2021, 'Avery Stone'),
    ('tt9000002', 'Midnight Circuit', 2022, 'Jordan Lee'),
    ('tt9000003', 'Paper Moons', 2019, 'Morgan Chen'),
    ('tt9000004', 'Quiet Signal', 2023, 'Avery Stone'),
    ('tt9000005', 'Last Train Home', 2018, 'Riley Brooks'),
    ('tt9000006', 'Laughing Algorithm', 2024, 'Taylor Quinn'),
    ('tt9000007', 'The Empty Theater', 2020, 'Morgan Chen'),
    ('tt9000008', 'Zero Rating', 2017, 'Jordan Lee');

INSERT INTO stars (id, name, birth_year) VALUES
    ('nm9000001', 'Alex Rivera', 1985),
    ('nm9000002', 'Jamie Park', 1990),
    ('nm9000003', 'Casey Morgan', NULL),
    ('nm9000004', 'Robin Shah', 1978),
    ('nm9000005', 'Drew Kim', 1995),
    ('nm9000006', 'Sam Ellis', 1988);

INSERT INTO genres_in_movies (genre_id, movie_id) VALUES
    (9001, 'tt9000001'), (9005, 'tt9000001'),
    (9005, 'tt9000002'), (9006, 'tt9000002'),
    (9003, 'tt9000003'),
    (9004, 'tt9000004'), (9006, 'tt9000004'),
    (9003, 'tt9000005'),
    (9002, 'tt9000006'),
    (9003, 'tt9000007'),
    (9001, 'tt9000008');

INSERT INTO stars_in_movies (star_id, movie_id) VALUES
    ('nm9000001', 'tt9000001'), ('nm9000002', 'tt9000001'),
    ('nm9000002', 'tt9000002'), ('nm9000003', 'tt9000002'),
    ('nm9000004', 'tt9000003'),
    ('nm9000001', 'tt9000004'), ('nm9000005', 'tt9000004'),
    ('nm9000006', 'tt9000005'),
    ('nm9000003', 'tt9000006'), ('nm9000005', 'tt9000006'),
    ('nm9000004', 'tt9000007'),
    ('nm9000006', 'tt9000008');

-- tt9000007 intentionally has no rating. tt9000008 has a low rating.
INSERT INTO ratings (movie_id, rating, vote_count) VALUES
    ('tt9000001', 8.4, 1250),
    ('tt9000002', 7.6, 830),
    ('tt9000003', 6.9, 410),
    ('tt9000004', 8.8, 2100),
    ('tt9000005', 7.2, 605),
    ('tt9000006', 7.9, 990),
    ('tt9000008', 2.1, 12);

INSERT INTO credit_cards (id, first_name, last_name, expiration) VALUES
    ('DEV-CARD-0001', 'Dev', 'Customer', '2035-12-31');

INSERT INTO customers (
    id, first_name, last_name, credit_card_id, address, email, password
) VALUES (
    900001,
    'Dev',
    'Customer',
    'DEV-CARD-0001',
    '1 Local Development Way',
    'dev.customer@fabflix.local',
    'oy+gjhI4UDIdtW8ThDUs89WMvXvKGftQ1AQDC6272nMsNkeaVvh7Zc8RxJgmtTGG'
);

INSERT INTO employees (email, password, fullname) VALUES (
    'dev.admin@fabflix.local',
    '+hgnsX4gKEbPAP8rtCAYgO/WB60c8hdV0/CleRPy4XA//1M4GnNjdRPbmoJZEXa8',
    'Development Administrator'
);

-- One historical sale exercises order-history and aggregation queries.
INSERT INTO sales (customer_id, movie_id, sale_date) VALUES
    (900001, 'tt9000001', '2026-01-15');

COMMIT;

-- Lightweight verification output for manual execution.
SELECT COUNT(*) AS seeded_movies FROM movies WHERE id LIKE 'tt900000%';
SELECT COUNT(*) AS seeded_stars FROM stars WHERE id LIKE 'nm900000%';
SELECT COUNT(*) AS seeded_customers FROM customers WHERE id = 900001;
SELECT COUNT(*) AS seeded_employees FROM employees WHERE email = 'dev.admin@fabflix.local';
