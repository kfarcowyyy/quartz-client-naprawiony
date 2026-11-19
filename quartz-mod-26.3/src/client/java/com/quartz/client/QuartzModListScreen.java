package com.quartz.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

public class QuartzModListScreen extends QuartzSafeScreen {
    private record Mod(Path file, String name, boolean enabled, boolean managed) {}
    private final Screen parent;
    private final List<Mod> mods = new ArrayList<>();
    private int page, pages, columns, rows, left, bodyWidth, sidebar, cardWidth, limit;
    private enum Filter { ALL, ENABLED, DISABLED }
    private Filter filter = Filter.ALL;
    private Component status;

    public QuartzModListScreen(Screen parent) {
        super(QuartzText.of("Mods", "Mody"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        scan();
        left = 12;
        sidebar = width < 390 ? 78 : 104;
        bodyWidth = width - 3 * left - sidebar;
        columns = bodyWidth >= 350 ? 3 : bodyWidth >= 200 ? 2 : 1;
        cardWidth = (bodyWidth - 8 * (columns - 1)) / columns;
        rows = Math.max(1, (height - 103) / 60);
        limit = rows * columns;
        pages = Math.max(1, (mods.size() + limit - 1) / limit);
        page = Math.min(page, pages - 1);
        nav(QuartzText.of("Mods", "Mody"), 45, true, () -> {});
        nav(QuartzText.of("HUD layout", "Układ HUD"), 69, false, () -> open(new QuartzHudEditorScreen(this)));
        nav(QuartzText.of("HUD settings", "Ustawienia HUD"), 93, false, () -> open(new QuartzSettingsScreen(this)));
        nav(QuartzText.of("Wardrobe", "Garderoba"), 117, false, () -> open(new QuartzWardrobeScreen(this)));
        nav(QuartzText.of("Packs & shaders", "Paczki i shadery"), 141, false, () -> open(new QuartzPacksScreen(this)));
        nav(QuartzText.of("Screenshots", "Zrzuty ekranu"), 165, false, () -> open(new QuartzScreenshotScreen(this)));
        add(Component.literal("×"), width - left - 24, 12, 24, 20, false, this::onClose);
        int contentLeft = left * 2 + sidebar;
        int pill = (bodyWidth - 12) / 3;
        filter(QuartzText.of("All", "Wszystkie"), contentLeft, pill, Filter.ALL);
        filter(QuartzText.of("Enabled", "Włączone"), contentLeft + pill + 6, pill, Filter.ENABLED);
        filter(QuartzText.of("Disabled", "Wyłączone"), contentLeft + (pill + 6) * 2, pill, Filter.DISABLED);
        add(Component.literal("‹"), contentLeft, height - 28, 23, 18, false, () -> move(-1)).active = page > 0;
        add(Component.literal("›"), contentLeft + 58, height - 28, 23, 18, false, () -> move(1)).active = page + 1 < pages;
        for (int i = 0; i < limit && page * limit + i < mods.size(); i++) {
            Mod item = mods.get(page * limit + i);
            int x = contentLeft + i % columns * (cardWidth + 8), y = 68 + i / columns * 60;
            Component label = item.managed() ? QuartzText.of("Built in", "Wbudowany")
                    : item.enabled() ? QuartzText.of("Disable", "Wyłącz") : QuartzText.of("Enable", "Włącz");
            add(label, x + 7, y + 31, cardWidth - 14, 17, false, () -> toggle(item)).active = !item.managed();
        }
    }

    private void scan() {
        mods.clear();
        Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve("mods");
        if (!Files.isDirectory(folder)) return;
        try (var paths = Files.list(folder)) {
            paths.filter(Files::isRegularFile).sorted().forEach(file -> {
                String name = file.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".jar") && !lower.endsWith(".jar.disabled")) return;
                boolean managed = lower.startsWith("quartz-client-managed") || lower.startsWith("quartz-fabric-api-managed");
                boolean enabled = lower.endsWith(".jar");
                if (filter == Filter.ENABLED && !enabled || filter == Filter.DISABLED && enabled) return;
                mods.add(new Mod(file, modName(file, name), enabled, managed));
            });
        } catch (Exception error) {
            status = QuartzText.of("Could not open the mods folder.", "Nie udało się odczytać folderu mods.");
        }
    }

    private static String modName(Path file, String name) {
        try (var zip = new ZipFile(file.toFile())) {
            var metadata = zip.getEntry("fabric.mod.json");
            if (metadata != null && metadata.getSize() >= 0 && metadata.getSize() <= 128 * 1024) {
                try (var json = new InputStreamReader(zip.getInputStream(metadata), StandardCharsets.UTF_8)) {
                    var label = JsonParser.parseReader(json).getAsJsonObject().get("name");
                    if (label != null && label.isJsonPrimitive()) return label.getAsString();
                }
            }
        } catch (Exception ignored) { }
        return name.replaceFirst("(?i)\\.jar(?:\\.disabled)?$", "");
    }

    private void filter(Component label, int x, int w, Filter next) {
        add(label, x, 42, w, 19, filter == next, () -> {
            filter = next;
            page = 0;
            rebuildWidgets();
        });
    }

    private void move(int amount) { page += amount; rebuildWidgets(); }

    private void toggle(Mod mod) {
        String name = mod.file().getFileName().toString();
        try {
            Path target = mod.file().resolveSibling(mod.enabled() ? name + ".disabled" : name.substring(0, name.length() - 9));
            Files.move(mod.file(), target);
            status = QuartzText.of("Restart Minecraft to apply the change.", "Uruchom grę ponownie, aby zastosować zmianę.");
            rebuildWidgets();
        } catch (Exception error) {
            status = QuartzText.of("Could not change the mod.", "Nie udało się zmienić stanu moda.");
        }
    }

    private void nav(Component label, int y, boolean accent, Runnable action) {
        if (y + 20 <= height - 12) add(label, left + 5, y, sidebar - 10, 20, accent, action);
    }

    private QuartzButton add(Component text, int x, int y, int w, int h, boolean accent, Runnable action) {
        return addRenderableWidget(new QuartzButton(text, x, y, w, h, accent, action));
    }

    private void open(Screen screen) { Minecraft.getInstance().gui.setScreen(screen); }
    @Override public void onClose() { open(parent); }

    @Override
    protected void drawQuartz(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        QuartzTheme.background(g, width, height);
        QuartzTheme.card(g, left, 9, width - 2 * left, 25, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
        QuartzTheme.card(g, left, 40, sidebar, height - 50, 0xFFFCF4FE, QuartzTheme.OUTLINE);
        QuartzTheme.crystal(g, left + 6, 11, 14, 21);
        g.text(font, "Quartz Client", left + 26, 18, QuartzTheme.ACCENT, false);
        int contentLeft = left * 2 + sidebar;
        for (int i = 0; i < limit && page * limit + i < mods.size(); i++) {
            Mod mod = mods.get(page * limit + i);
            int x = contentLeft + i % columns * (cardWidth + 8), y = 68 + i / columns * 60;
            QuartzTheme.card(g, x, y, cardWidth, 54, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
            g.text(font, font.plainSubstrByWidth(mod.name(), cardWidth - 18), x + 8, y + 8, QuartzTheme.INK, false);
            g.text(font, mod.enabled() ? QuartzText.of("Enabled", "Włączony") : QuartzText.of("Disabled", "Wyłączony"),
                    x + 8, y + 20, mod.enabled() ? QuartzTheme.ACCENT : QuartzTheme.MUTED, false);
        }
        String footer = status == null ? mods.isEmpty() ? QuartzText.of("No matching mods.", "Brak modów w tym widoku.").getString() : "" : status.getString();
        g.text(font, (page + 1) + "/" + pages, contentLeft + 28, height - 23, QuartzTheme.MUTED, false);
        g.text(font, font.plainSubstrByWidth(footer, Math.max(0, bodyWidth - 88)), contentLeft + 86, height - 23, QuartzTheme.MUTED, false);
        drawWidgets(g, mouseX, mouseY, delta);
    }
}
