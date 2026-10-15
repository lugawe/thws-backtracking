package thws.hedgefond.api.marketdata;

import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thws.hedgefond.core.marketdata.MarketDataException;
import thws.hedgefond.core.marketdata.UnknownSymbolException;

/** Maps {@link UnknownSymbolException} to 404 and any other {@link MarketDataException} to 502. */
@Provider
public class MarketDataExceptionMapper implements ExceptionMapper<MarketDataException> {

    private static final Logger log = LoggerFactory.getLogger(MarketDataExceptionMapper.class);

    public MarketDataExceptionMapper() {}

    @Override
    public Response toResponse(MarketDataException e) {
        Response.Status status;
        if (e instanceof UnknownSymbolException) {
            status = Response.Status.NOT_FOUND;
        } else {
            status = Response.Status.BAD_GATEWAY;
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", e.getMessage()))
                .build();
    }
}
