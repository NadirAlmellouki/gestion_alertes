package FST.MST_RSI.PFA.rulesengine.domain.model;

public enum ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    IN,
    NOT_IN,
    IS_NULL,
    IS_NOT_NULL;

    public static ConditionOperator fromDb(String value) {
        if (value == null) {
            return EQUALS;
        }
        return switch (value.toUpperCase()) {
            case "=", "EQUALS" -> EQUALS;
            case "!=", "NOT_EQUALS", "≠" -> NOT_EQUALS;
            case "CONTIENT", "CONTAINS" -> CONTAINS;
            case "NOT_CONTAINS" -> NOT_CONTAINS;
            case "STARTS_WITH" -> STARTS_WITH;
            case "ENDS_WITH" -> ENDS_WITH;
            case ">", "GREATER_THAN" -> GREATER_THAN;
            case ">=", "GREATER_OR_EQUAL" -> GREATER_OR_EQUAL;
            case "<", "LESS_THAN" -> LESS_THAN;
            case "<=", "LESS_OR_EQUAL" -> LESS_OR_EQUAL;
            case "IN" -> IN;
            case "NOT IN", "NOT_IN" -> NOT_IN;
            case "IS_NULL" -> IS_NULL;
            case "IS_NOT_NULL" -> IS_NOT_NULL;
            default -> {
                try {
                    yield ConditionOperator.valueOf(value);
                } catch (IllegalArgumentException ex) {
                    yield EQUALS;
                }
            }
        };
    }

    public String toDbValue() {
        return name();
    }
}
