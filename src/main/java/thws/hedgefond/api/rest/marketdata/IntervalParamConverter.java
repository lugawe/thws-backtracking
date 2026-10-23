package thws.hedgefond.api.rest.marketdata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import thws.hedgefond.core.marketdata.Interval;

/** Lets JAX-RS read {@link Interval} params like {@code ?interval=1day}. */
@Provider
public class IntervalParamConverter implements ParamConverterProvider {

    private static final ParamConverter<Interval> CONVERTER = new ParamConverter<>() {

        @Override
        public Interval fromString(String value) {
            return switch (value) {
                case "1min" -> Interval.MINUTES_1;
                case "5min" -> Interval.MINUTES_5;
                case "1h" -> Interval.HOURS_1;
                case "1day" -> Interval.DAY_1;
                case "1week" -> Interval.WEEK_1;
                default -> throw new BadRequestException("Unknown interval: " + value);
            };
        }

        @Override
        public String toString(Interval value) {
            return switch (value) {
                case MINUTES_1 -> "1min";
                case MINUTES_5 -> "5min";
                case HOURS_1 -> "1h";
                case DAY_1 -> "1day";
                case WEEK_1 -> "1week";
            };
        }
    };

    public IntervalParamConverter() {}

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        return rawType == Interval.class ? (ParamConverter<T>) CONVERTER : null;
    }
}
