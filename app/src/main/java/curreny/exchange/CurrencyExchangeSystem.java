package curreny.exchange;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class CurrencyExchangeSystem implements CurrencyExchange {
    
    /**
     * getRate - method to return the exchange rate of the given currency pair.
     * @param {currencyPair} the currency pair to return the exchange rate of.
     * @return the exchange rate.
     */
    public static BigDecimal getRate(String cuurencyPair){
        return new BigDecimal(0);
    }


    /**
     * getPortConnection - method to connect to the database.
     * @return connection to the database.
     */
    public static Connection getPortConnection(){
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
