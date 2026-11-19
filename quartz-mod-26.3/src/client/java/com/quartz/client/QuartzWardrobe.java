package com.quartz.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class QuartzWardrobe {
    record Selection(String skin) {}
    record Skin(String id, String name, boolean slim) {}
    record Cape(String name, boolean active) {}
    record Metadata(String name) {}
    private static final Gson JSON = new Gson();
    private static final Pattern ID = Pattern.compile("[0-9a-f]{32}-(wide|slim)");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("quartz", "imported_skin");
    private static String applied = "account";
    private static PlayerModelType model = PlayerModelType.WIDE;
    private static boolean registered;
    private static int ticks;

    private QuartzWardrobe() {}

    private static Path file() {
        String shared = System.getProperty("quartz.wardrobe");
        return shared == null ? Minecraft.getInstance().gameDirectory.toPath().resolve("config/quartz-wardrobe.json")
                : Path.of(URI.create(shared));
    }

    private static Path sibling(String suffix) {
        Path base = file();
        String name = base.getFileName().toString();
        return base.resolveSibling(name.substring(0, name.length() - 5) + suffix);
    }

    static Path skinFolder() { return sibling("-skins"); }

    private static Path skinFile(String id) {
        if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException("Unknown skin.");
        return skinFolder().resolve(id + ".png");
    }

    static String selected() {
        try {
            var selection = JSON.fromJson(Files.readString(file()), Selection.class);
            if (selection != null && selection.skin() != null && ID.matcher(selection.skin()).matches()
                    && Files.isRegularFile(skinFile(selection.skin()))) return selection.skin();
        } catch (IOException | RuntimeException ignored) { }
        return "account";
    }

    static List<Skin> skins() {
        if (!Files.isDirectory(skinFolder())) return List.of();
        try (var files = Files.list(skinFolder())) {
            return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".png")).map(name -> name.substring(0, name.length() - 4))
                    .filter(id -> ID.matcher(id).matches()).sorted().map(QuartzWardrobe::skin).toList();
        } catch (IOException error) { return List.of(); }
    }

    private static Skin skin(String id) {
        String name = "Skin " + id.substring(0, 6);
        try {
            var entry = JSON.fromJson(Files.readString(skinFolder().resolve(id + ".json")), Metadata.class);
            if (entry != null && entry.name() != null) name = entry.name();
        } catch (IOException | RuntimeException ignored) { }
        return new Skin(id, name, id.endsWith("-slim"));
    }

    static List<Cape> capes() {
        try {
            List<Cape> values = JSON.fromJson(Files.readString(sibling("-capes.json")), new TypeToken<List<Cape>>() {}.getType());
            return values == null ? List.of() : values;
        } catch (IOException | RuntimeException ignored) { return List.of(); }
    }

    static void equip(String id) throws IOException {
        if (!"account".equals(id) && !Files.isRegularFile(skinFile(id))) throw new IOException("Unknown skin");
        Path file = file();
        Files.createDirectories(file.getParent());
        Path pending = Files.createTempFile(file.getParent(), "wardrobe-", ".tmp");
        try {
            Files.writeString(pending, JSON.toJson(new Selection(id)));
            try {
                Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(pending); }
        refresh();
    }

    static void importPng(Path source, boolean slim) throws IOException {
        String name = source.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".png") || Files.size(source) > 1024 * 1024) {
            throw new IOException("Select a Minecraft PNG");
        }
        try (var stream = Files.newInputStream(source)) {
            byte[] header = stream.readNBytes(24);
            if (header.length != 24 || header[0] != (byte) 137 || header[1] != 80 || header[2] != 78 || header[3] != 71
                    || java.nio.ByteBuffer.wrap(header).getInt(16) != 64 || java.nio.ByteBuffer.wrap(header).getInt(20) != 64) {
                throw new IOException("Minecraft skin must be 64 × 64");
            }
        }
        String id = UUID.randomUUID().toString().replace("-", "") + (slim ? "-slim" : "-wide");
        Path target = skinFile(id);
        Files.createDirectories(target.getParent());
        try (var stream = Files.newInputStream(source); NativeImage image = NativeImage.read(stream)) {
            if (image.getWidth() != 64 || image.getHeight() != 64) throw new IOException("Minecraft skin must be 64 × 64");
            image.writeToFile(target);
        }
        try {
            String label = name.substring(0, name.length() - 4);
            Files.writeString(target.resolveSibling(id + ".json"), JSON.toJson(new Metadata(label.substring(0, Math.min(label.length(), 80)))));
        } catch (IOException error) {
            Files.deleteIfExists(target);
            throw error;
        }
    }

    static void delete(String id) throws IOException {
        Path file = skinFile(id);
        if (id.equals(selected())) equip("account");
        Files.deleteIfExists(file);
        Files.deleteIfExists(file.resolveSibling(id + ".json"));
    }

    private static void refresh() {
        String id = selected();
        if (applied.equals(id)) return;
        var textures = Minecraft.getInstance().getTextureManager();
        if (registered) {
            textures.release(TEXTURE);
            registered = false;
        }
        applied = id;
        if ("account".equals(id)) return;
        try (var stream = Files.newInputStream(skinFile(id))) {
            NativeImage image = NativeImage.read(stream);
            if (image.getWidth() != 64 || image.getHeight() != 64) { image.close(); return; }
            textures.register(TEXTURE, new DynamicTexture(() -> "Quartz imported skin", image));
            model = id.endsWith("-slim") ? PlayerModelType.SLIM : PlayerModelType.WIDE;
            registered = true;
        } catch (IOException | RuntimeException ignored) { }
    }

    static void tick() {
        if (++ticks >= 20) {
            ticks = 0;
            refresh();
        }
    }

    public static PlayerSkin apply(PlayerSkin account) {
        if (!registered || account == null) return account;
        return new PlayerSkin(new ClientAsset.ResourceTexture(TEXTURE, TEXTURE), account.cape(), account.elytra(),
                model, account.secure());
    }
}
