package FST.MST_RSI.PFA.notification.domain.service;

/**
 * Utility class that converts phone numbers / SIP URIs into local Asterisk PJSIP extensions.
 *
 * <p><strong>Extension resolution logic</strong>:
 * <ol>
 *   <li>Strip all non-digit characters.</li>
 *   <li>Remove well-known country-code prefixes (212 for Morocco, 33 for France, 1 for USA/Canada,
 *       44 for UK, 213 for Algeria, 216 for Tunisia) when the resulting number starts with a
 *       local subscriber prefix (6, 7 for Morocco; etc.).</li>
 *   <li>Take the last {@code EXTENSION_DIGITS} digits (default: 4) as the local SIP extension.</li>
 * </ol>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code +212600001001} &rarr; {@code 1001}</li>
 *   <li>{@code 1001} &rarr; {@code 1001}</li>
 *   <li>{@code sip:1002@alertops.local} &rarr; {@code 1002} (via {@link #pjsipEndpoint})</li>
 * </ul>
 */
public final class SipEndpointMapper {

    /** Number of digits used for local SIP extensions (e.g. 1001, 1002). */
    private static final int EXTENSION_DIGITS = 4;

    private SipEndpointMapper() {
    }

    /**
     * Extracts the local SIP extension from a phone number or raw extension string.
     *
     * @param phone a phone number in any format, or a plain extension like "1001"
     * @return the local PJSIP extension (e.g. "1001")
     * @throws IllegalArgumentException if {@code phone} is blank or contains no digits
     */
    public static String extensionFromPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone/extension is required");
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            throw new IllegalArgumentException("Invalid SIP extension: " + phone);
        }

        // Strip common country codes so that e.g. +212600001001 -> 600001001 -> 1001
        digits = stripCountryCode(digits);

        // Return the last EXTENSION_DIGITS digits as the local extension
        if (digits.length() > EXTENSION_DIGITS) {
            digits = digits.substring(digits.length() - EXTENSION_DIGITS);
        }
        return digits;
    }

    /**
     * Removes a leading country-code prefix from a digit-only string when unambiguous.
     * Supported prefixes: 212 (MA), 213 (DZ), 216 (TN), 33 (FR), 44 (UK), 1 (US/CA).
     */
    private static String stripCountryCode(String digits) {
        // Morocco: 212 6/7 XXXXXXXX  (12 digits total including CC)
        if (digits.startsWith("212") && digits.length() >= 11) {
            return digits.substring(3);
        }
        // Algeria: 213 X XXXXXXXX
        if (digits.startsWith("213") && digits.length() >= 11) {
            return digits.substring(3);
        }
        // Tunisia: 216 X XXXXXXXX
        if (digits.startsWith("216") && digits.length() >= 11) {
            return digits.substring(3);
        }
        // France: 33 X XX XX XX XX (11 digits)
        if (digits.startsWith("33") && digits.length() == 11) {
            return digits.substring(2);
        }
        // UK: 44 XXXXXXXXXX (12 digits)
        if (digits.startsWith("44") && digits.length() == 12) {
            return digits.substring(2);
        }
        // USA/Canada: 1 XXXXXXXXXX (11 digits)
        if (digits.startsWith("1") && digits.length() == 11) {
            return digits.substring(1);
        }
        return digits;
    }

    public static String sipUri(String phone, String domain) {
        String host = domain == null || domain.isBlank() ? "localhost" : domain;
        return "sip:" + extensionFromPhone(phone) + "@" + host;
    }

    public static String pjsipEndpoint(String phone) {
        return "PJSIP/" + extensionFromPhone(phone);
    }
}
