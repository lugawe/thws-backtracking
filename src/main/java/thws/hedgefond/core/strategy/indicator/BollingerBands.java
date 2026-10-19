package thws.hedgefond.core.strategy.indicator;

import java.math.BigDecimal;

public record BollingerBands(BigDecimal middle, BigDecimal upper, BigDecimal lower) {}
