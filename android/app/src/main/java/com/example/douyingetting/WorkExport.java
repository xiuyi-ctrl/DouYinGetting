package com.example.douyingetting;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class WorkExport {
    static String name(String title, String link) {
        String clean = title.replaceAll("[^\\p{L}\\p{M}\\p{N} ._-]+", " ").replaceAll(" +", " ").trim();
        int length = clean.codePointCount(0, clean.length());
        if (length > 48) clean = clean.substring(0, clean.offsetByCodePoints(0, 48));
        clean = clean.replaceAll("^[ ._-]+|[ ._-]+$", "");
        if (clean.isEmpty()) clean = "作品";
        try {
            String source = link.split("[?#]", 2)[0].replaceFirst("/+$", "");
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder(clean).append('-');
            for (int i = 0; i < 5; i++) name.append(String.format(Locale.ROOT, "%02x", digest[i] & 255));
            return name.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    static void writeZip(OutputStream output, List<File> files) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            HashSet<String> names = new HashSet<>();
            for (File file : files) {
                String base = file.getName().replace('\\', '_');
                String name = base;
                for (int i = 2; !names.add(name.toLowerCase(Locale.ROOT)); i++) name = i + "-" + base;
                zip.putNextEntry(new ZipEntry(name));
                Files.copy(file.toPath(), zip);
                zip.closeEntry();
            }
        }
    }
}
