package com.example.douyingetting;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public class RecordArchiveTest {
    private static final byte[] ANIMATION = "GIF89a-original-animation-bytes".getBytes(StandardCharsets.US_ASCII);
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    private static List<String> validateRecord(File folder) throws Exception {
        if (!new File(folder, "manifest.json").isFile() || !new File(folder, "media.gif").isFile())
            throw new IOException("Missing record files");
        return Arrays.asList("manifest.json", "media.gif");
    }
    private static String validateFavorite(File file) throws Exception {
        if (!Arrays.equals(Files.readAllBytes(file.toPath()), ANIMATION)) throw new IOException("Bad favorite");
        return ".gif";
    }
    private static byte[] write(Map<String, File> records, List<File> favorites) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RecordArchive.write(out, records, favorites, RecordArchiveTest::validateRecord, RecordArchiveTest::validateFavorite);
        return out.toByteArray();
    }
    private static RecordArchive.Result read(byte[] zip, File root) throws Exception {
        return RecordArchive.read(new ByteArrayInputStream(zip), new File(root, "stage"), new File(root, "records"),
                new File(root, "favorites"), Collections.emptySet(), RecordArchiveTest::validateRecord, RecordArchiveTest::validateFavorite);
    }
    private static byte[] zip(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(data)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey())); zip.write(entry.getValue()); zip.closeEntry();
            }
        }
        return data.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory(Paths.get(args[0]), "backup-test-").toFile();
        File source = new File(root, "source"); check(source.mkdirs());
        File record = new File(source, "record"); check(record.mkdirs());
        Files.write(new File(record, "manifest.json").toPath(), "{}".getBytes());
        Files.write(new File(record, "media.gif").toPath(), ANIMATION);
        File favorite = new File(source, "favorite.gif"); Files.write(favorite.toPath(), ANIMATION);
        check(favorite.setLastModified(1600000000000L));
        Map<String, File> records = Collections.singletonMap("record-1", record);
        byte[] complete = write(records, Arrays.asList(favorite, favorite));
        File all = new File(root, "all"); check(all.mkdirs());
        File stage = new File(all, "stage");
        File recordsDestination = new File(all, "records");
        File favoritesDestination = new File(all, "favorites");
        try (RecordArchive.Prepared prepared = RecordArchive.prepare(new ByteArrayInputStream(complete), stage,
                RecordArchiveTest::validateRecord, RecordArchiveTest::validateFavorite)) {
            RecordArchive.Result preview = prepared.preview(recordsDestination, favoritesDestination, Collections.emptySet());
            check(prepared.recordCount() == 1 && preview.skipped == 0 && preview.favoriteHashes.size() == 1);
            check(preview.duplicateFavoriteHashes.isEmpty() && !recordsDestination.exists() && !favoritesDestination.exists());
        }
        check(!stage.exists() && !recordsDestination.exists() && !favoritesDestination.exists());
        RecordArchive.Result imported = read(complete, all);
        check(imported.added.size() == 1 && imported.favoritesAdded == 1);
        File[] saved = new File(all, "favorites").listFiles(); check(saved.length == 1);
        check(Arrays.equals(Files.readAllBytes(saved[0].toPath()), ANIMATION));
        check(Math.abs(saved[0].lastModified() - favorite.lastModified()) < 2000);
        RecordArchive.Result again = read(complete, all);
        check(again.added.isEmpty() && again.skipped == 1 && again.favoritesAdded == 0 && again.favoritesSkipped == 1);
        try (RecordArchive.Prepared prepared = RecordArchive.prepare(new ByteArrayInputStream(complete), stage,
                RecordArchiveTest::validateRecord, RecordArchiveTest::validateFavorite)) {
            RecordArchive.Result preview = prepared.preview(recordsDestination, favoritesDestination, Collections.emptySet());
            check(preview.skipped == 1 && preview.duplicateFavoriteHashes.equals(imported.favoriteHashes));
            RecordArchive.Result restored = prepared.install(recordsDestination, favoritesDestination, Collections.emptySet());
            check(restored.skipped == preview.skipped && restored.duplicateFavoriteHashes.equals(preview.duplicateFavoriteHashes));
        }
        File favoriteOnly = new File(root, "favorite-only"); check(favoriteOnly.mkdirs());
        check(read(write(Collections.emptyMap(), Collections.singletonList(favorite)), favoriteOnly).favoritesAdded == 1);
        byte[] metadata = "{\"groups\":[\"表情\"],\"sources\":{}}".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream v3 = new ByteArrayOutputStream();
        RecordArchive.write(v3, Collections.emptyMap(), Collections.singletonList(favorite), metadata,
                RecordArchiveTest::validateRecord, RecordArchiveTest::validateFavorite);
        File restoredMetadata = new File(root, "with-metadata"); check(restoredMetadata.mkdirs());
        RecordArchive.Result v3Result = read(v3.toByteArray(), restoredMetadata);
        check(Arrays.equals(metadata, v3Result.favoriteMetadata));
        check(v3Result.favoritesAdded == 1 && v3Result.favoriteHashes.size() == 1);
        RecordArchive.Result duplicate = read(v3.toByteArray(), restoredMetadata);
        check(duplicate.favoritesSkipped == 1 && duplicate.favoriteHashes.equals(v3Result.favoriteHashes));
        File renamed = new File(root, "renamed"); check(new File(renamed, "favorites").mkdirs());
        Files.write(new File(renamed, "favorites/legacy-name.gif").toPath(), ANIMATION);
        check(read(write(Collections.emptyMap(), Collections.singletonList(favorite)), renamed).favoritesSkipped == 1);
        Map<String, byte[]> legacy = new LinkedHashMap<>();
        legacy.put("xiuyi-records.version", "1\n".getBytes());
        legacy.put("records/legacy/manifest.json", "{}".getBytes());
        legacy.put("records/legacy/media.gif", ANIMATION);
        File old = new File(root, "old"); check(old.mkdirs());
        RecordArchive.Result oldResult = read(zip(legacy), old);
        check(oldResult.added.size() == 1 && oldResult.favoritesAdded == 0);
        for (String invalid : Arrays.asList("favorites/../../escape.gif", "favorites/invalid.gif")) {
            Map<String, byte[]> broken = new LinkedHashMap<>(legacy);
            broken.put("xiuyi-records.version", "2\n".getBytes());
            broken.put(invalid, "bad media".getBytes());
            File failure = new File(root, "failure-" + UUID.randomUUID()); check(failure.mkdirs());
            try { read(zip(broken), failure); throw new AssertionError("Bad ZIP accepted"); }
            catch (IOException expected) { }
            check(!new File(failure, "records/legacy").exists() && !new File(failure, "stage").exists());
        }
        // A destination conflict must roll back records installed earlier in this import.
        File conflict = new File(root, "conflict"); check(new File(conflict, "favorites").mkdirs());
        Files.write(new File(conflict, "favorites/" + saved[0].getName()).toPath(), "different existing file".getBytes());
        try { read(complete, conflict); throw new AssertionError("Conflict accepted"); }
        catch (IOException expected) { }
        check(!new File(conflict, "records/record-1").exists());
        check(new String(Files.readAllBytes(new File(conflict, "favorites/" + saved[0].getName()).toPath())).equals("different existing file"));
        System.out.println("PASS: complete backup, favorites-only, original bytes, content dedup, renamed duplicates, v1 compatibility, traversal, validation and rollback");
    }
}
