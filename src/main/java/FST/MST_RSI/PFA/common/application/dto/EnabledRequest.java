package FST.MST_RSI.PFA.common.application.dto;

import jakarta.validation.constraints.NotNull;

public record EnabledRequest(@NotNull Boolean enabled) {
}
