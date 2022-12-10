# Currency Exchange 

The latest exchange rates are provided by the following API: https://api.exchangerate.host/latest.

The exchange rates are stored in a PostgreSQL database and have an expiry time of 1 hour till they need to be updated. Incase of heavy load on the database, the exchange rates are stored in a cache