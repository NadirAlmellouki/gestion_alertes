package FST.MST_RSI.PFA.directory.application.service;

import java.util.Locale;
import java.util.regex.Pattern;

final class ReferentialPersonNormalizer {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ReferentialPersonNormalizer() {
    }

    record ParsedPerson(String firstName, String lastName, String fullName, String email) {
    }

    static boolean isValidEmail(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return EMAIL.matcher(value.trim().toLowerCase(Locale.ROOT)).matches();
    }

    static ParsedPerson fromPersonSheet(String lastName, String firstName, String emailRaw) {
        if (!isValidEmail(emailRaw)) {
            return null;
        }
        String email = emailRaw.trim().toLowerCase(Locale.ROOT);
        String fn = trimToNull(firstName);
        String ln = trimToNull(lastName);
        String fullName = buildFullName(fn, ln, email);
        return new ParsedPerson(fn, ln, fullName, email);
    }

    static ParsedPerson fromEmailOnly(String emailRaw) {
        if (!isValidEmail(emailRaw)) {
            return null;
        }
        String email = emailRaw.trim().toLowerCase(Locale.ROOT);
        return new ParsedPerson(null, null, email, email);
    }

    private static String buildFullName(String firstName, String lastName, String email) {
        if (firstName != null || lastName != null) {
            return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        }
        return email;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
