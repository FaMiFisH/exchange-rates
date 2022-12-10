package curreny.exchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CurrencyExchangeSystem extends CurrencyExchange {

    private CacheSystem cache;
    private String baseCurrency;
    private String currencyExchangeAPI;
    private ArrayList<String> currencies;

    /**
     * Constructor. Initailise the database.
     * @throws SQLException
     * @throws IOException
     */
    public CurrencyExchangeSystem() throws SQLException {
        this.cache = new CacheSystem();
        this.currencyExchangeAPI = "https://api.exchangerate.host/latest";
        this.currencies = new ArrayList<String>();
        
        try {
            updateRates();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * exchangeCurrency - method to exchange source currency to target currency.
     * The base class is used as an intermediatory in the conversion.
     * @param {sourceCurrency}
     * @param {targetCurrency}
     * @param {amount}
     * @return the amount in the target currency.
     * @throws IOException
     * @throws SQLException
     */
    public BigDecimal currencyExchange(String sourceCurrency, String targetCurrency, Double amount) throws SQLException, IOException{

        // validate provided currencies
        if (!currencies.contains(sourceCurrency)) throw new IllegalArgumentException(sourceCurrency + " is not supported!");
        if (!currencies.contains(targetCurrency)) throw new IllegalArgumentException(targetCurrency + " is not supported!");

        // convert to BigDecimal inorder to perform BigDecimal operations 
        BigDecimal value = BigDecimal.valueOf(amount);

        // convert from source currency to base currency
        BigDecimal base_source_rate = getRate(sourceCurrency);
        BigDecimal baseValue = value.divide(base_source_rate, 10, RoundingMode.HALF_UP);

        // convert base currency to target currency
        BigDecimal base_target_rate = getRate(targetCurrency);
        BigDecimal targetValue = baseValue.multiply(base_target_rate);

        BigDecimal exchangeRate = base_target_rate.divide(base_source_rate, 5, RoundingMode.HALF_UP);
        System.out.println("Exchange rate from " + sourceCurrency + " to " + targetCurrency + ": " + exchangeRate);

        return targetValue.setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }
    

    /**
     * getRate - method to return the exchange rate of the given currency pair.
     * NOTE: since currency column is unique in the database, stmt1 returns just 1 row.
     * To increase performance, if database is under heavy load, load the exchange rate from cahce.
     * If the exchange rate is invalid, we update the database with latest values.
     * @param {currency} the currency to return the exchange rate of from base currency.
     * @return the exchange rate.
     * @throws SQLException
     * @throws IOException
     */
    public BigDecimal getRate(String currency) throws SQLException {

        // if database is under heavy load, load exchange rate from cache
        if ( isDatabaseUnderHeavyLoad()) return this.cache.getRate(currency); 

        String stmt1 = "SELECT rate, updated FROM exchange_rates WHERE currency=?";
        try {
            PreparedStatement preparedStmt = super.getConn().prepareStatement(stmt1);
            preparedStmt.setString(1, currency);
            ResultSet resultSet = preparedStmt.executeQuery();

            if (resultSet == null) throw new IllegalStateException("result set is null");
            
            // check if exchange rate is valid
            while (resultSet.next()){
                Timestamp lastUpdated = resultSet.getTimestamp("updated");

                // if exchange rate is invalid, update exchange rates in the database
                if(!isExchangeRateValid(lastUpdated)){
                    try {
                        updateRates();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    return resultSet.getBigDecimal("rate");
                }
            }

            // Database was updated so re-execute the query
            BigDecimal rate = null;
            resultSet = preparedStmt.executeQuery();
            while (resultSet.next()) {
                rate =  resultSet.getBigDecimal("rate");
            }

            if (rate == null) throw new IllegalStateException("Rate has null value");

            return rate;
            
        } catch (SQLException e) {
            throw e;
        }
    }


    /**
     * isDatabaseUnderHeavyLoad - method to determine if database is under heavy load.
     * @throws SQLException
     * @return determined by number of current connections being > 10.
     */
    private boolean isDatabaseUnderHeavyLoad() throws SQLException {
        String stmt = "SELECT COUNT(*) AS total FROM pg_stat_activity WHERE datname = 'exchange_rates'";
        try {
            PreparedStatement preparedStmt = super.getConn().prepareStatement(stmt);
            ResultSet resultSet = preparedStmt.executeQuery();

            int total = 0;
            while(resultSet.next()) {
                total = resultSet.getInt("total");
            }

            return total >= 10; 

        } catch (SQLException e) {
            throw e;
        }
    }


    /**
     * isExchangeRateValid - method to determine if the given timestamp is valid.
     * The timestamp is valid if it is within the past hour.
     * @param {time} 
     * @return boolean result.
     */
    private boolean isExchangeRateValid(Timestamp time) {
        Date date = new Date();  
        Timestamp currentTime = new Timestamp(date.getTime());  

        // subtract an hour
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTime.getTime());
        cal.add(Calendar.HOUR, -1);
        currentTime = new Timestamp(cal.getTime().getTime());
        
        return time.compareTo(currentTime) >= 0 ? true : false;
    }


    /**
     * storeRate - method to store the rate of the given currency in the database.
     * If given currency is already in the database, updates its value.
     * @param {currency}
     * @param {rate}
     * @throws SQLException
     */
    private void storeRate(String currency, BigDecimal rate) throws SQLException{
        String stmt = "INSERT INTO exchange_rates (currency, rate, updated) VALUES (?,?,NOW()) ON CONFLICT (currency) DO UPDATE SET rate=?, updated=NOW()";

        try {
            PreparedStatement preparedStmt = super.getConn().prepareStatement(stmt);

            preparedStmt.setString(1, currency);
            preparedStmt.setBigDecimal(2, rate);
            preparedStmt.setBigDecimal(3, rate);
            
            preparedStmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }


    /**
     * updateRates - method to update exchange rates in the database.
     * NOTE: each value represents the exchange rate from the base currency.
     * @throws SQLException
     */
    private void updateRates() throws IOException, SQLException{

        // make a get request to the API
        HttpURLConnection conn = getRequest();
            
        // get the status code of the response 
        int statusCode = conn.getResponseCode();

        // ensure the status code is: 200 OK
        if (statusCode != 200) throw new RuntimeException("HTTP response code: " + statusCode);
        
        // get the exchange rates from the response body
        JsonObject rates =  parseResponse(conn);

        // flag to determine whether we need to initialise currencies
        boolean flag = currencies.size() == 0;

        // iterate through each exchange rate 
        Set<Map.Entry<String, JsonElement>> setRates = rates.entrySet();
        for (Map.Entry<String, JsonElement> entry : setRates) {
            String currency = entry.getKey();
            BigDecimal rate = entry.getValue().getAsBigDecimal(); 
            // initialise currencies
            if (flag) this.currencies.add(currency);
            
            // store exchange rate to the database
            storeRate(currency, rate);
        }
    }


    /**
     * getRequest - method to create a HTTP get request.
     * @return the connection instance of the request.
     */
    private HttpURLConnection getRequest() throws IOException{
        URL url = new URL(this.currencyExchangeAPI);     
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("accept", "application/json");
        conn.connect();

        return conn;
    }


    /**
     * parseResponse - method to parse the JSON reponse and store in the hashmap.
     * @param {conn} the connection instance of the request.
     * @return the JSON containing the exchange rates.
     */
    private JsonObject parseResponse(HttpURLConnection conn) throws IOException {
        JsonParser jp = new JsonParser();
        JsonElement root = jp.parse(new InputStreamReader((InputStream) conn.getContent()));
        JsonObject jsonobj = root.getAsJsonObject();

        // get the base currency and response data
        String base = jsonobj.get("base").getAsString();
        JsonObject rates = jsonobj.get("rates").getAsJsonObject();
        
        // update base currency if needed
        if (this.baseCurrency != base) this.baseCurrency = base;

        return rates;
    }
}
