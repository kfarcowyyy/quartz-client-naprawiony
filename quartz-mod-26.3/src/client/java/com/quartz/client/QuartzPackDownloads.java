package com.quartz.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.zip.ZipFile;

final class QuartzPackDownloads {
    enum Kind {
        RESOURCES("resourcepack", "resourcepacks"), SHADERS("shader", "shaderpacks");
        final String type, folder;
        Kind(String type, String folder) { this.type = type; this.folder = folder; }
    }

    record Project(String id, String title) {}
    record Results(List<Project> items, int total) {}
    private static final long MAX_DOWNLOAD = 128L * 1024 * 1024;
    private static final String VERSION = "26.3";
    private static final String ROOT = "https://api.modrinth.com/v2";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private static final java.util.concurrent.Executor IO = Executors.newVirtualThreadPerTaskExecutor();

    private QuartzPackDownloads() {}

    static CompletableFuture<Results> search(Kind kind, String query, int offset, int size) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String facets = "[[\"project_type:" + kind.type + "\"],[\"versions:" + VERSION + "\"]]";
                JsonObject response = get(ROOT + "/search?limit=" + size + "&offset=" + offset + "&query="
                        + enc(query) + "&facets=" + enc(facets)).getAsJsonObject();
                List<Project> projects = new ArrayList<>();
                for (var hit : response.getAsJsonArray("hits")) {
                    JsonObject row = hit.getAsJsonObject();
                    String id = row.get("project_id").getAsString();
                    if (id.matches("[A-Za-z0-9]{1,32}")) projects.add(new Project(id, row.get("title").getAsString()));
                }
                return new Results(projects, response.get("total_hits").getAsInt());
            } catch (Exception error) { throw new CompletionException(error); }
        }, IO);
    }

    static CompletableFuture<String> install(Kind kind, Project project, Path instance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonArray versions = get(ROOT + "/project/" + project.id() + "/version?game_versions="
                        + enc("[\"" + VERSION + "\"]")).getAsJsonArray();
                Path folder = instance.resolve(kind.folder);
                Files.createDirectories(folder);
                for (var version : versions) {
                    var files = version.getAsJsonObject().getAsJsonArray("files");
                    JsonObject candidate = null;
                    for (var file : files) {
                        JsonObject row = file.getAsJsonObject();
                        if (!row.get("filename").getAsString().toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
                        if (candidate == null || row.get("primary").getAsBoolean()) candidate = row;
                    }
                    if (candidate != null) return download(candidate, folder, kind);
                }
                throw new IOException("No compatible ZIP from Modrinth.");
            } catch (Exception error) { throw new CompletionException(error); }
        }, IO);
    }

    private static String download(JsonObject info, Path folder, Kind kind) throws Exception {
        URI uri = URI.create(info.get("url").getAsString());
        if (!"https".equals(uri.getScheme()) || !"cdn.modrinth.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null || uri.getPort() != -1) throw new IOException("Unsupported pack host.");
        long size = info.get("size").getAsLong();
        String hash = info.getAsJsonObject("hashes").get("sha512").getAsString().toLowerCase(Locale.ROOT);
        if (!hash.matches("[a-f0-9]{128}") || size <= 0 || size > MAX_DOWNLOAD) throw new IOException("Unsupported pack size or hash.");
        String safeName = info.get("filename").getAsString().replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeName.length() > 100) safeName = safeName.substring(0, 96) + ".zip";
        Path target = folder.resolve("quartz-" + hash.substring(0, 12) + "-" + safeName);
        Path pending = Files.createTempFile(folder, ".quartz-download-", ".tmp");
        try {
            var response = HTTP.send(request(uri, 45), HttpResponse.BodyHandlers.ofInputStream());
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            try (InputStream body = response.body();
                 var checked = new DigestInputStream(body, digest);
                 var output = Files.newOutputStream(pending)) {
                if (response.statusCode() != 200) throw new IOException("Pack HTTP " + response.statusCode());
                byte[] bytes = new byte[64 * 1024];
                long read = 0;
                for (int n; (n = checked.read(bytes)) != -1;) {
                    read += n;
                    if (read > size) throw new IOException("Pack exceeds its declared size.");
                    output.write(bytes, 0, n);
                }
                if (read != size || !HexFormat.of().formatHex(digest.digest()).equals(hash)) {
                    throw new IOException("Pack integrity check failed.");
                }
            }
            try (var zip = new ZipFile(pending.toFile())) {
                boolean valid = kind == Kind.RESOURCES ? zip.getEntry("pack.mcmeta") != null
                        : zip.stream().anyMatch(entry -> entry.getName().startsWith("shaders/"));
                if (!valid) throw new IOException("The ZIP is not a valid pack of this type.");
            }
            Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
            return target.getFileName().toString();
        } finally { Files.deleteIfExists(pending); }
    }

    private static HttpRequest request(URI uri, int timeout) {
        return HttpRequest.newBuilder(uri).header("User-Agent", "kfarcowyyy/quartz-client/0.2.4")
                .timeout(Duration.ofSeconds(timeout)).GET().build();
    }

    private static com.google.gson.JsonElement get(String url) throws IOException, InterruptedException {
        var response = HTTP.send(request(URI.create(url), 12), HttpResponse.BodyHandlers.ofInputStream());
        try (var input = response.body()) {
            if (response.statusCode() != 200) throw new IOException("Modrinth HTTP " + response.statusCode());
            byte[] bytes = input.readNBytes(4 * 1024 * 1024 + 1);
            if (bytes.length > 4 * 1024 * 1024) throw new IOException("Modrinth response too large.");
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private static String enc(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
}
