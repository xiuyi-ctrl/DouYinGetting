package com.example.douyingetting;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

public final class BackupEncryptionTest {
    private static final char[] PASSWORD = "024861".toCharArray();

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }

    private static void rejected(byte[] data, char[] password) throws Exception {
        try (InputStream input = BackupEncryption.decrypt(new ByteArrayInputStream(data), password)) {
            input.read();
            // Closing an unread tail must still check every chunk.
        } catch (BackupEncryption.InvalidBackup expected) {
            return;
        }
        throw new AssertionError("Invalid backup was accepted");
    }

    public static void main(String[] args) throws Exception {
        byte[] original = new byte[3 * 65536 + 17];
        new Random(42).nextBytes(original);
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (OutputStream output = BackupEncryption.encrypt(archive, PASSWORD)) {
            output.write(original, 0, 11);
            output.write(original, 11, original.length - 11);
        }
        byte[] encoded = archive.toByteArray();
        BufferedInputStream encodedInput = new BufferedInputStream(new ByteArrayInputStream(encoded));
        check(BackupEncryption.version(encodedInput) == 2);
        check(BackupEncryption.version(new BufferedInputStream(new ByteArrayInputStream(original))) == 0);
        for (char[] pin : new char[][]{"12345".toCharArray(), "1234567".toCharArray(), "12a456".toCharArray()}) {
            try {
                BackupEncryption.encrypt(new ByteArrayOutputStream(), pin);
                throw new AssertionError("Invalid PIN was accepted");
            } catch (IllegalArgumentException expected) { }
        }
        try (InputStream input = BackupEncryption.decrypt(encodedInput, PASSWORD)) {
            check(Arrays.equals(original, input.readAllBytes()));
        }
        rejected(encoded, "wrong-password".toCharArray());
        byte[] changed = encoded.clone();
        changed[changed.length - 21] ^= 1;
        rejected(changed, PASSWORD);
        rejected(Arrays.copyOf(encoded, encoded.length - 20), PASSWORD);
        rejected(Arrays.copyOf(encoded, encoded.length + 1), PASSWORD);
        System.out.println("PASS: encrypted streaming round trip, wrong password, tamper, truncation and trailing bytes");
    }
}
