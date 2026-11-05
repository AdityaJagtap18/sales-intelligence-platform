package com.nexora.platform.privacy;

import java.util.regex.Pattern;

/**
 * Strips the common forms of personal data (GDPR Art. 4(1)) out of free text
 * before it is embedded into Qdrant, sent to the local Ollama model, or
 * persisted to a log/audit table -- data minimisation (Art. 5(1)(c)) applied
 * at the point text enters the pipeline, rather than trusting every
 * downstream consumer to handle it carefully. This is a pattern-based
 * heuristic (emails, phone numbers, SSNs, card-like numbers), not a named-
 * entity recognizer -- it will not catch a bare personal name with no
 * surrounding pattern. See docs/implementation/known-limitations.md.
 */
public final class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CREDIT_CARD = Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?\\d{1,3}[-. ]?)?\\(?\\d{3}\\)?[-. ]\\d{3}[-. ]\\d{4}\\b");

    private PiiRedactor() {
    }

    public static String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String redacted = text;
        redacted = CREDIT_CARD.matcher(redacted).replaceAll("[REDACTED-CARD]");
        redacted = SSN.matcher(redacted).replaceAll("[REDACTED-SSN]");
        redacted = EMAIL.matcher(redacted).replaceAll("[REDACTED-EMAIL]");
        redacted = PHONE.matcher(redacted).replaceAll("[REDACTED-PHONE]");
        return redacted;
    }
}
