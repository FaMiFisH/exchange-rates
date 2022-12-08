package curreny.exchange;

import java.math.BigDecimal;
import java.util.HashMap;

public class CacheSystem implements CurrencyExchange {
    
    private HashMap<String,BigDecimal> cache;

    /**
     * getRate - method to get the exchange rate from the database.
     * @param {currencyPair} the currency pair to get the exchange rate of.
     * @return the exchange rate as BigDecimal.
     */
    public BigDecimal getRate(String currencyPair){
        // TODO: implement this
        return new BigDecimal(0.0);
    }

    /**
     * get - method to get the exchange rate of the given currency pair from cache.
     * If the exchange rate is not in cache, make a call to the database and add it to the cache.
     * @param {currencyPair} the currency pair to get the exchange rate of.
     * @return the exchange rate as BigDecimal.
     */
    public BigDecimal get(String currencyPair){
        BigDecimal rate = cache.get(currencyPair);
        if (rate == null){
            // get the value from the database
            rate = getRate(currencyPair);

            // add exchange rate to cache
            cache.put(currencyPair, rate);
        }
        return rate;
    }
}
