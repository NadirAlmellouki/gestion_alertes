package FST.MST_RSI.PFA.notification.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SipEndpointMapperTest {

    @Test
    void mapsPlainExtensionToItself() {
        assertThat(SipEndpointMapper.extensionFromPhone("1001")).isEqualTo("1001");
        assertThat(SipEndpointMapper.extensionFromPhone("1002")).isEqualTo("1002");
        assertThat(SipEndpointMapper.extensionFromPhone("1004")).isEqualTo("1004");
    }

    @Test
    void mapsMoroccanInternationalNumberToLocalExtension() {
        // +212 600 000 1001 -> strip 212 -> 6000001001 -> last 4 -> 1001
        assertThat(SipEndpointMapper.extensionFromPhone("+212600001001")).isEqualTo("1001");
        assertThat(SipEndpointMapper.extensionFromPhone("+212600001002")).isEqualTo("1002");
        assertThat(SipEndpointMapper.extensionFromPhone("+212600001003")).isEqualTo("1003");
        assertThat(SipEndpointMapper.extensionFromPhone("+212600001004")).isEqualTo("1004");
        // Without leading +
        assertThat(SipEndpointMapper.extensionFromPhone("212600001001")).isEqualTo("1001");
    }

    @Test
    void mapsAlgerianInternationalNumberToLocalExtension() {
        // +213 XX XXX 1001 -> strip 213 -> last 4 -> 1001
        assertThat(SipEndpointMapper.extensionFromPhone("+213555001001")).isEqualTo("1001");
    }

    @Test
    void buildsPjsipEndpointString() {
        assertThat(SipEndpointMapper.pjsipEndpoint("sip:1002@alertops.local")).isEqualTo("PJSIP/1002");
        assertThat(SipEndpointMapper.pjsipEndpoint("+212600001003")).isEqualTo("PJSIP/1003");
    }

    @Test
    void buildsSipUriString() {
        assertThat(SipEndpointMapper.sipUri("1003", "localhost")).isEqualTo("sip:1003@localhost");
        assertThat(SipEndpointMapper.sipUri("+212600001004", "alertops.local")).isEqualTo("sip:1004@alertops.local");
    }

    @Test
    void rejectsBlankPhone() {
        assertThatThrownBy(() -> SipEndpointMapper.extensionFromPhone("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPhoneWithNoDigits() {
        assertThatThrownBy(() -> SipEndpointMapper.extensionFromPhone("sip:@"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
