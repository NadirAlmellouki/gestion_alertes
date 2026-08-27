package FST.MST_RSI.PFA.dashboard.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardProjectionRepositoryTest {

    @ParameterizedTest
    @CsvSource({
            "false, , , 0, 0, INACTIVE",
            "true, , user@bank.com, 0, 0, PARTIAL",
            "true, +33601020304, user@bank.com, 5, 0, AVAILABLE",
            "true, +33601020304, user@bank.com, 2, 3, UNREACHABLE",
            "true, +33601020304, , 0, 0, AVAILABLE",
            "true, , , 0, 0, UNREACHABLE"
    })
    void deriveAvailability(
            boolean active,
            String phone,
            String email,
            long success,
            long failed,
            String expected
    ) {
        assertThat(DashboardProjectionRepository.deriveAvailability(active, phone, email, success, failed))
                .isEqualTo(expected);
    }

    @Test
    void deriveAvailabilityBlankPhoneTreatedAsMissing() {
        assertThat(DashboardProjectionRepository.deriveAvailability(true, "  ", "a@b.com", 0, 0))
                .isEqualTo("PARTIAL");
    }
}
