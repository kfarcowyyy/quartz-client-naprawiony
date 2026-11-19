package com.quartz.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuartzConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    public static Data data = new Data();

    private QuartzConfig() {}

    public static final class Data {
        public String language = "en";
        public Map<String, Hud> modules = new LinkedHashMap<>();
        public boolean classicMenu = false;
        public int hudX = 12;
        public int hudY = 12;
        public int background = 0xB0101732;
        public int foreground = 0xFFF3F5FF;
        public String hudText;
    }

    static final class Hud {
        boolean enabled, background = true, showValue = true, shadow = true, brackets;
        int bg = 0xB0101732, fg = 0xFFF3F5FF;
        int x, y;
        Integer oldX, oldY;
        String text, target;

        Hud() {}

        Hud(QuartzHudModule module) {
            enabled = module.initiallyEnabled;
            x = module.x;
            y = module.y;
            text = module.template;
            if (module.icons()) {
                bg = 0x77000000;
                fg = 0xFFFFFFFF;
            }
        }
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
        if (data.modules == null) data.modules = new LinkedHashMap<>();
        boolean upgrade = data.modules.isEmpty() && data.hudText != null;
        for (QuartzHudModule module : QuartzHudModule.values()) {
            if (data.modules.get(module.name()) != null) continue;
            Hud setting = new Hud(module);
            if (upgrade && (module == QuartzHudModule.FPS || module == QuartzHudModule.CPS)) {
                setting.oldX = data.hudX;
                setting.oldY = data.hudY + (module == QuartzHudModule.CPS ? 21 : 0);
                setting.bg = data.background;
                setting.fg = data.foreground;
                String token = module == QuartzHudModule.FPS ? "{fps}" : "{cps}";
                String other = module == QuartzHudModule.FPS ? "{cps}" : "{fps}";
                if (data.hudText.contains(token) && !data.hudText.contains(other)) {
                    setting.text = data.hudText.replace(token, "{value}");
                }
            }
            data.modules.put(module.name(), setting);
        }
        save();
    }

    public static void save() {
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(data));
        } catch (Exception ignored) { }
    }

    public static void recordClick(int button) {
        long now = System.nanoTime();
        ArrayDeque<Long> clicks = button == 0 ? LEFT : button == 1 ? RIGHT : null;
        if (clicks == null) return;
        clicks.addLast(now);
        trim(clicks, now);
    }

    private static void trim(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() >= 1_000_000_000L) clicks.removeFirst();
    }

    public static String cps() {
        long now = System.nanoTime();
        trim(LEFT, now);
        trim(RIGHT, now);
        return LEFT.size() + " | " + RIGHT.size();
    }
}
