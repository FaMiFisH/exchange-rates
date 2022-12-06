package curreny.exchange;

import java.math.BigDecimal;

public interface CurrencyExchange {
    public BigDecimal getRate(String currencyPair);
}
