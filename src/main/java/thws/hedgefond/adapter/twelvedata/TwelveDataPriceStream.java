package thws.hedgefond.adapter.twelvedata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.twelvedata.client.ws.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwelveDataPriceStream {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataPriceStream.class);

    private final BroadcastProcessor<PriceEvent> ticks = BroadcastProcessor.create();
    private final Map<String, Multi<PriceEvent>> streams = new ConcurrentHashMap<>();

    private final TwelvedataWebSocketClient client;

    public TwelveDataPriceStream(TwelvedataWebSocketClient client) {
        this.client = client;
        this.client.addListener(new Listener());
    }

    public void disconnect() {
        log.info("Twelve Data WebSocket: Disconnect");
        client.disconnect();
    }

    public Multi<PriceEvent> prices(String symbol) {
        return streams.computeIfAbsent(symbol.toUpperCase(), this::open);
    }

    private Multi<PriceEvent> open(String symbol) {
        return Uni.createFrom()
                .completionStage(client::connect)
                .invoke(() -> subscribe(symbol))
                .onItem()
                .transformToMulti(_ -> ticks.filter(tick -> symbol.equals(tick.getSymbol())))
                .onTermination()
                .invoke(() -> unsubscribe(symbol))
                .broadcast()
                .withCancellationAfterLastSubscriberDeparture()
                .toAllSubscribers();
    }

    private void subscribe(String symbol) {
        log.info("Twelve Data WebSocket: Subscribe {}", symbol);
        client.subscribe(symbol);
    }

    private void unsubscribe(String symbol) {
        log.info("Twelve Data WebSocket: Unsubscribe {}", symbol);
        streams.remove(symbol);
        if (client.isConnected()) {
            client.unsubscribe(symbol);
        }
    }

    private class Listener implements TwelvedataWebSocketListener {

        private Listener() {}

        @Override
        public void onPrice(PriceEvent event) {
            log.debug("Twelve Data WebSocket: PriceEvent for {}", event.getSymbol());
            ticks.onNext(event);
        }

        @Override
        public void onSubscribeStatus(SubscribeStatusEvent event) {
            for (SubscribeStatusItem fail : event.getFails()) {
                log.error("Twelve Data WebSocket: Subscription failed for {}", fail.getSymbol());
            }
        }

        @Override
        public void onReconnecting(ReconnectingEvent event) {
            log.warn("Twelve Data WebSocket: Reconnecting (attempt {})", event.getAttempt());
        }

        @Override
        public void onError(TwelvedataWebSocketException e) {
            log.error("Twelve Data WebSocket: Error", e);
        }
    }
}
