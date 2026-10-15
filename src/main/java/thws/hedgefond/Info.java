package thws.hedgefond;

import java.util.Currency;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Multi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thws.hedgefond.core.marketdata.MarketDataProvider;

// TODO just for testing
@ApplicationScoped
public class Info {

    private static final Logger log = LoggerFactory.getLogger(Info.class);

    private final MarketDataProvider marketDataProvider;

    @Inject
    public Info(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    // @Startup
    public void test() {
        Multi.createFrom()
                .publisher(marketDataProvider.streamQuotes("AAPL", Currency.getInstance("EUR")))
                .subscribe()
                .with(q -> {
                    log.info("{} price: {} {}", q.name(), q.price(), q.currency());
                });
    }
}
