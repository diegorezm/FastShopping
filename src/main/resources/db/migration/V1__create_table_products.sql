CREATE TABLE IF NOT EXISTS products (
    id UUID primary key,
    name varchar(256) not null,
    price NUMERIC(10,2) NOT NULL
);