package com.example.douyingetting;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BatchExtraction {
    private static final Pattern URL = Pattern.compile("https://[^\\s<>\"\\u3000，。；、！？（）【】]+",
            Pattern.CASE_INSENSITIVE);
    static final String[] PLATFORMS = {"抖音", "小红书", "快手"};

    enum State {
        QUEUED("待提取"), RUNNING("提取中"), ACTION("需要处理"), DONE("已完成"),
        PARTIAL("部分失败"), SKIPPED("已跳过");

        final String label;
        State(String label) { this.label = label; }
    }

    static final class Task {
        final String link;
        final int platform;
        final String key;
        boolean comments;
        State state = State.QUEUED;
        String detail = "";
        String recordId = "";

        Task(String link, int platform, String key) {
            this.link = link;
            this.platform = platform;
            this.key = key;
        }
    }

    final ArrayList<Task> tasks = new ArrayList<>();

    int add(String text) {
        int added = 0;
        Matcher urls = URL.matcher(text.replace("\\:", ":"));
        while (urls.find()) {
            Task task = parse(urls.group().replaceFirst("[,.;!?')\\]]+$", ""));
            if (task == null || tasks.stream().anyMatch(item -> item.key.equals(task.key)
                    && (item.state == State.QUEUED || item.state == State.RUNNING || item.state == State.ACTION))) continue;
            tasks.add(task);
            added++;
        }
        return added;
    }

    private static Task parse(String link) {
        try {
            Uri uri = Uri.parse(link);
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) return null;
            host = host.toLowerCase(Locale.ROOT);
            if ("douyin.com".equals(host)) host = "www.douyin.com";
            String path = uri.getEncodedPath();
            if (path == null || path.length() < 2) return null;
            link = uri.buildUpon().scheme("https").encodedAuthority(host).fragment(null).build().toString();
            int platform;
            if (WorkMedia.isShareLink(link, false)) platform = 0;
            else if (WorkMedia.isShareLink(link, true)) platform = 1;
            else if (link.equals(KuaishouPost.extractLink(link))) platform = 2;
            else return null;
            String id = platform == 2 ? null : WorkMedia.pageId(link, platform == 1);
            String key = platform + ":" + (id == null ? host + path.replaceFirst("/+$", "") : id);
            return new Task(link, platform, key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    Task next() {
        for (Task task : tasks) {
            if (task.state == State.QUEUED || task.state == State.ACTION) return task;
        }
        return null;
    }

    String encode() {
        JSONArray data = new JSONArray();
        for (Task task : tasks) {
            try {
                data.put(new JSONObject().put("link", task.link).put("comments", task.comments)
                        .put("state", task.state.name()).put("detail", task.detail).put("record", task.recordId));
            } catch (JSONException error) {
                throw new IllegalStateException(error);
            }
        }
        return data.toString();
    }

    void restore(String saved) {
        try {
            JSONArray data = new JSONArray(saved);
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.optJSONObject(i);
                if (row == null) continue;
                Task task = parse(row.optString("link"));
                if (task == null) continue;
                task.comments = task.platform != 2 && row.optBoolean("comments");
                task.recordId = row.optString("record");
                task.detail = row.optString("detail");
                try { task.state = State.valueOf(row.optString("state")); }
                catch (IllegalArgumentException ignored) { task.state = State.QUEUED; }
                if (task.state == State.RUNNING) {
                    task.state = State.ACTION;
                    task.detail = "上次提取已中断，可以继续";
                }
                tasks.add(task);
            }
        } catch (JSONException ignored) { }
    }
}
