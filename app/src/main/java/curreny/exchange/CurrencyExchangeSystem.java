package curreny.exchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CurrencyExchangeSystem implements CurrencyExchange {

    // TODO: add cache system here
    // TODO: add a converter function

    private Connection dbConn;
    private String baseCurrency;
    private String currencyExchangeAPI;

    /**
     * Constructor.
     */
    public CurrencyExchangeSystem(){
        this.dbConn = getPortConnection();
        this.currencyExchangeAPI = "https://api.exchangerate.host/latest";
    }
    
    /**
     * getRate - method to return the exchange rate of the given currency pair.
     * @param {currencyPair} the currency pair to return the exchange rate of.
     * @return the exchange rate.
     */
    public BigDecimal getRate(String cuurencyPair){

        // TODO: check for time here

        // TODO: implement SQL query here
        return new BigDecimal(0);
    }

    /**
     * storeRate - method to store the rate of the given currency in the database.
     * If given currency is already in the database, updates its value.
     * @param {currency}
     * @param {rate}
     */
    public void storeRate(String currency, BigDecimal rate){
        String stmt = "INSERT INTO exchange_rates ('currency', 'rate', 'updated') VALUES (?,?,NOW()) ON DUPLICATE KEY UPDATE 'rate'=?, 'updated'=NOW()";

        try {
            PreparedStatement preparedStmt = dbConn.prepareStatement(stmt);

            preparedStmt.setString(1, currency);
            preparedStmt.setBigDecimal(2, rate);
            preparedStmt.setBigDecimal(3, rate);
            
            preparedStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * updateRates - method to update exchange rates in the database.
     * NOTE: each value represents the exchange rate from the base currency
     */
    public void updateRates() throws IOException{

        // make a get request to the API
        HttpURLConnection conn = getRequest();
            
        // get the status code of the response 
        int statusCode = conn.getResponseCode();

        // ensure the status code is: 200 OK
        if (statusCode != 200){
            throw new RuntimeException("HTTP response code: " + statusCode);
        }
        
        // get the exchange rates from the response body
        JsonObject rates =  parseResponse(conn);

        // iterate through each exchange rate 
        Set<Map.Entry<String, JsonElement>> setRates = rates.entrySet();
        for (Map.Entry<String, JsonElement> entry : setRates) {
            String currency = entry.getKey();
            BigDecimal rate = entry.getValue().getAsBigDecimal(); 
            
            storeRate(currency, rate);
        }

        System.out.println("Successfully updated exchange rates!");
    }

    /**
     * getRequest - method to create a HTTP get request.
     * @return the connection instance of the request.
     */
    private HttpURLConnection getRequest() throws IOException{
        URL url = new URL(currencyExchangeAPI);     
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
        if (baseCurrency != base) baseCurrency = base;

        return rates;
    }


    /**
     * getPortConnection - method to connect to the database.
     * @return connection to the database.
     */
    private Connection getPortConnection(){
        String user = "postgres";
        String passwrd = "password";
        Connection conn;

        // check if driver is available
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException x) {
            System.out.println("Driver could not be loaded");
        }

        try {
            conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/exchange_rates?user="+ user +"&password=" + passwrd);
            return conn;
        } catch(SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            e.printStackTrace();
            System.out.println("Error retrieving connection");
            return null;
        }
    }
}
