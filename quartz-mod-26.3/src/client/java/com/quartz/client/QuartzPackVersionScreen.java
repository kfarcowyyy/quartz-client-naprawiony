package com.quartz.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

final class QuartzPackVersionScreen extends QuartzSafeScreen {
    private final Screen parent;
    private final Consumer<String> select;
    private final String active;
    private String filter = "";
    private boolean requested, failed;
    private List<String> releases = List.of(QuartzPackDownloads.VERSION);
    private int left, span, first, limit, last;

    QuartzPackVersionScreen(Screen parent, String active, Consumer<String> select) {
        super(QuartzText.of("Minecraft version", "Wersja Minecrafta"));
        this.parent = parent;
        this.active = active;
        this.select = select;
    }

    @Override protected void init() {
        span = Math.min(320, width - 20);
        left = (width - span) / 2;
        var input = addRenderableWidget(new EditBox(font, left, 35, span, 20, title));
        input.setMaxLength(16);
        input.setValue(filter);
        input.setHint(QuartzText.of("Find version…", "Znajdź wersję…"));
        input.setResponder(value -> {
            filter = value;
            first = 0;
            rebuildWidgets();
            setFocused(children().stream().filter(EditBox.class::isInstance).findFirst().orElse(null));
        });
        int half = (span - 6) / 2;
        button(QuartzText.of("Any version", "Każda wersja"), left, 60, half, active.isEmpty(), () -> select.accept(""));
        button(QuartzText.of("Current", "Bieżąca").append(" · " + QuartzPackDownloads.VERSION),
                left + half + 6, 60, half, active.equals(QuartzPackDownloads.VERSION), () -> select.accept(QuartzPackDownloads.VERSION));
        var found = releases.stream().filter(v -> v.contains(filter.strip())).toList();
        limit = Math.max(1, (height - 128) / 22) * 3;
        last = Math.max(0, found.size() - limit);
        first = Math.min(last, first);
        int cell = (span - 10) / 3;
        for (int i = 0; first + i < found.size() && i < limit; i++) {
            String release = found.get(first + i);
            button(Component.literal(release), left + i % 3 * (cell + 5), 88 + i / 3 * 22, cell, release.equals(active),
                    () -> select.accept(release));
        }
        button(Component.literal("↑"), left, height - 24, 22, false, () -> move(-3)).active = first > 0;
        button(Component.literal("↓"), left + 26, height - 24, 22, false, () -> move(3)).active = first < last;
        button(QuartzText.of("Back", "Wróć"), left + span - 62, height - 24, 62, false, this::onClose);
    }

    private QuartzButton button(Component label, int x, int y, int w, boolean accent, Runnable action) {
        return addRenderableWidget(new QuartzButton(label, x, y, w, 19, accent, action));
    }

    private void move(int amount) {
        int next = Math.max(0, Math.min(last, first + amount));
        if (next == first) return;
        first = next;
        rebuildWidgets();
    }

    @Override public void tick() {
        super.tick();
        if (requested) return;
        requested = true;
        QuartzPackDownloads.gameVersions().whenComplete((found, error) -> minecraft.execute(() -> {
            if (error == null) releases = found;
            else failed = true;
            if (minecraft.gui.screen() == this) rebuildWidgets();
        }));
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy == 0 || y < 86 || y >= height - 29) return super.mouseScrolled(x, y, dx, dy);
        move(dy < 0 ? 3 : -3);
        return true;
    }

    @Override protected void drawQuartz(GuiGraphicsExtractor g, int mx, int my, float delta) {
        QuartzTheme.background(g, width, height);
        g.text(font, title, left, 16, QuartzTheme.ACCENT, false);
        if (failed) g.text(font, QuartzText.of("Could not load versions", "Nie udało się pobrać wersji"), left + 54, height - 19, QuartzTheme.MUTED, false);
        drawWidgets(g, mx, my, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
