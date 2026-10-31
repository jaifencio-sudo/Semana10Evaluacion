package com.example.jaimemaps;

/**
 * Estado de autenticación en memoria (vive mientras viva el proceso de la app).
 *
 * - Si el proceso muere, la sesión se pierde y se vuelve a pedir autenticación
 *   (a diferencia de guardarla en el Bundle, que sobrevivía a la muerte del proceso).
 * - Sobrevive a la rotación de pantalla, para no pedir autenticación en cada giro.
 * - Si la app pasa a segundo plano más de {@code graceMs}, se exige autenticarse de nuevo.
 *
 * Todas las funciones reciben el reloj como parámetro para poder probarlas con
 * pruebas unitarias sin depender de Android.
 */
final class AuthSession {

    private static long lastAuthAt = 0L;   // 0 = nunca autenticado
    private static long lastLeftAt = 0L;   // 0 = la app sigue en primer plano

    private AuthSession() { }

    static synchronized void markAuthenticated(long now) {
        lastAuthAt = now;
        lastLeftAt = 0L;
    }

    /** La app salió de primer plano (se llama en onStop, salvo cambio de configuración). */
    static synchronized void markLeft(long now) {
        lastLeftAt = now;
    }

    /** ¿Hay que pedir autenticación al volver a primer plano? */
    static synchronized boolean needsAuth(long now, long graceMs) {
        if (lastAuthAt == 0L) return true;
        if (lastLeftAt != 0L && now - lastLeftAt > graceMs) return true;
        lastLeftAt = 0L; // vuelve dentro del plazo de gracia: sigue autenticado
        return false;
    }

    /** ¿La última autenticación es lo bastante reciente para una acción sensible? */
    static synchronized boolean isRecent(long now, long validityMs) {
        return lastAuthAt != 0L && now - lastAuthAt < validityMs;
    }

    /** Solo para pruebas unitarias. */
    static synchronized void reset() {
        lastAuthAt = 0L;
        lastLeftAt = 0L;
    }
}
