package thws.hedgefond.core.strategy;

import java.math.BigDecimal;
import java.util.List;

public sealed interface Condition permits ComparisonCondition, CrossCondition, LogicalCondition {

    boolean matches(EvaluationContext context, int index);

    static Condition comparison(String left, String operator, String right) {
        return new ComparisonCondition(left, operator, right);
    }

    static Condition cross(String left, String direction, String right) {
        return new CrossCondition(left, direction, right);
    }

    static Condition logical(String operator, List<Condition> conditions) {
        return new LogicalCondition(operator, conditions);
    }
}

record ComparisonCondition(String left, String operator, String right) implements Condition {

    @Override
    public boolean matches(EvaluationContext context, int index) {
        BigDecimal leftValue = context.resolve(left, index);
        BigDecimal rightValue = context.resolve(right, index);
        if (leftValue == null || rightValue == null) {
            return false;
        }
        int comparison = leftValue.compareTo(rightValue);
        return switch (operator) {
            case ">" -> comparison > 0;
            case ">=" -> comparison >= 0;
            case "<" -> comparison < 0;
            case "<=" -> comparison <= 0;
            case "=" -> comparison == 0;
            default -> throw new StrategyException("Unknown comparison operator: " + operator);
        };
    }
}

record CrossCondition(String left, String direction, String right) implements Condition {

    @Override
    public boolean matches(EvaluationContext context, int index) {
        if (index == 0) {
            return false;
        }
        BigDecimal previousLeft = context.resolve(left, index - 1);
        BigDecimal previousRight = context.resolve(right, index - 1);
        BigDecimal currentLeft = context.resolve(left, index);
        BigDecimal currentRight = context.resolve(right, index);
        if (previousLeft == null || previousRight == null || currentLeft == null || currentRight == null) {
            return false;
        }
        if ("above".equals(direction)) {
            return previousLeft.compareTo(previousRight) <= 0 && currentLeft.compareTo(currentRight) > 0;
        }
        return previousLeft.compareTo(previousRight) >= 0 && currentLeft.compareTo(currentRight) < 0;
    }
}

record LogicalCondition(String operator, List<Condition> conditions) implements Condition {

    @Override
    public boolean matches(EvaluationContext context, int index) {
        return "and".equals(operator)
                ? conditions.stream().allMatch(condition -> condition.matches(context, index))
                : conditions.stream().anyMatch(condition -> condition.matches(context, index));
    }
}
