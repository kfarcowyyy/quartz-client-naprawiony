package com.quartz.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
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
    private record Mod(Path file, String name, boolean enabled, boolean managed, QuartzHudModule module) {}
    private final Screen parent;
    private final List<Mod> mods = new ArrayList<>();
    private int firstRow, lastRow, columns, rows, left, bodyWidth, sidebar, cardWidth, limit, contentLeft;
    private static final int ROW = 79;
    private boolean installed;
    private String query = "";
    private EditBox search;
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
        rows = Math.max(1, (height - 92) / ROW);
        limit = rows * columns;
        lastRow = Math.max(0, (mods.size() + columns - 1) / columns - rows);
        firstRow = Math.min(firstRow, lastRow);
        nav(QuartzText.of("Quartz modules", "Moduły Quartz"), 43, !installed, () -> changeList(false));
        nav(QuartzText.of("Installed mods", "Zainstalowane mody"), 65, installed, () -> changeList(true));
        nav(QuartzText.of("HUD layout", "Układ HUD"), 87, false, () -> open(new QuartzHudEditorScreen(this)));
        nav(QuartzText.of("Settings", "Ustawienia"), 109, false, () -> open(new QuartzSettingsScreen(this)));
        nav(QuartzText.of("Wardrobe", "Garderoba"), 131, false, () -> open(new QuartzWardrobeScreen(this)));
        nav(QuartzText.of("Packs & shaders", "Paczki i shadery"), 153, false, () -> open(new QuartzPacksScreen(this)));
        nav(QuartzText.of("Screenshots", "Zrzuty ekranu"), 175, false, () -> open(new QuartzScreenshotScreen(this)));
        add(Component.literal("×"), width - left - 24, 12, 24, 20, false, this::onClose);
        int searchWidth = Math.min(190, width - 165);
        search = addRenderableWidget(new EditBox(font, width - left - 33 - searchWidth, 13, searchWidth, 17,
                QuartzText.of("Search mods", "Szukaj modów")));
        search.setMaxLength(64);
        search.setValue(query);
        search.setHint(QuartzText.of("Find a module…", "Znajdź moduł…"));
        search.setResponder(value -> {
            query = value;
            firstRow = 0;
            rebuildWidgets();
            setFocused(search);
        });
        contentLeft = left * 2 + sidebar;
        int pill = (bodyWidth - 12) / 3;
        filter(QuartzText.of("All", "Wszystkie"), contentLeft, pill, Filter.ALL);
        filter(QuartzText.of("Enabled", "Włączone"), contentLeft + pill + 6, pill, Filter.ENABLED);
        filter(QuartzText.of("Disabled", "Wyłączone"), contentLeft + (pill + 6) * 2, pill, Filter.DISABLED);
        add(Component.literal("↑"), contentLeft, height - 21, 19, 15, false, () -> move(-1)).active = firstRow > 0;
        add(Component.literal("↓"), contentLeft + 23, height - 21, 19, 15, false, () -> move(1)).active = firstRow < lastRow;
        for (int i = 0; i < limit && firstRow * columns + i < mods.size(); i++) {
            Mod item = mods.get(firstRow * columns + i);
            int x = contentLeft + i % columns * (cardWidth + 8), y = 65 + i / columns * ROW;
            Component label = item.managed() ? QuartzText.of("Built in", "Wbudowany")
                    : item.enabled() ? QuartzText.of("Disable", "Wyłącz") : QuartzText.of("Enable", "Włącz");
            int buttonW = cardWidth - (item.module() == null ? 14 : 41);
            add(label, x + 7, y + 49, buttonW, 19, item.enabled(), () -> toggle(item)).active = !item.managed();
            if (item.module() != null) {
                add(Component.literal("⚙"), x + cardWidth - 29, y + 49, 22, 19, false,
                        () -> open(new QuartzHudSettingsScreen(this, item.module())));
            }
        }
    }

    private void scan() {
        mods.clear();
        if (!installed) {
            for (QuartzHudModule module : QuartzHudModule.values()) {
                boolean enabled = module.settings().enabled;
                if (matches(enabled, module.en) || matches(enabled, module.label().getString())) {
                    mods.add(new Mod(null, module.label().getString(), enabled, false, module));
                }
            }
            return;
        }
        Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve("mods");
        if (!Files.isDirectory(folder)) return;
        try (var paths = Files.list(folder)) {
            paths.filter(Files::isRegularFile).sorted().forEach(file -> {
                String name = file.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".jar") && !lower.endsWith(".jar.disabled")) return;
                boolean managed = lower.startsWith("quartz-client-managed") || lower.startsWith("quartz-fabric-api-managed");
                boolean enabled = lower.endsWith(".jar");
                String label = modName(file, name);
                if (matches(enabled, label)) mods.add(new Mod(file, label, enabled, managed, null));
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
            firstRow = 0;
            rebuildWidgets();
        });
    }

    private boolean matches(boolean enabled, String name) {
        return (filter == Filter.ALL || (filter == Filter.ENABLED) == enabled)
                && name.toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));
    }

    private void changeList(boolean next) {
        installed = next;
        filter = Filter.ALL;
        status = null;
        firstRow = 0;
        query = "";
        rebuildWidgets();
    }

    private void move(int amount) {
        int next = Math.max(0, Math.min(lastRow, firstRow + amount));
        if (next == firstRow) return;
        firstRow = next;
        rebuildWidgets();
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (x < contentLeft || y < 63 || y >= height - 23 || dy == 0)
            return super.mouseScrolled(x, y, dx, dy);
        move(dy > 0 ? -1 : 1);
        return true;
    }

    private void toggle(Mod mod) {
        if (mod.module() != null) {
            mod.module().settings().enabled = !mod.enabled();
            QuartzConfig.save();
            rebuildWidgets();
            return;
        }
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

    private void open(Screen screen) { Minecraft.getInstance().setScreen(screen); }
    @Override public void onClose() { open(parent); }

    @Override
    protected void drawQuartz(GuiGraphics g, int mouseX, int mouseY, float delta) {
        QuartzTheme.background(g, width, height);
        QuartzTheme.card(g, left, 9, width - 2 * left, 25, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
        QuartzTheme.card(g, left, 40, sidebar, height - 50, 0xFFFCF4FE, QuartzTheme.OUTLINE);
        QuartzTheme.crystal(g, left + 6, 11, 14, 21);
        g.drawString(font, "Quartz Client", left + 26, 18, QuartzTheme.ACCENT, false);
        for (int i = 0; i < limit && firstRow * columns + i < mods.size(); i++) {
            Mod mod = mods.get(firstRow * columns + i);
            int x = contentLeft + i % columns * (cardWidth + 8), y = 65 + i / columns * ROW;
            QuartzTheme.card(g, x, y, cardWidth, 74, mod.enabled() ? 0xFFFFF9FF : QuartzTheme.WHITE, QuartzTheme.OUTLINE);
            g.drawString(font, font.plainSubstrByWidth(mod.name(), Math.max(0, cardWidth - 16)), x + 8, y + 8, QuartzTheme.INK, false);
            if (mod.module() != null) {
                int area = Math.max(24, cardWidth - 12);
                var sample = QuartzHud.panel(mod.module(), minecraft, true, area, 27);
                g.enableScissor(x + 5, y + 20, x + cardWidth - 5, y + 47);
                try {
                    QuartzHud.draw(g, minecraft, mod.module(), sample, x + (cardWidth - sample.width()) / 2,
                            y + 21 + Math.max(0, (26 - sample.height()) / 2));
                } finally {
                    g.disableScissor();
                }
            } else {
                g.drawString(font, mod.enabled() ? QuartzText.of("Enabled", "Włączony") : QuartzText.of("Disabled", "Wyłączony"),
                        x + 8, y + 27, QuartzTheme.MUTED, false);
            }
        }
        String footer = status == null ? mods.isEmpty() ? QuartzText.of("No matching mods.", "Brak modów w tym widoku.").getString() : "" : status.getString();
        if (lastRow > 0) {
            int track = Math.max(10, height - 90), thumb = Math.max(9, track * rows / (rows + lastRow));
            QuartzTheme.rounded(g, width - left + 1, 65, 4, track, 0xFFE9D9F1);
            QuartzTheme.rounded(g, width - left + 1, 65 + (track - thumb) * firstRow / lastRow, 4, thumb, QuartzTheme.ACCENT);
        }
        g.drawString(font, font.plainSubstrByWidth(footer, Math.max(0, bodyWidth - 88)), contentLeft + 50, height - 17, QuartzTheme.MUTED, false);
        drawWidgets(g, mouseX, mouseY, delta);
    }
}
