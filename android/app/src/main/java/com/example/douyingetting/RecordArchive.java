package com.example.douyingetting;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

/** Portable record backups. Extraction is validated in a staging directory before installation. */
final class RecordArchive {
    private static final String MARKER = "xiuyi-records.version";
    private static final long MAX_BYTES = 2L * 1024 * 1024 * 1024;
    interface Validator { List<String> files(File folder) throws Exception; }
    static final class Result {
        final HashSet<String> added = new HashSet<>();
        int skipped;
    }
    static boolean leaf(String name) {
        return !name.isEmpty() && !name.equals(".") && !name.equals("..")
                && name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf(':') < 0;
    }
    static void write(OutputStream output, Map<String, File> records, Validator validator) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(output))) {
            zip.putNextEntry(new ZipEntry(MARKER));
            zip.write("1\n".getBytes(StandardCharsets.UTF_8));
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
        }
    }
    static Result read(InputStream input, File staging, File destination, Set<String> known,
                       Validator validator) throws Exception {
        Result result = new Result();
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
                        if (!(name.equals("records/") || (parts.length == 3 && parts[0].equals("records")
                                && parts[1].matches("[A-Za-z0-9_-]{1,80}") && parts[2].isEmpty())))
                            throw new IOException("备份目录无效");
                        continue;
                    }
                    if (!name.equals(MARKER) && !(parts.length == 3 && parts[0].equals("records")
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
                }
            }
            File marker = new File(staging, MARKER);
            if (!marker.isFile() || !new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim().equals("1"))
                throw new IOException("请选择咻咿提取导出的记录备份");
            File[] folders = new File(staging, "records").listFiles();
            if (folders == null || folders.length == 0) throw new IOException("备份中没有扫描记录");
            for (File folder : folders) {
                List<String> expected = validator.files(folder);
                for (File file : Objects.requireNonNull(folder.listFiles()))
                    if (!expected.contains(file.getName())) throw new IOException("备份含有记录之外的文件");
            }
            if (!destination.isDirectory() && !destination.mkdirs()) throw new IOException("无法创建记录目录");
            for (File folder : folders) {
                String id = folder.getName();
                File target = new File(destination, id);
                if (known.contains(id) || target.exists()) { result.skipped++; continue; }
                Files.move(folder.toPath(), target.toPath());
                result.added.add(id);
            }
            return result;
        } catch (Exception error) {
            for (String id : result.added) delete(new File(destination, id));
            throw error;
        } finally { delete(staging); }
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
