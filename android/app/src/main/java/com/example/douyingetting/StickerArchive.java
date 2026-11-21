package com.example.douyingetting;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Imports image originals from a desktop export without trusting ZIP paths or extensions. */
final class StickerArchive {
    private static final long MAX_TOTAL = 512L * 1024 * 1024;
    private static final long MAX_FILE = 32L * 1024 * 1024;
    interface Validator { void check(File file) throws Exception; }
    static final class Result {
        int added, duplicates, failed;
    }

    static Result read(InputStream input, File staging, File destination, Validator validator) throws Exception {
        Result result = new Result();
        ArrayList<File> installed = new ArrayList<>();
        try {
            if (!staging.mkdirs()) throw new IOException("无法创建导入目录");
            HashSet<String> hashes = new HashSet<>();
            long total = 0;
            int entries = 0;
            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input))) {
                ZipEntry entry;
                byte[] buffer = new byte[65536];
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entries > 5000) throw new IOException("压缩包条目超过 5000 个");
                    String name = entry.getName();
                    if (name.startsWith("/") || name.contains("\\") || name.contains(":"))
                        throw new IOException("压缩包路径无效");
                    for (String part : name.split("/"))
                        if (part.equals("..")) throw new IOException("压缩包路径无效");
                    if (entry.isDirectory()) {
                        if (zip.read() != -1) throw new IOException("压缩包目录无效");
                        continue;
                    }
                    File temp = new File(staging, "entry.tmp");
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    long size = 0;
                    byte[] header = new byte[12];
                    int headerSize = 0;
                    try (OutputStream out = new FileOutputStream(temp)) {
                        int count;
                        while ((count = zip.read(buffer)) != -1) {
                            total += count;
                            size += count;
                            if (total > MAX_TOTAL || size > MAX_FILE)
                                throw new IOException("压缩包过大：总计不超过 512 MB，单文件不超过 32 MB");
                            int take = Math.min(count, header.length - headerSize);
                            System.arraycopy(buffer, 0, header, headerSize, take);
                            headerSize += take;
                            digest.update(buffer, 0, count);
                            out.write(buffer, 0, count);
                        }
                    }
                    String extension = extension(header);
                    if (extension == null) {
                        if (name.toLowerCase(Locale.ROOT).matches(".*\\.(gif|png|jpe?g|webp)$")) result.failed++;
                        temp.delete();
                        continue;
                    }
                    StringBuilder hash = new StringBuilder();
                    for (byte b : digest.digest()) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
                    String output = hash + extension;
                    if (hashes.contains(output) || new File(destination, output).isFile()) {
                        result.duplicates++;
                        temp.delete();
                        continue;
                    }
                    try { validator.check(temp); }
                    catch (Exception error) { result.failed++; temp.delete(); continue; }
                    hashes.add(output);
                    Files.move(temp.toPath(), new File(staging, output).toPath());
                }
            }
            File[] files = staging.listFiles();
            if (files == null || (files.length == 0 && result.duplicates == 0))
                throw new IOException("压缩包中没有可读取的 PNG、JPG、GIF 或 WebP 表情");
            if (!destination.isDirectory() && !destination.mkdirs()) throw new IOException("无法保存导入表情");
            for (File file : files) {
                File target = new File(destination, file.getName());
                Files.move(file.toPath(), target.toPath());
                installed.add(target);
                result.added++;
            }
            return result;
        } catch (Exception error) {
            for (File file : installed) file.delete();
            throw error;
        } finally {
            File[] files = staging.listFiles();
            if (files != null) for (File file : files) file.delete();
            staging.delete();
        }
    }

    private static String extension(byte[] b) {
        if (b[0] == (byte) 0xff && b[1] == (byte) 0xd8 && b[2] == (byte) 0xff) return ".jpg";
        if (b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return ".png";
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return ".gif";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return ".webp";
        return null;
    }
}
