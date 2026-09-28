package com.quickbite.quickbite.util;

/**
 * Rates a password's strength so the registration screen can give real-time feedback and reject
 * passwords that are too weak. Purely rule-based (length + character variety) — no external
 * library needed for a check this simple.
 */
public class PasswordStrengthUtil {

    private static final int MIN_LENGTH = 6;

    /** From weakest to strongest, matching Java's natural enum ordering (TOO_SHORT.compareTo(WEAK) < 0). */
    public enum Strength {
        TOO_SHORT, WEAK, MEDIUM, STRONG
    }

    private PasswordStrengthUtil() {
    }

    public static Strength evaluate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return Strength.TOO_SHORT;
        }

        int score = 0;
        if (password.length() >= 10) score++;
        if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;

        if (score <= 1) return Strength.WEAK;
        if (score <= 2) return Strength.MEDIUM;
        return Strength.STRONG;
    }

    /** Human-readable feedback shown under the password field as the user types. */
    public static String describe(Strength strength) {
        return switch (strength) {
            case TOO_SHORT -> "Too short — use at least " + MIN_LENGTH + " characters.";
            case WEAK -> "Weak — add uppercase letters, numbers, or symbols.";
            case MEDIUM -> "Medium — add a symbol or more length to reach Strong.";
            case STRONG -> "Strong password.";
        };
    }

    /** CSS style class matching how serious the feedback is. */
    public static String styleClassFor(Strength strength) {
        return switch (strength) {
            case TOO_SHORT, WEAK -> "error-label";
            case MEDIUM -> "warning-label";
            case STRONG -> "success-label";
        };
    }

    /** QuickBite requires at least MEDIUM strength to create an account. */
    public static boolean isAcceptable(Strength strength) {
        return strength == Strength.MEDIUM || strength == Strength.STRONG;
    }
}