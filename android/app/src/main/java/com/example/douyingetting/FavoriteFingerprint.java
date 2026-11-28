package com.example.douyingetting;

import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

final class FavoriteFingerprint {
    private static final String STORE = "AndroidKeyStore";

    static boolean available(Context context) {
        if (!context.getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)) return false;
        android.hardware.fingerprint.FingerprintManager fingerprint =
                context.getSystemService(android.hardware.fingerprint.FingerprintManager.class);
        if (fingerprint == null || !fingerprint.hasEnrolledFingerprints()) return false;
        BiometricManager manager = context.getSystemService(BiometricManager.class);
        if (manager == null) return false;
        int result = Build.VERSION.SDK_INT >= 30
                ? manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                : manager.canAuthenticate();
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    static Cipher cipher(String alias, boolean create) throws GeneralSecurityException, IOException {
        if (create) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE);
            KeyGenParameterSpec.Builder spec = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true);
            if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
            else spec.setUserAuthenticationValidityDurationSeconds(-1);
            generator.init(spec.build());
            generator.generateKey();
        }
        KeyStore store = KeyStore.getInstance(STORE);
        store.load(null);
        java.security.Key key = store.getKey(alias, null);
        if (key == null) throw new GeneralSecurityException("Biometric key unavailable");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }

    static void remove(String alias) {
        if (alias == null) return;
        try {
            KeyStore store = KeyStore.getInstance(STORE);
            store.load(null);
            store.deleteEntry(alias);
        } catch (IOException | GeneralSecurityException ignored) { }
    }
}
