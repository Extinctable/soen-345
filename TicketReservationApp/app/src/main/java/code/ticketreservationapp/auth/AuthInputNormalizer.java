package code.ticketreservationapp.auth;

import java.util.Locale;
import java.util.regex.Pattern;

final class AuthInputNormalizer {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private AuthInputNormalizer() {}

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String trimmed = phone.trim();
        boolean hasPlusPrefix = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "";
        }
        return hasPlusPrefix ? "+" + digits : digits;
    }

    static boolean isValidEmail(String email) {
        return !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }

    static boolean isValidPhone(String phone) {
        if (phone.isEmpty()) {
            return false;
        }
        String digitsOnly = phone.startsWith("+") ? phone.substring(1) : phone;
        return digitsOnly.length() >= 10 && digitsOnly.length() <= 15;
    }
}
