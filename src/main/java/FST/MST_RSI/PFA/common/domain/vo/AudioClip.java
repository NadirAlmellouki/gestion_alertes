package FST.MST_RSI.PFA.common.domain.vo;

public record AudioClip(String storageUrl, String mimeType, int durationSeconds) {
    public AudioClip {
        if (storageUrl == null || storageUrl.isBlank()) {
            throw new IllegalArgumentException("storageUrl cannot be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be blank");
        }
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds cannot be negative");
        }
    }
}
