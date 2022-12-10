# Currency Exchange 

The latest exchange rates are provided by the following API: https://api.exchangerate.host/latest.

### Method
The exchange rates are stored in a PostgreSQL database and have an expiry time of 1 hour till they need to be updated. Incase of heavy load on the database, the exchange rates are stored in a cache. A threshold value on the number of accesses to the database is used to determine if the database is under heavy load. Once the database is stress free, we reset the cache to not use old exchange rates. The database stores the exchange rates relative to a base currency so there might be some inaccuracy when converting currencies.

### Improvements
- Also store the timestamps of exchange rates in the cache to update when invalid. 