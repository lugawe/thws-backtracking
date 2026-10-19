package thws.hedgefond.core.strategy;

import java.math.BigDecimal;

public record StrategySignal(long timestamp, Action action, BigDecimal price) {}
