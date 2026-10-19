package thws.hedgefond.core.strategy.dsl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import thws.hedgefond.core.strategy.Action;
import thws.hedgefond.core.strategy.Condition;
import thws.hedgefond.core.strategy.IndicatorDefinition;
import thws.hedgefond.core.strategy.StrategyDefinition;
import thws.hedgefond.core.strategy.StrategyException;

public class StrategyParser {

    private static final Pattern HEADER = Pattern.compile("strategy\\s+\\\"([^\\\"]+)\\\"\\s*\\{");
    private static final Pattern INDICATOR = Pattern.compile("(\\w+)\\s*=\\s*(\\w+)\\(([^)]*)\\)");
    private static final Pattern CROSS = Pattern.compile("crosses(Above|Below)\\(([\\w.]+),\\s*([\\w.]+)\\)");
    private static final Pattern COMPARISON = Pattern.compile("(.+?)\\s*(>=|<=|>|<|=)\\s*(.+)");

    public StrategyDefinition parse(String source) {
        String[] lines =
                source.lines().map(String::trim).filter(line -> !line.isBlank()).toArray(String[]::new);
        if (lines.length < 2) throw new StrategyException("Incomplete strategy");
        Matcher header = HEADER.matcher(lines[0]);
        if (!header.matches() || !"}".equals(lines[lines.length - 1])) {
            throw new StrategyException("Expected strategy \\\"name\\\" { ... }");
        }
        Map<String, IndicatorDefinition> indicators = new LinkedHashMap<>();
        Condition buy = null;
        Condition sell = null;
        Action buyAction = Action.BUY;
        Action sellAction = Action.SELL;
        for (int i = 1; i < lines.length - 1; i++) {
            String line = stripComment(lines[i]);
            if (line.isBlank()) continue;
            if (line.equals("on buy do marketBuy()")) {
                buyAction = Action.BUY;
                continue;
            }
            if (line.equals("on sell do marketSell()")) {
                sellAction = Action.SELL;
                continue;
            }
            Matcher indicator = INDICATOR.matcher(line);
            if (indicator.matches()) {
                indicators.put(
                        indicator.group(1),
                        new IndicatorDefinition(indicator.group(2).toLowerCase(), splitArguments(indicator.group(3))));
                continue;
            }
            if (line.startsWith("buy when ")) buy = parseCondition(line.substring(9));
            else if (line.startsWith("sell when ")) sell = parseCondition(line.substring(10));
            else throw new StrategyException("Unknown strategy statement: " + line);
        }
        return new StrategyDefinition(header.group(1), indicators, buy, sell, buyAction, sellAction);
    }

    private Condition parseCondition(String expression) {
        String[] orParts = expression.split("\\s+or\\s+");
        if (orParts.length > 1) {
            return Condition.logical(
                    "or", Arrays.stream(orParts).map(this::parseAnd).toList());
        }
        return parseAnd(expression);
    }

    private Condition parseAnd(String expression) {
        String[] andParts = expression.split("\\s+and\\s+");
        if (andParts.length > 1) {
            return Condition.logical(
                    "and", Arrays.stream(andParts).map(this::parseAtom).toList());
        }
        return parseAtom(expression);
    }

    private Condition parseAtom(String expression) {
        Matcher cross = CROSS.matcher(expression.trim());
        if (cross.matches()) {
            return Condition.cross(cross.group(2), cross.group(1).toLowerCase(), cross.group(3));
        }
        Matcher comparison = COMPARISON.matcher(expression.trim());
        if (!comparison.matches()) throw new StrategyException("Invalid condition: " + expression);
        return Condition.comparison(
                comparison.group(1).trim(),
                comparison.group(2),
                comparison.group(3).trim());
    }

    private static List<String> splitArguments(String arguments) {
        List<String> result = new ArrayList<>();
        for (String argument : arguments.split(",")) result.add(argument.trim());
        return result;
    }

    private static String stripComment(String line) {
        int comment = line.indexOf("//");
        return comment >= 0 ? line.substring(0, comment).trim() : line;
    }
}
