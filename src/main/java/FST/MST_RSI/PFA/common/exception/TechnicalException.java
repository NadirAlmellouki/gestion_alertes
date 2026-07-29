package FST.MST_RSI.PFA.common.exception;

public class TechnicalException extends RuntimeException {

    private final String code;

    public TechnicalException(String message) {
        this("TECHNICAL_ERROR", message);
    }

    public TechnicalException(String message, Throwable cause) {
        this("TECHNICAL_ERROR", message, cause);
    }

    public TechnicalException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TechnicalException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
