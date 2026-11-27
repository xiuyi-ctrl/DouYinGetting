package com.example.douyingetting;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class BackupEncryption {
    private static final byte[] MAGIC = "XIUYI-E2".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] LEGACY_MAGIC = "XIUYI-E1".getBytes(StandardCharsets.US_ASCII);
    private static final int SALT = 16, NONCE_PREFIX = 8, CHUNK = 65536, TAG = 16;
    private static final int HEADER = MAGIC.length + SALT + NONCE_PREFIX;

    static final class InvalidBackup extends IOException {
        InvalidBackup(Throwable cause) { super("备份密码不正确或文件已损坏", cause); }
    }

    static int version(BufferedInputStream input) throws IOException {
        byte[] prefix = new byte[MAGIC.length];
        input.mark(MAGIC.length);
        int length = 0;
        while (length < prefix.length) {
            int read = input.read(prefix, length, prefix.length - length);
            if (read < 0) break;
            length += read;
        }
        input.reset();
        if (length != MAGIC.length) return 0;
        if (Arrays.equals(prefix, MAGIC)) return 2;
        return Arrays.equals(prefix, LEGACY_MAGIC) ? 1 : 0;
    }

    private static SecretKey key(char[] password, byte[] header) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, Arrays.copyOfRange(header, MAGIC.length, MAGIC.length + SALT), 600000, 256);
        try {
            byte[] bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            try { return new SecretKeySpec(bytes, "AES"); }
            finally { Arrays.fill(bytes, (byte) 0); }
        } finally { spec.clearPassword(); }
    }

    // Each authenticated chunk binds the header, its ordinal and its size. An authenticated empty chunk ends the stream.
    private static byte[] crypt(int mode, SecretKey key, byte[] header, int index, int size, byte[] data, int length)
            throws GeneralSecurityException {
        byte[] nonce = ByteBuffer.allocate(12).put(header, MAGIC.length + SALT, NONCE_PREFIX).putInt(index).array();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(header);
        cipher.updateAAD(ByteBuffer.allocate(4).putInt(size).array());
        return cipher.doFinal(data, 0, length);
    }

    static OutputStream encrypt(OutputStream output, char[] password) throws GeneralSecurityException, IOException {
        if (password.length != 6) throw new IllegalArgumentException("A backup PIN must have 6 digits");
        for (char digit : password) if (digit < '0' || digit > '9')
            throw new IllegalArgumentException("A backup PIN must contain only digits");
        byte[] header = new byte[HEADER];
        new SecureRandom().nextBytes(header);
        System.arraycopy(MAGIC, 0, header, 0, MAGIC.length);
        SecretKey key = key(password, header);
        DataOutputStream stream = new DataOutputStream(output);
        stream.write(header);
        return new OutputStream() {
            private final byte[] buffer = new byte[CHUNK];
            private int used, index;
            private boolean closed;

            @Override public void write(int value) throws IOException {
                buffer[used++] = (byte) value;
                if (used == CHUNK) block();
            }

            @Override public void write(byte[] input, int offset, int length) throws IOException {
                while (length > 0) {
                    int count = Math.min(length, CHUNK - used);
                    System.arraycopy(input, offset, buffer, used, count);
                    used += count;
                    offset += count;
                    length -= count;
                    if (used == CHUNK) block();
                }
            }

            private void block() throws IOException {
                try {
                    byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, key, header, index++, used, buffer, used);
                    stream.writeInt(used);
                    stream.write(encrypted);
                    used = 0;
                } catch (GeneralSecurityException error) { throw new IOException(error); }
            }

            @Override public void close() throws IOException {
                if (closed) return;
                closed = true;
                try (DataOutputStream target = stream) {
                    if (used > 0) block();
                    block();
                } finally { Arrays.fill(buffer, (byte) 0); }
            }
        };
    }

    static InputStream decrypt(InputStream input, char[] password) throws GeneralSecurityException, IOException {
        DataInputStream stream = new DataInputStream(input);
        byte[] header = new byte[HEADER];
        try { stream.readFully(header); }
        catch (EOFException error) { throw new InvalidBackup(error); }
        byte[] prefix = Arrays.copyOf(header, MAGIC.length);
        if (!Arrays.equals(prefix, MAGIC) && !Arrays.equals(prefix, LEGACY_MAGIC)) throw new InvalidBackup(null);
        SecretKey key = key(password, header);
        return new InputStream() {
            private byte[] block = new byte[0];
            private int index, offset;
            private boolean ended, failed, closed;

            private boolean ready() throws IOException {
                if (offset < block.length) return true;
                if (ended) return false;
                try {
                    int size = stream.readInt();
                    if (size < 0 || size > CHUNK || index >= 65536) throw new InvalidBackup(null);
                    byte[] encrypted = new byte[size + TAG];
                    stream.readFully(encrypted);
                    block = crypt(Cipher.DECRYPT_MODE, key, header, index++, size, encrypted, encrypted.length);
                    offset = 0;
                    ended = size == 0;
                    if (ended && stream.read() != -1) throw new InvalidBackup(null);
                    return !ended;
                } catch (IOException | GeneralSecurityException error) {
                    failed = true;
                    throw error instanceof InvalidBackup ? (InvalidBackup) error : new InvalidBackup(error);
                }
            }

            @Override public int read() throws IOException {
                return ready() ? block[offset++] & 255 : -1;
            }

            @Override public int read(byte[] output, int start, int length) throws IOException {
                if (length == 0) return 0;
                if (!ready()) return -1;
                int count = Math.min(length, block.length - offset);
                System.arraycopy(block, offset, output, start, count);
                offset += count;
                return count;
            }

            @Override public void close() throws IOException {
                if (closed) return;
                closed = true;
                try (DataInputStream source = stream) {
                    // ZIP readers may stop at the directory before consuming the authentication trailer.
                    while (!failed && ready()) offset = block.length;
                }
            }
        };
    }
}
