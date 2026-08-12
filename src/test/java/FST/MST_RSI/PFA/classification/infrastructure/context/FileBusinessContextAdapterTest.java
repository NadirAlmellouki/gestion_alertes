package FST.MST_RSI.PFA.classification.infrastructure.context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FileBusinessContextAdapterTest {

    @Autowired
    private FileBusinessContextAdapter adapter;

    @Test
    void findsPayCoreByName() {
        var candidates = adapter.findCandidates("PayCore");

        assertThat(candidates).isNotEmpty();
        assertThat(candidates.getFirst().name()).isEqualTo("PayCore");
        assertThat(candidates.getFirst().psi()).isNotBlank();
    }

    @Test
    void returnsEmptyWhenNoMatch() {
        assertThat(adapter.findCandidates("ZZZ-UNKNOWN-SOLUTION")).isEmpty();
    }
}
