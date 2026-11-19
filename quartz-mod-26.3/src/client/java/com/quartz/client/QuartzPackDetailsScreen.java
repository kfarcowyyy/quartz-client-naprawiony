package com.quartz.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

final class QuartzPackDetailsScreen extends QuartzSafeScreen {
    private final Screen parent;
    private final QuartzPackDownloads.Project project;
    private final Runnable install;
    private final QuartzPackImages images = new QuartzPackImages();
    private List<FormattedCharSequence> lines = List.of();
    private QuartzPackDownloads.Details details;
    private int left, span, picture, scroll, maxScroll;
    private boolean requested, failed;

    QuartzPackDetailsScreen(Screen parent, QuartzPackDownloads.Project project, Runnable install) {
        super(Component.literal(project.title()));
        this.parent = parent;
        this.project = project;
        this.install = install;
    }

    @Override protected void init() {
        span = Math.min(width - 24, 512);
        left = (width - span) / 2;
        button(QuartzText.of("Back", "Wróć"), left, height - 27, 58, false, this::onClose);
        button(QuartzText.of("Download", "Pobierz"), left + span - 75, height - 27, 75, true, install).active =
                details == null || details.versions().isEmpty() || details.versions().contains(QuartzPackDownloads.VERSION);
        if (details != null && details.gallery().size() > 1) {
            button(Component.literal("‹"), width / 2 - 44, height - 27, 19, false, () -> change(-1));
            button(Component.literal("›"), width / 2 + 25, height - 27, 19, false, () -> change(1));
        }
        buildLines();
    }

    private void change(int delta) { picture = Math.floorMod(picture + delta, details.gallery().size()); }

    @Override public void tick() {
        super.tick();
        if (requested) return;
        requested = true;
        QuartzPackDownloads.details(project).whenComplete((result, error) -> minecraft.execute(() -> {
            details = result;
            failed = error != null;
            if (minecraft.gui.screen() == this) rebuildWidgets();
        }));
    }

    private void buildLines() {
        String body = details == null ? project.description() : details.body();
        if (body.length() > 14_000) body = body.substring(0, 14_000);
        body = body.replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("!?\\[([^\\]]*)\\]\\([^\\n)]*\\)", "$1").replace("**", "");
        var all = new ArrayList<FormattedCharSequence>();
        if (details != null && !details.license().isBlank()) {
            all.addAll(font.split(QuartzText.of("License: " + details.license(), "Licencja: " + details.license()), span - 18));
        }
        if (details != null && !details.versions().isEmpty()) {
            all.addAll(font.split(Component.literal("Minecraft: " + String.join(", ", details.versions())), span - 18));
        }
        for (String line : body.split("\\R")) all.addAll(font.split(Component.literal(line.isBlank() ? " " : line), span - 18));
        lines = List.copyOf(all);
    }

    private QuartzButton button(Component label, int x, int y, int w, boolean accent, Runnable action) {
        return addRenderableWidget(new QuartzButton(label, x, y, w, 19, accent, action));
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy == 0 || maxScroll == 0) return super.mouseScrolled(x, y, dx, dy);
        scroll = Math.max(0, Math.min(maxScroll, scroll + (dy < 0 ? 3 : -3)));
        return true;
    }

    @Override protected void drawQuartz(GuiGraphicsExtractor g, int mx, int my, float delta) {
        QuartzTheme.background(g, width, height);
        QuartzTheme.card(g, left, 9, span, height - 43, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
        images.draw(g, project.icon(), left + 7, 16, 26, 26);
        g.text(font, font.plainSubstrByWidth(project.title(), Math.max(0, span - 48)), left + 40, 18, QuartzTheme.ACCENT, false);
        g.text(font, failed ? QuartzText.of("Could not load details", "Brak szczegółów")
                : details == null ? QuartzText.of("Loading from Modrinth…", "Wczytuję z Modrinth…") : Component.literal("Modrinth"),
                left + 40, 31, QuartzTheme.MUTED, false);
        int y = 49;
        if (details != null && !details.gallery().isEmpty()) {
            int imageHeight = Math.max(32, Math.min(135, (height - 85) / 2));
            QuartzTheme.rounded(g, left + 7, y, span - 14, imageHeight, 0xFFF0E4F6);
            images.draw(g, details.gallery().get(picture), left + 7, y, span - 14, imageHeight);
            if (details.gallery().size() > 1) QuartzGraphics.center(g, font,
                    Component.literal((picture + 1) + " / " + details.gallery().size()), width / 2, height - 21, QuartzTheme.ACCENT);
            y += imageHeight + 8;
        }
        int count = Math.max(0, (height - 38 - y) / 11);
        maxScroll = Math.max(0, lines.size() - count);
        scroll = Math.min(maxScroll, scroll);
        for (int i = 0; i < count && i + scroll < lines.size(); i++)
            g.text(font, lines.get(i + scroll), left + 8, y + i * 11, QuartzTheme.INK, false);
        drawWidgets(g, mx, my, delta);
    }

    @Override public void removed() { images.clear(); }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
