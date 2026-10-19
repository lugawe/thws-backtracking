package thws.hedgefond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import thws.hedgefond.core.strategy.StrategyDefinition;
import thws.hedgefond.core.strategy.dsl.StrategyParser;

public class StrategyParserTest {

    @Test
    void parsesTrendPullbackStrategy() {
        String source = """
                strategy "Trend Pullback" {
                  trend = ema(close, 200)
                  momentum = rsi(close, 14)

                  buy when close > trend and momentum < 35
                  sell when close < trend or momentum > 65

                  on buy do marketBuy()
                  on sell do marketSell()
                }
                """;

        StrategyDefinition strategy = new StrategyParser().parse(source);

        assertEquals("Trend Pullback", strategy.name());
        assertEquals(2, strategy.indicators().size());
        assertNotNull(strategy.buy());
        assertNotNull(strategy.sell());
    }

    @Test
    void parsesCrossingACompositeIndicatorValue() {
        StrategyDefinition strategy = new StrategyParser().parse("""
                strategy "Mean Reversion" {
                  bands = bollinger(close, 20, 2)
                  rsi = rsi(close, 14)
                  buy when crossesBelow(close, bands.lower) and rsi < 30
                  sell when crossesAbove(close, bands.middle)
                }
                """);

        assertEquals(2, strategy.indicators().size());
        assertNotNull(strategy.buy());
        assertNotNull(strategy.sell());
    }
}
