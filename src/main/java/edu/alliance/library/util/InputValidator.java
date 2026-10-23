package edu.alliance.library.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern ISBN = Pattern.compile("^(?:\\d{9}[\\dXx]|\\d{13})$");
    private InputValidator() { }

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    public static boolean validEmail(String value) {
        return EMAIL.matcher(clean(value)).matches();
    }

    public static boolean validIsbn(String value) {
        return ISBN.matcher(clean(value).replaceAll("[- ]", "")).matches();
    }

}
