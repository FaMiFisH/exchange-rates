DROP TABLE IF EXISTS exchange_rates;
CREATE TABLE exchange_rates (
    id SERIAL PRIMARY KEY,
    currency VARCHAR(3) UNIQUE,
    rate DECIMAL,
    updated TIMESTAMP 
);