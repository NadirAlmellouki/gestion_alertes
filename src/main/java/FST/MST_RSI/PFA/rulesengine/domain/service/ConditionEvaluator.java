package FST.MST_RSI.PFA.rulesengine.domain.service;

import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRuleContext;
import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

@Component
public class ConditionEvaluator {

    public boolean evaluate(BusinessRuleContext context, String fieldName, ConditionOperator operator, String expectedValue) {
        Object actual = context.fieldValue(fieldName);
        return switch (operator) {
            case IS_NULL -> actual == null || Objects.toString(actual, "").isBlank();
            case IS_NOT_NULL -> actual != null && !Objects.toString(actual, "").isBlank();
            case CONTAINS -> contains(actual, expectedValue);
            case NOT_CONTAINS -> !contains(actual, expectedValue);
            case STARTS_WITH -> startsWith(actual, expectedValue);
            case ENDS_WITH -> endsWith(actual, expectedValue);
            case IN -> inList(actual, expectedValue, false);
            case NOT_IN -> !inList(actual, expectedValue, false);
            case GREATER_THAN -> compareNumber(actual, expectedValue) > 0;
            case GREATER_OR_EQUAL -> compareNumber(actual, expectedValue) >= 0;
            case LESS_THAN -> compareNumber(actual, expectedValue) < 0;
            case LESS_OR_EQUAL -> compareNumber(actual, expectedValue) <= 0;
            case NOT_EQUALS -> !equalsValue(actual, expectedValue);
            case EQUALS -> equalsValue(actual, expectedValue);
        };
    }

    private static boolean equalsValue(Object actual, String expected) {
        if (actual instanceof Boolean bool) {
            return bool == parseBoolean(expected);
        }
        return normalize(actual).equals(normalize(expected));
    }

    private static boolean contains(Object actual, String expected) {
        return normalize(actual).contains(normalize(expected));
    }

    private static boolean startsWith(Object actual, String expected) {
        return normalize(actual).startsWith(normalize(expected));
    }

    private static boolean endsWith(Object actual, String expected) {
        return normalize(actual).endsWith(normalize(expected));
    }

    private static boolean inList(Object actual, String expected, boolean ignoreCase) {
        String actualNorm = ignoreCase ? normalize(actual) : Objects.toString(actual, "");
        return Arrays.stream(expected.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> ignoreCase ? s.toLowerCase(Locale.ROOT) : s)
                .anyMatch(item -> item.equals(actualNorm));
    }

    private static int compareNumber(Object actual, String expected) {
        double left = toNumber(actual);
        double right = Double.parseDouble(expected.trim());
        return Double.compare(left, right);
    }

    private static double toNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(Objects.toString(value, "0"));
    }

    private static boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(v) || "oui".equals(v) || "yes".equals(v);
    }

    private static String normalize(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean bool) {
            return bool ? "oui" : "non";
        }
        return value.toString().trim().toLowerCase(Locale.ROOT);
    }
}
