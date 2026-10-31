package com.example.jaimemaps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class AuthSessionTest {

    private static final long GRACE = 30_000L;

    @Before
    public void setUp() {
        AuthSession.reset();
    }

    @Test
    public void neverAuthenticatedRequiresAuth() {
        assertTrue(AuthSession.needsAuth(1_000L, GRACE));
    }

    @Test
    public void afterAuthWithoutLeavingDoesNotRequireAuth() {
        AuthSession.markAuthenticated(1_000L);
        assertFalse(AuthSession.needsAuth(500_000L, GRACE)); // sigue en primer plano
    }

    @Test
    public void returnWithinGraceKeepsSession() {
        AuthSession.markAuthenticated(1_000L);
        AuthSession.markLeft(5_000L);
        assertFalse(AuthSession.needsAuth(5_000L + GRACE, GRACE));
    }

    @Test
    public void returnAfterGraceRequiresAuth() {
        AuthSession.markAuthenticated(1_000L);
        AuthSession.markLeft(5_000L);
        assertTrue(AuthSession.needsAuth(5_000L + GRACE + 1, GRACE));
    }

    @Test
    public void recentAuthWindowForSensitiveActions() {
        assertFalse(AuthSession.isRecent(1_000L, 30_000L));       // nunca autenticado
        AuthSession.markAuthenticated(10_000L);
        assertTrue(AuthSession.isRecent(39_999L, 30_000L));       // dentro de 30 s
        assertFalse(AuthSession.isRecent(40_000L, 30_000L));      // vencida
    }
}
