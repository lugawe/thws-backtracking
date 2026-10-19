package thws.hedgefond.core.strategy.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import thws.hedgefond.core.marketdata.Candle;

public final class Indicators {

    private Indicators() {}

    public static BigDecimal sma(List<Candle> candles, int index, int period) {
        if (index + 1 < period) return null;
        return candles.subList(index + 1 - period, index + 1).stream()
                .map(Candle::close)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    public static BigDecimal ema(List<Candle> candles, int index, int period) {
        if (index + 1 < period) return null;
        BigDecimal ema = sma(candles, period - 1, period);
        BigDecimal multiplier = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1), 8, RoundingMode.HALF_UP);
        for (int i = period; i <= index; i++) {
            ema = candles.get(i).close().subtract(ema).multiply(multiplier).add(ema);
        }
        return ema;
    }

    public static BigDecimal rsi(List<Candle> candles, int index, int period) {
        if (index < period) return null;
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            BigDecimal change =
                    candles.get(i).close().subtract(candles.get(i - 1).close());
            if (change.signum() >= 0) gains = gains.add(change);
            else losses = losses.add(change.abs());
        }
        if (losses.signum() == 0) return BigDecimal.valueOf(100);
        BigDecimal rs = gains.divide(losses, 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP));
    }

    public static BollingerBands bollinger(List<Candle> candles, int index, int period, BigDecimal deviations) {
        BigDecimal middle = sma(candles, index, period);
        if (middle == null) return null;
        double variance = candles.subList(index + 1 - period, index + 1).stream()
                .mapToDouble(c -> c.close().doubleValue() - middle.doubleValue())
                .map(value -> value * value)
                .average()
                .orElse(0);
        BigDecimal width = BigDecimal.valueOf(Math.sqrt(variance)).multiply(deviations);
        return new BollingerBands(middle, middle.add(width), middle.subtract(width));
    }
}
