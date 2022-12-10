package curreny.exchange;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class CacheSystem extends CurrencyExchange {
    
    private HashMap<String,BigDecimal> cache;

    public boolean isEmpty(){
        return cache.size() == 0;
    }

    public void clear() {
        cache.clear();
    }

    /**
     * getRate - method to get the exchange rate from the database.
     * @param {currency} the currency pair to get the exchange rate of.
     * @return the exchange rate as BigDecimal.
     * @throws SQLException
     */
    public BigDecimal getRate(String currency) throws SQLException{
        
        String stmt = "SELECT rate, updated FROM exchange_rates WHERE currency=?";

        try {
            PreparedStatement preparedStmt = super.getConn().prepareStatement(stmt);
            preparedStmt.setString(1, currency);
            ResultSet resultSet = preparedStmt.executeQuery();

            if (resultSet == null) throw new NullPointerException("result set is null");
            
            while (resultSet.next()){
                return resultSet.getBigDecimal("rate");
            }

            throw new NullPointerException("invalid exchange rate");
            
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * get - method to get the exchange rate of the given currency pair from cache.
     * If the exchange rate is not in cache, make a call to the database and add it to the cache.
     * @param {currency} the currency to get the exchange rate of from base currency.
     * @return the exchange rate as BigDecimal.
     * @throws Exception
     */
    public BigDecimal get(String currency) throws Exception{
        BigDecimal rate = cache.get(currency);
        if (rate == null){

            try{
                // get the value from the database
                rate = getRate(currency);
            } catch (Exception e){
                throw e;
            }

            // add exchange rate to cache
            cache.put(currency, rate);
        }
        return rate;
    }
}
