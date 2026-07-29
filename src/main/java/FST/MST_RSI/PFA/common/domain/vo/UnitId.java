package FST.MST_RSI.PFA.common.domain.vo;

public record UnitId(String value) {
    public UnitId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UnitId cannot be blank");
        }
    }
}
