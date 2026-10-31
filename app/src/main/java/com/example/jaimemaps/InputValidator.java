package com.example.jaimemaps;

import java.util.regex.Pattern;

/**
 * Validación y sanitización de entradas del usuario (lista blanca).
 * Solo se aceptan letras, números, espacios y . , - _  (máx. 40 caracteres).
 * Cualquier otro carácter (comillas, ;, --, <, >, etc.) hace que se rechace el texto.
 */
public final class InputValidator {

    public static final int MAX_TITLE_LENGTH = 40;

    private static final Pattern TITLE_PATTERN = Pattern.compile("^[\\p{L}\\p{N} _.,\\-]+$");

    private InputValidator() { }

    /** @return el título limpio, o null si no es válido. */
    public static String sanitizeTitle(String raw) {
        if (raw == null) return null;
        String t = raw.trim().replaceAll("\\s+", " ");
        if (t.isEmpty() || t.length() > MAX_TITLE_LENGTH) return null;
        if (!TITLE_PATTERN.matcher(t).matches()) return null;
        return t;
    }
}
