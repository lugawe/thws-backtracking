package thws.hedgefond.market;

import java.time.LocalDate;
import java.util.SequencedCollection;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Multi;
import thws.hedgefond.market.MarketDataProvider.Currency;
import thws.hedgefond.market.MarketDataProvider.Interval;

@Path("/stocks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StockResource {

    private final MarketDataProvider provider;

    @Inject
    public StockResource(MarketDataProvider provider) {
        this.provider = provider;
    }

    @GET
    @Path("/{symbol}/quote")
    public Quote getQuote(
            @PathParam("symbol") String symbol, @QueryParam("currency") @DefaultValue("EUR") Currency currency) {

        return provider.getQuote(symbol, currency);
    }

    @GET
    @Path("/{symbol}/quote/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Quote> streamQuotes(
            @PathParam("symbol") String symbol, @QueryParam("currency") @DefaultValue("EUR") Currency currency) {

        return provider.streamQuotes(symbol, currency);
    }

    @GET
    @Path("/{symbol}/candles")
    public SequencedCollection<Candle> getHistory(
            @PathParam("symbol") String symbol,
            @QueryParam("interval") @DefaultValue("1day") Interval interval,
            @QueryParam("start") LocalDate start,
            @QueryParam("end") LocalDate end,
            @QueryParam("limit") @DefaultValue("30") long limit,
            @QueryParam("currency") @DefaultValue("EUR") Currency currency) {

        return provider.getHistory(symbol, interval, start, end, limit, currency);
    }
}
