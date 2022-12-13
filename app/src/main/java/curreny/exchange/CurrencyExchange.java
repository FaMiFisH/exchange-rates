package curreny.exchange;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class CurrencyExchange implements AutoCloseable {

    private Connection dbConn;

    public CurrencyExchange(){
        this.dbConn = getPortConnection();
    }


    public Connection getConn(){
        return this.dbConn;
    }

    @Override
    public void close() throws Exception {
        this.dbConn.close();
    }


    /**
     * getRate - abstract method to return the exchange rate of the given currency.
     * @param {currency}
     * @return the exchange rate of the given currency.
     */
    public abstract BigDecimal getRate(String currency) throws SQLException;
    

    /**
     * getPortConnection - method to connect to the database.
     * @return connection to the database.
     */
    private Connection getPortConnection(){
        Connection conn;

        // check if driver is available
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException x) {
            System.out.println("Driver could not be loaded");
        }

        try {
            conn = DriverManager.getConnection("jdbc:postgresql://" + AWSAuth.endpoint + ":" + AWSAuth.port + "/" + AWSAuth.dbName + "?user="+ AWSAuth.username +"&password=" + AWSAuth.password);
            return conn;
        } catch(SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            e.printStackTrace();
            System.out.println("Error retrieving connection");
            return null;
        }
    }

}
