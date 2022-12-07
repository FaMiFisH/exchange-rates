DROP TABLE IF EXISTS exchange_rates;
CREATE TABLE exchange_rates (
    id SERIAL PRIMARY KEY,
    sourceCurrency VARCHAR(3),
    targetCurrency VARCHAR(3),
    rate DECIMAL,
    callTime TIMESTAMP 
);