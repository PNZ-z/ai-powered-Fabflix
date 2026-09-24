USE moviedb;

CREATE TABLE IF NOT EXISTS employees (
                                         email VARCHAR(50) PRIMARY KEY,
                                         password VARCHAR(128) NOT NULL,
                                         fullname VARCHAR(100)
);
