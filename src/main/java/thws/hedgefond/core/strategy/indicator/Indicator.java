package thws.hedgefond.core.strategy.indicator;

import java.util.List;

import thws.hedgefond.core.marketdata.Candle;

public interface Indicator {

    Object calculate(List<Candle> candles, int index);
}
