package com.example.jaimemaps;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public final class AuthHelper {

    private static final int AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    private AuthHelper() { }

    public static void authenticate(@NonNull FragmentActivity activity,
                                    @NonNull Callback callback) {
        //  si el dispositivo no tiene bloqueo de pantalla, NO se concede acceso.
        if (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            callback.onFailure(activity.getString(R.string.auth_not_available));
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        callback.onFailure(errString.toString());
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.auth_title))
                .setSubtitle(activity.getString(R.string.auth_subtitle))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build();
        prompt.authenticate(info);
    }
}
