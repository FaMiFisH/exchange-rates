package curreny.exchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExchangeRates {
    private static String sURL = "https://api.exchangerate.host/latest";
    // cache for exchange rates
    private static HashMap<String, Float> exchangeRates;

    public ExchangeRates(){
        // initialise hashMap to store currency exchange rates
        exchangeRates = new HashMap<String, Float>();

        try{
            updateRates();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * makeRequest - method to create a HTTP get request.
     * @return the connection instance of the request.
     */
    public HttpURLConnection makeGetRequest() throws IOException{
        // create a request 
        URL url = new URL(sURL);     
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("accept", "application/json");
        conn.connect();

        return conn;
    }

    /**
     * parseResponse - method to parse the JSON reponse and store in the hashmap.
     * @param {conn} the connection instance of the request.
     */
    public void parseResponse(HttpURLConnection conn) throws IOException {
        // parse the response
        JsonParser jp = new JsonParser();
        JsonElement root = jp.parse(new InputStreamReader((InputStream) conn.getContent()));
        JsonObject jsonobj = root.getAsJsonObject();

        // get the currency rates from the response data
        JsonObject rates = jsonobj.get("rates").getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> setRates = rates.entrySet();
        
        // store exchange rates in the hashmap
        for (Map.Entry<String, JsonElement> entry : setRates) {
            String country = entry.getKey();
            Float rate = entry.getValue().getAsFloat(); 
            exchangeRates.put(country, rate);
        }
    }

    /**
     * updateRates - method to update exchange rate.
     */
    public void updateRates() throws IOException{
        // remove all current exchange rates
        if (exchangeRates.size() > 0) exchangeRates.clear();

        // make a get request to the API
        HttpURLConnection conn = makeGetRequest();
            
        // get the status code of the response 
        int statusCode = conn.getResponseCode();

        // ensure the status code is: 200 OK
        if (statusCode != 200){
            throw new RuntimeException("HTTP response code: " + statusCode);
        }
        
        // parse and store the response
        parseResponse(conn);

        System.out.println("Updated exchange rates!");
    }
}
