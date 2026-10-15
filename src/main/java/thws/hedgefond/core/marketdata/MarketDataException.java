package thws.hedgefond.core.marketdata;

public class MarketDataException extends RuntimeException {

    public MarketDataException() {
        super();
    }

    public MarketDataException(String message) {
        super(message);
    }

    public MarketDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public MarketDataException(Throwable cause) {
        super(cause);
    }
}
