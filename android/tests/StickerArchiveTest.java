package com.example.douyingetting;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class StickerArchiveTest {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII=");
    private static byte[] zip(String[] names, byte[][] contents) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (int i = 0; i < names.length; i++) {
                out.putNextEntry(new ZipEntry(names[i]));
                out.write(contents[i]);
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
    private static StickerArchive.Result read(byte[] bytes, File root, File dest) throws Exception {
        return StickerArchive.read(new ByteArrayInputStream(bytes), new File(root, "stage"), dest, file -> {});
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory(Paths.get(args[0]), "archive-test-").toFile();
        File dest = new File(root, "images");
        byte[] valid = zip(new String[]{"images/renamed.bin", "images/copy.png", "README.txt", "bad.gif"},
                new byte[][]{PNG, PNG, "Instructions".getBytes(), "not an image".getBytes()});
        StickerArchive.Result first = read(valid, root, dest);
        check(first.added == 1 && first.duplicates == 1 && first.failed == 1);
        check(first.files.size() == 1 && first.installed.size() == 1);
        check(dest.listFiles()[0].getName().endsWith(".png"));
        StickerArchive.Result again = read(valid, root, dest);
        check(again.added == 0 && again.duplicates == 2 && dest.listFiles().length == 1);
        check(again.files.equals(first.files) && again.installed.isEmpty());
        byte[] different = PNG.clone(); different[different.length - 1] ^= 1;
        StickerArchive.Result mixed = read(zip(new String[]{"shared.png", "different.png"}, new byte[][]{PNG, different}), root, dest);
        check(mixed.files.size() == 2 && mixed.files.containsAll(first.files) && mixed.added == 1 && mixed.duplicates == 1);
        // Remove only the additional test file to retain the original rollback assertions below.
        for (String name : mixed.installed) check(new File(dest, name).delete());
        for (String path : new String[]{"../escape.png", "/absolute.png", "images/../../escape.png", "C:/escape.png", "images\\escape.png"}) {
            try { read(zip(new String[]{"good.png", path}, new byte[][]{PNG, PNG}), root, dest); throw new AssertionError(path); }
            catch (IOException expected) { }
            check(dest.listFiles().length == 1 && !new File(root, "stage").exists());
        }
        try { read("not a zip".getBytes(), root, dest); throw new AssertionError(); }
        catch (IOException expected) { }
        try { read(zip(new String[]{"folder/"}, new byte[][]{PNG}), root, dest); throw new AssertionError(); }
        catch (IOException expected) { }
        byte[] huge = new byte[32 * 1024 * 1024 + 1];
        try { read(zip(new String[]{"large.png"}, new byte[][]{huge}), root, dest); throw new AssertionError(); }
        catch (IOException expected) { check(expected.getMessage().contains("32 MB")); }
        check(dest.listFiles().length == 1 && !new File(root, "stage").exists());
        System.out.println("PASS: content detection, archive and cross-import deduplication, invalid media, traversal rejection, rollback and size limit");
    }
}
