package thws.hedgefond.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Function;

public class CurrencyConverter implements Function<BigDecimal, BigDecimal> {

    private static final int SCALE = 6;

    private final BigDecimal rate;

    public CurrencyConverter(BigDecimal rate) {
        this.rate = Objects.requireNonNull(rate);
    }

    @Override
    public BigDecimal apply(BigDecimal amount) {
        return amount != null ? amount.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP) : null;
    }
}
