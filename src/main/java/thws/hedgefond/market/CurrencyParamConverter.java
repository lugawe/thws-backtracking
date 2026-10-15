package thws.hedgefond.market;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Currency;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/** Lets JAX-RS read {@link Currency} params like {@code ?currency=EUR} since it has no valueOf/fromString. */
@Provider
public class CurrencyParamConverter implements ParamConverterProvider {

    private static final ParamConverter<Currency> CONVERTER = new ParamConverter<>() {

        @Override
        public Currency fromString(String value) {
            try {
                return Currency.getInstance(value);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown currency: " + value);
            }
        }

        @Override
        public String toString(Currency value) {
            return value.getCurrencyCode();
        }
    };

    public CurrencyParamConverter() {}

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        return rawType == Currency.class ? (ParamConverter<T>) CONVERTER : null;
    }
}
