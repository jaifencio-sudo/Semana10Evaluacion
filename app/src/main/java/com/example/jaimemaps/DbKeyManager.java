package com.example.jaimemaps;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Gestiona la contraseña con la que se cifra la base de datos SQLite (SQLCipher).
 *
 * Diseño:
 *  1. Se genera una contraseña aleatoria de 256 bits (SecureRandom) la primera vez.
 *  2. Esa contraseña se guarda CIFRADA (AES-256-GCM) con una clave que vive en el
 *     Android Keystore y que no puede extraerse del dispositivo.
 *  3. En SharedPreferences privadas solo queda el texto cifrado + el IV.
 *
 * Así, aunque alguien copie el archivo de la base de datos o las preferencias,
 * no puede descifrarlos sin la clave protegida por el hardware/Keystore.
 * La contraseña nunca está en el código fuente ni en el APK.
 */
final class DbKeyManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "jaimemaps_db_wrap_key";
    private static final String PREFS = "jaimemaps_db_key_store";
    private static final String PREF_CIPHERTEXT = "ct";
    private static final String PREF_IV = "iv";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int PASSPHRASE_BYTES = 32;

    private DbKeyManager() { }

    /** Devuelve la contraseña de la base (la crea si no existe). */
    static synchronized String getOrCreatePassphrase(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            String ct = prefs.getString(PREF_CIPHERTEXT, null);
            String iv = prefs.getString(PREF_IV, null);
            if (ct != null && iv != null) {
                return decrypt(Base64.decode(ct, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP));
            }
            return createAndStore(prefs);
        } catch (GeneralSecurityException | RuntimeException e) {
            // La clave del Keystore se perdió o quedó inválida (p. ej. cambio de bloqueo
            // de pantalla o restauración del dispositivo). Los datos cifrados ya no son
            // recuperables: se reinicia el almacenamiento seguro en vez de fallar.
            resetAll(app, prefs);
            try {
                return createAndStore(prefs);
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("No se pudo inicializar el cifrado de la base de datos", ex);
            }
        }
    }

    private static String createAndStore(SharedPreferences prefs) throws GeneralSecurityException {
        byte[] raw = new byte[PASSPHRASE_BYTES];
        new SecureRandom().nextBytes(raw);
        String passphrase = Base64.encodeToString(raw, Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrapKey());
        byte[] ct = cipher.doFinal(passphrase.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();

        prefs.edit()
                .putString(PREF_CIPHERTEXT, Base64.encodeToString(ct, Base64.NO_WRAP))
                .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply();
        java.util.Arrays.fill(raw, (byte) 0);
        return passphrase;
    }

    private static String decrypt(byte[] ct, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrapKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    }

    private static SecretKey getOrCreateWrapKey() throws GeneralSecurityException {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) {
                return (SecretKey) ks.getKey(KEY_ALIAS, null);
            }
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            return kg.generateKey();
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException(e);
        }
    }

    /** Borra clave, preferencias y base cifrada cuando la clave ya no es utilizable. */
    private static void resetAll(Context app, SharedPreferences prefs) {
        prefs.edit().clear().apply();
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS);
        } catch (Exception ignored) {
            // si no se puede borrar, createAndStore volverá a usar/crear la clave
        }
        app.deleteDatabase(MarkerDbHelper.DB_NAME);
    }
}
