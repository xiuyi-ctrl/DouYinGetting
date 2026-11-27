package com.example.douyingetting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

final class RecycleBin {
    static final long DAY = TimeUnit.DAYS.toMillis(1);
    private static final long RETENTION = 7 * DAY;
    private static final String MANIFEST = "trash.json";
    private final File directory;

    RecycleBin(File directory) {
        this.directory = directory;
    }

    static final class Entry {
        final File container;
        final JSONObject data;
        final String name;
        final boolean record;
        final File payload;
        final long expiresAt;

        Entry(File container, JSONObject data) throws JSONException {
            this.container = container;
            this.data = data;
            name = data.getString("name");
            if (!RecordArchive.leaf(name) || MANIFEST.equals(name) || "trash.tmp".equals(name))
                throw new JSONException("Invalid recycle bin file name");
            record = data.optBoolean("record");
            payload = new File(container, record ? "record" : name);
            expiresAt = data.getLong("deletedAt") + RETENTION;
        }
    }

    Entry stage(File original, JSONObject data) throws IOException, JSONException {
        Files.createDirectories(directory.toPath());
        File container = Files.createTempDirectory(directory.toPath(), "deleted-").toFile();
        try {
            data.put("name", original.getName()).put("deletedAt", System.currentTimeMillis());
            Entry entry = new Entry(container, data);
            write(entry);
            Files.move(original.toPath(), entry.payload.toPath());
            return entry;
        } catch (IOException | JSONException error) {
            removeManifest(container);
            throw error;
        }
    }

    ArrayList<Entry> entries(long now) {
        ArrayList<Entry> entries = new ArrayList<>();
        File[] containers = directory.listFiles(File::isDirectory);
        if (containers == null) return entries;
        for (File container : containers) {
            try {
                File manifest = new File(container, MANIFEST);
                JSONObject data = new JSONObject(new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8));
                Entry entry = new Entry(container, data);
                if (now >= entry.expiresAt) purge(container);
                else if (entry.record ? entry.payload.isDirectory() : entry.payload.isFile()) entries.add(entry);
            } catch (IOException | JSONException error) {
                // Older staging directories have no original record identity.
                if (now - container.lastModified() >= RETENTION) purge(container);
            }
        }
        entries.sort((first, second) -> Long.compare(second.expiresAt, first.expiresAt));
        return entries;
    }

    void restore(Entry entry, File target) throws IOException {
        if (System.currentTimeMillis() >= entry.expiresAt) throw new IOException("回收期限已过");
        if (target.exists()) throw new IOException("原位置已有同一项，回收站中的副本仍会保留");
        Files.createDirectories(target.getParentFile().toPath());
        Files.move(entry.payload.toPath(), target.toPath());
        removeManifest(entry.container);
    }

    void renameGroup(String from, String to) {
        for (Entry entry : entries(System.currentTimeMillis())) {
            if (entry.record || !from.equals(entry.data.optString("group"))) continue;
            try {
                entry.data.put("group", to);
                write(entry);
            } catch (IOException | JSONException ignored) { }
        }
    }

    void discard(Entry entry) throws IOException, JSONException {
        entry.data.put("deletedAt", System.currentTimeMillis() - RETENTION);
        write(entry);
        purge(entry.container);
    }

    private void write(Entry entry) throws IOException {
        File temporary = new File(entry.container, "trash.tmp");
        Files.write(temporary.toPath(), entry.data.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(temporary.toPath(), new File(entry.container, MANIFEST).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void removeManifest(File container) {
        new File(container, MANIFEST).delete();
        new File(container, "trash.tmp").delete();
        container.delete();
    }

    private void purge(File container) {
        File[] files = container.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (MANIFEST.equals(file.getName())) continue;
            if (file.isDirectory() && "record".equals(file.getName())) {
                File[] media = file.listFiles();
                if (media == null) return;
                for (File item : media) if (!item.isFile() || !item.delete()) return;
            }
            if (!file.delete()) return;
        }
        removeManifest(container);
    }
}
