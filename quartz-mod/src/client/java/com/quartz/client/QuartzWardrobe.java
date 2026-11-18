package com.quartz.client;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
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

public final class QuartzWardrobe {
    public record Item(String id, String english, String polish) {}
    public record Equipped(String skin, String cape, String accessory) {}

    public static final List<Item> SKINS = List.of(
            new Item("account", "Account skin", "Skin konta"),
            new Item("amethyst", "Amethyst scout", "Ametystowy zwiadowca"),
            new Item("aurora", "Aurora explorer", "Zorza polarna"),
            new Item("ember", "Ember ranger", "Wędrowiec ognia"));
    public static final List<Item> CAPES = List.of(
            new Item("none", "Account cape", "Peleryna konta"),
            new Item("crystal", "Quartz crest", "Herb kwarcu"),
            new Item("night", "Starfall", "Deszcz gwiazd"),
            new Item("sunrise", "Sunrise", "Wschód słońca"));
    public static final List<Item> ACCESSORIES = List.of(
            new Item("none", "None", "Brak"),
            new Item("crown", "Quartz crown", "Korona z kwarcu"),
            new Item("visor", "Starlight visor", "Gwiezdne gogle"));

    private static final Gson GSON = new Gson();
    private static final Equipped DEFAULT = new Equipped("account", "none", "none");
    private static Equipped active = DEFAULT;
    private static ClientAsset.Texture skinTexture;
    private static ClientAsset.Texture capeTexture;
    private static int ticks;

    private QuartzWardrobe() {}

    private static Path file() {
        String sharedFile = System.getProperty("quartz.wardrobe");
        return sharedFile == null
                ? Minecraft.getInstance().gameDirectory.toPath().resolve("config/quartz-wardrobe.json")
                : Path.of(URI.create(sharedFile));
    }

    private static String valid(List<Item> options, String value) {
        return options.stream().anyMatch(item -> item.id().equals(value)) ? value : options.getFirst().id();
    }

    private static ClientAsset.ResourceTexture texture(String path) {
        Identifier id = Identifier.fromNamespaceAndPath("quartz", "textures/wardrobe/" + path + ".png");
        return new ClientAsset.ResourceTexture(id, id);
    }

    private static void update(Equipped next) {
        if (active.equals(next)) return;
        active = next;
        skinTexture = "account".equals(next.skin()) ? null : texture("skin-" + next.skin() + "-" + next.accessory());
        capeTexture = "none".equals(next.cape()) ? null : texture("cape-" + next.cape());
    }

    public static void refresh() {
        try {
            Path file = file();
            if (!Files.exists(file)) {
                update(DEFAULT);
                return;
            }
            Equipped stored = GSON.fromJson(Files.readString(file), Equipped.class);
            if (stored == null) return;
            String skin = valid(SKINS, stored.skin());
            String accessory = valid(ACCESSORIES, stored.accessory());
            if ("account".equals(skin) && !"none".equals(accessory)) skin = "amethyst";
            update(new Equipped(skin, valid(CAPES, stored.cape()), accessory));
        } catch (IOException | RuntimeException error) {
            // Keep the last readable selection if another process is saving it.
        }
    }

    public static void tick() {
        if (++ticks >= 20) {
            ticks = 0;
            refresh();
        }
    }

    public static Equipped equipped() { return active; }

    public static void equip(Equipped choice) throws IOException {
        String skin = "account".equals(choice.skin()) && !"none".equals(choice.accessory()) ? "amethyst" : choice.skin();
        Equipped next = new Equipped(skin, choice.cape(), choice.accessory());
        Path file = file();
        Files.createDirectories(file.getParent());
        Path pending = Files.createTempFile(file.getParent(), "wardrobe-", ".tmp");
        try {
            Files.writeString(pending, GSON.toJson(next));
            try {
                Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
            }
            update(next);
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    public static PlayerSkin apply(PlayerSkin original) {
        if (skinTexture == null && capeTexture == null) return original;
        return new PlayerSkin(skinTexture == null ? original.body() : skinTexture,
                capeTexture == null ? original.cape() : capeTexture,
                original.elytra(), skinTexture == null ? original.model() : PlayerModelType.WIDE, original.secure());
    }
}
