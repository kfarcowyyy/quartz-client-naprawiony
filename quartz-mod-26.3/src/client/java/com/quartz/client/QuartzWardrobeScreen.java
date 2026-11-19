package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class QuartzWardrobeScreen extends Screen {
    private enum Tab { SKINS, CAPES, COSMETICS }
    private final Screen parent;
    private Tab tab = Tab.SKINS;
    private boolean slim;
    private int left, top, span, page, pages, limit;
    private List<QuartzWardrobe.Skin> skins = List.of();
    private List<QuartzWardrobe.Cape> capes = List.of();
    private Component notice;

    public QuartzWardrobeScreen(Screen parent) {
        super(QuartzText.of("Wardrobe", "Garderoba"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        span = Math.min(480, width - 24);
        left = (width - span) / 2;
        top = 12;
        int tabWidth = (span - 12) / 3;
        tab(QuartzText.of("Skins", "Skiny"), left, tabWidth, Tab.SKINS);
        tab(QuartzText.of("Capes", "Peleryny"), left + tabWidth + 6, tabWidth, Tab.CAPES);
        tab(QuartzText.of("3D cosmetics", "Kosmetyki 3D"), left + (tabWidth + 6) * 2, tabWidth, Tab.COSMETICS);
        add(QuartzText.of("Back", "Wróć"), left + span - 63, height - 28, 63, 18, false, this::onClose);
        limit = Math.max(1, (height - 113) / 27);
        if (tab == Tab.COSMETICS) return;
        if (tab == Tab.SKINS) drawSkinControls();
        else capes = QuartzWardrobe.capes();
        int count = tab == Tab.SKINS ? skins.size() : capes.size();
        pages = Math.max(1, (count + limit - 1) / limit);
        page = Math.min(page, pages - 1);
        if (pages > 1) {
            add(Component.literal("‹"), left, height - 28, 23, 18, false, () -> { page--; rebuildWidgets(); }).active = page > 0;
            add(Component.literal("›"), left + 52, height - 28, 23, 18, false, () -> { page++; rebuildWidgets(); }).active = page + 1 < pages;
        }
        if (tab != Tab.SKINS) return;
        String selected = QuartzWardrobe.selected();
        for (int i = 0; i < limit && page * limit + i < skins.size(); i++) {
            var skin = skins.get(page * limit + i);
            boolean on = skin.id().equals(selected);
            int y = 86 + 27 * i;
            add(on ? QuartzText.of("Equipped", "Założony") : QuartzText.of("Equip", "Załóż"),
                    left + span - 126, y + 2, 66, 18, on, () -> change(() -> QuartzWardrobe.equip(skin.id()))).active = !on;
            add(QuartzText.of("Remove", "Usuń"), left + span - 53, y + 2, 47, 18, false,
                    () -> change(() -> QuartzWardrobe.delete(skin.id())));
        }
    }

    private void drawSkinControls() {
        skins = QuartzWardrobe.skins();
        int half = span / 3;
        add(QuartzText.of("Add skin", "Dodaj skina"), left, 57, half - 5, 20, true, () ->
                notice = QuartzText.of("Drop your 64 × 64 PNG onto this screen.", "Upuść PNG 64 × 64 na ten ekran."));
        add(Component.literal(slim ? "Slim" : "Classic"), left + half, 57, half - 5, 20, false, () -> {
            slim = !slim;
            rebuildWidgets();
        });
        add(QuartzText.of("Account skin", "Skin konta"), left + 2 * half, 57, half - 5, 20, false,
                () -> change(() -> QuartzWardrobe.equip("account"))).active = !"account".equals(QuartzWardrobe.selected());
    }

    private interface Change { void run() throws IOException; }

    private void change(Change action) {
        try {
            action.run();
            notice = null;
            rebuildWidgets();
        } catch (IOException | RuntimeException error) {
            notice = QuartzText.of("Could not update the skin.", "Nie udało się zmienić skina.");
        }
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (tab != Tab.SKINS) return;
        change(() -> {
            for (Path source : files) QuartzWardrobe.importPng(source, slim);
        });
    }

    private void tab(Component label, int x, int size, Tab next) {
        add(label, x, 29, size, 21, tab == next, () -> {
            tab = next;
            page = 0;
            notice = null;
            rebuildWidgets();
        });
    }

    private QuartzButton add(Component label, int x, int y, int size, int h, boolean accent, Runnable action) {
        return addRenderableWidget(new QuartzButton(label, x, y, size, h, accent, action));
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        QuartzTheme.background(g, width, height);
        g.text(font, title, left, top + 3, QuartzTheme.ACCENT, false);
        QuartzTheme.card(g, left, 82, span, Math.max(30, height - 120), QuartzTheme.WHITE, QuartzTheme.OUTLINE);
        if (tab == Tab.COSMETICS) {
            g.text(font, QuartzText.of("Your 3D cosmetics will appear here.", "Tu pojawią się Twoje kosmetyki 3D."),
                    left + 9, 95, QuartzTheme.MUTED, false);
        } else if ((tab == Tab.SKINS && skins.isEmpty()) || (tab == Tab.CAPES && capes.isEmpty())) {
            Component empty = tab == Tab.SKINS
                    ? QuartzText.of("Drop a 64 × 64 PNG here to add a skin.", "Dodaj skina, upuszczając tutaj PNG 64 × 64.")
                    : QuartzText.of("No capes loaded. Check your Minecraft account in the launcher.", "Brak wczytanych peleryn. Sprawdź konto Minecraft w launcherze.");
            g.text(font, font.plainSubstrByWidth(empty.getString(), span - 18), left + 9, 95, QuartzTheme.MUTED, false);
        } else if (tab != Tab.COSMETICS) {
            int count = tab == Tab.SKINS ? skins.size() : capes.size();
            for (int i = 0; i < limit && page * limit + i < count; i++) {
                int y = 86 + 27 * i;
                int index = page * limit + i;
                QuartzTheme.rounded(g, left + 4, y, span - 8, 22, 0xFFF9F1FD);
                String text = tab == Tab.SKINS ? skins.get(index).name() : capes.get(index).name();
                g.text(font, font.plainSubstrByWidth(text, Math.max(0, span - 143)), left + 10, y + 7, QuartzTheme.INK, false);
                if (tab == Tab.CAPES && capes.get(index).active()) {
                    g.text(font, QuartzText.of("Active on account", "Aktywna na koncie"),
                            left + span - 124, y + 7, QuartzTheme.ACCENT, false);
                }
            }
        }
        if (pages > 1 && tab != Tab.COSMETICS) g.text(font, (page + 1) + "/" + pages, left + 27, height - 23, QuartzTheme.MUTED, false);
        if (notice != null) {
            int x = left + ((pages > 1 && tab != Tab.COSMETICS) ? 80 : 0);
            String text = font.plainSubstrByWidth(notice.getString(), Math.max(0, left + span - 69 - x));
            g.text(font, text, x, height - 23, QuartzTheme.ACCENT, false);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
