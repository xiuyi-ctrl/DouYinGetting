package com.example.douyingetting;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Portable record backups. Extraction is validated in a staging directory before installation. */
final class RecordArchive {
    private static final String MARKER = "xiuyi-records.version";
    private static final long MAX_BYTES = 2L * 1024 * 1024 * 1024;
    interface Validator { List<String> files(File folder) throws Exception; }
    interface FavoriteValidator { String extension(File file) throws Exception; }
    static final class Result {
        final HashSet<String> added = new HashSet<>();
        int skipped;
        int favoritesAdded, favoritesSkipped;
    }
    static boolean leaf(String name) {
        return !name.isEmpty() && !name.equals(".") && !name.equals("..")
                && name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf(':') < 0;
    }
    static int write(OutputStream output, Map<String, File> records, Collection<File> favorites,
                     Validator validator, FavoriteValidator favoriteValidator) throws Exception {
        int favoriteCount = 0;
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(output))) {
            zip.putNextEntry(new ZipEntry(MARKER));
            zip.write("2\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            long total = 0;
            for (Map.Entry<String, File> record : records.entrySet()) {
                if (!record.getKey().matches("[A-Za-z0-9_-]{1,80}")) throw new IOException("记录编号无效");
                for (String name : new LinkedHashSet<>(validator.files(record.getValue()))) {
                    File file = new File(record.getValue(), name);
                    total += file.length();
                    if (total > MAX_BYTES) throw new IOException("备份超过 2 GB，请先清理不需要的记录");
                    zip.putNextEntry(new ZipEntry("records/" + record.getKey() + "/" + name));
                    try (InputStream input = new FileInputStream(file)) { copy(input, zip); }
                    zip.closeEntry();
                }
            }
            HashSet<String> hashes = new HashSet<>();
            for (File file : favorites) {
                String extension = favoriteValidator.extension(file);
                String hash = hash(file);
                if (!hashes.add(hash)) continue;
                total += file.length();
                if (total > MAX_BYTES) throw new IOException("备份超过 2 GB，请先清理不需要的素材");
                ZipEntry favorite = new ZipEntry("favorites/" + hash + extension);
                favorite.setTime(file.lastModified());
                zip.putNextEntry(favorite);
                try (InputStream input = new FileInputStream(file)) { copy(input, zip); }
                zip.closeEntry();
                favoriteCount++;
            }
            if (records.isEmpty() && favoriteCount == 0) throw new IOException("没有可导出的记录或收藏");
        }
        return favoriteCount;
    }
    static Result read(InputStream input, File staging, File destination, File favoritesDestination,
                       Set<String> known, Validator validator, FavoriteValidator favoriteValidator) throws Exception {
        Result result = new Result();
        ArrayList<File> installedFavorites = new ArrayList<>();
        try {
            if (!staging.mkdirs()) throw new IOException("无法创建导入暂存目录");
            HashSet<String> entries = new HashSet<>();
            long total = 0;
            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input))) {
                ZipEntry entry;
                byte[] buffer = new byte[65536];
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (entries.size() >= 100000 || !entries.add(name)) throw new IOException("备份条目过多或重复");
                    String[] parts = name.split("/", -1);
                    if (entry.isDirectory()) {
                        if (!(name.equals("records/") || name.equals("favorites/") || (parts.length == 3 && parts[0].equals("records")
                                && parts[1].matches("[A-Za-z0-9_-]{1,80}") && parts[2].isEmpty())))
                            throw new IOException("备份目录无效");
                        if (zip.read() != -1) throw new IOException("备份目录无效");
                        continue;
                    }
                    if (!name.equals(MARKER) && !(parts.length == 2 && parts[0].equals("favorites") && leaf(parts[1]))
                            && !(parts.length == 3 && parts[0].equals("records")
                            && parts[1].matches("[A-Za-z0-9_-]{1,80}") && leaf(parts[2])))
                        throw new IOException("备份文件路径无效");
                    File file = new File(staging, name);
                    if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) throw new IOException("无法创建目录");
                    long size = 0;
                    try (OutputStream out = new FileOutputStream(file)) {
                        int count;
                        while ((count = zip.read(buffer)) != -1) {
                            total += count; size += count;
                            if (total > MAX_BYTES || (file.getName().equals("manifest.json") && size > 4 * 1024 * 1024)
                                    || (name.equals(MARKER) && size > 32)) throw new IOException("备份过大或文件无效");
                            out.write(buffer, 0, count);
                        }
                    }
                    if (parts.length == 2 && parts[0].equals("favorites") && entry.getTime() >= 0)
                        file.setLastModified(entry.getTime());
                }
            }
            File marker = new File(staging, MARKER);
            String version = marker.isFile() ? new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim() : "";
            if (!(version.equals("1") || version.equals("2"))) throw new IOException("请选择咻咿提取导出的备份");
            File[] folders = new File(staging, "records").listFiles();
            if (folders == null) folders = new File[0];
            File[] favorites = new File(staging, "favorites").listFiles();
            if (favorites == null) favorites = new File[0];
            if ((version.equals("1") && favorites.length > 0) || (folders.length == 0 && favorites.length == 0))
                throw new IOException("备份内容无效或为空");
            for (File folder : folders) {
                List<String> expected = validator.files(folder);
                for (File file : Objects.requireNonNull(folder.listFiles()))
                    if (!expected.contains(file.getName())) throw new IOException("备份含有记录之外的文件");
            }
            LinkedHashMap<File, String> favoriteNames = new LinkedHashMap<>();
            for (File favorite : favorites) favoriteNames.put(favorite, hash(favorite) + favoriteValidator.extension(favorite));
            HashSet<String> favoriteHashes = new HashSet<>();
            File[] existing = favoritesDestination.listFiles();
            if (favorites.length > 0 && existing != null) for (File file : existing)
                if (file.isFile() && !file.getName().endsWith(".tmp")) favoriteHashes.add(hash(file));
            if (!destination.isDirectory() && !destination.mkdirs()) throw new IOException("无法创建记录目录");
            for (File folder : folders) {
                String id = folder.getName();
                File target = new File(destination, id);
                if (known.contains(id) || target.exists()) { result.skipped++; continue; }
                Files.move(folder.toPath(), target.toPath());
                result.added.add(id);
            }
            if (!favoriteNames.isEmpty() && !favoritesDestination.isDirectory() && !favoritesDestination.mkdirs())
                throw new IOException("无法创建收藏目录");
            for (Map.Entry<File, String> favorite : favoriteNames.entrySet()) {
                String hash = favorite.getValue().substring(0, 64);
                if (!favoriteHashes.add(hash)) { result.favoritesSkipped++; continue; }
                File target = new File(favoritesDestination, favorite.getValue());
                if (target.exists()) throw new IOException("收藏文件冲突，请检查本机文件后重试");
                Files.move(favorite.getKey().toPath(), target.toPath());
                installedFavorites.add(target);
                result.favoritesAdded++;
            }
            return result;
        } catch (Exception error) {
            for (File file : installedFavorites) file.delete();
            for (String id : result.added) delete(new File(destination, id));
            throw error;
        } finally { delete(staging); }
    }
    private static String hash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536]; int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
        return hex.toString();
    }
    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[65536]; int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
    }
    private static void delete(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
