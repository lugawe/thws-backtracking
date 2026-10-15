package thws.hedgefond.core.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.SequencedCollection;
import java.util.concurrent.Flow;

public interface MarketDataProvider {

    BigDecimal getExchangeRate(Currency from, Currency to);

    Quote getQuote(String symbol, Currency currency);

    Flow.Publisher<Quote> streamQuotes(String symbol, Currency currency);

    SequencedCollection<Candle> getHistory(
            String symbol, Interval interval, LocalDate start, LocalDate end, long limit, Currency currency);
}
