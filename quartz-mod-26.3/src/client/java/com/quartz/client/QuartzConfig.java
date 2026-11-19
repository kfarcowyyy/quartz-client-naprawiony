package com.quartz.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;

public final class QuartzConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    static final String DEFAULT_HUD_TEXT = "FPS {fps}  |  CPS {cps}";
    public static Data data = new Data();

    private QuartzConfig() {}

    public static final class Data {
        public String language = "en";
        public boolean hudEnabled = true;
        public boolean classicMenu = false;
        public int hudX = 12;
        public int hudY = 12;
        public int background = 0xB0101732;
        public int foreground = 0xFFF3F5FF;
        public String hudText = DEFAULT_HUD_TEXT;
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/quartz-client.json");
    }

    public static void load() {
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) data = GSON.fromJson(Files.readString(file), Data.class);
            if (data == null) data = new Data();
        } catch (Exception ignored) {
            data = new Data();
        }
        data.hudText = hudTemplate();
        save();
    }

    static String hudTemplate() {
        String value = data.hudText;
        if (value == null || value.isBlank()) return DEFAULT_HUD_TEXT;
        return value.length() <= 72 ? value : value.substring(0, 72);
    }

    public static void save() {
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(data));
        } catch (Exception ignored) { }
    }

    public static void recordClick(int button) {
        long now = System.currentTimeMillis();
        ArrayDeque<Long> clicks = button == 0 ? LEFT : button == 1 ? RIGHT : null;
        if (clicks == null) return;
        clicks.addLast(now);
        trim(clicks, now);
    }

    private static void trim(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
    }

    public static String cps() {
        long now = System.currentTimeMillis();
        trim(LEFT, now);
        trim(RIGHT, now);
        return LEFT.size() + " | " + RIGHT.size();
    }
}
