package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public final class QuartzTitleScreen extends Screen {
    private int left, top, panelWidth;

    private QuartzTitleScreen() { super(Component.literal("Quartz Client")); }

    public static void replaceVanilla(Minecraft client) {
        if (client.isDemo() || !(client.screen instanceof TitleScreen original) || original instanceof Classic) return;
        client.setScreen(QuartzConfig.data.classicMenu ? new Classic() : new QuartzTitleScreen());
    }

    private static final class Classic extends TitleScreen {
        @Override
        protected void init() {
            super.init();
            addRenderableWidget(Button.builder(QuartzText.of("Switch to Quartz menu", "Wróć do menu Quartz"), button -> {
                QuartzConfig.data.classicMenu = false;
                QuartzConfig.save();
                minecraft.setScreen(new QuartzTitleScreen());
            }).bounds(6, 6, 126, 20).build());
        }
    }

    @Override
    protected void init() {
        panelWidth = Math.min(248, width - 28);
        int half = (panelWidth - 6) / 2;
        left = (width - panelWidth) / 2;
        top = Math.max(53, (height - 91) / 2);
        add(Component.translatable("menu.singleplayer"), left, top, panelWidth, true,
                () -> show(new SelectWorldScreen(this)));
        add(Component.translatable("menu.multiplayer"), left, top + 26, panelWidth, false,
                () -> show(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)))
                .active = minecraft.allowsMultiplayer();
        add(QuartzText.of("Mods", "Mody"), left, top + 52, half, false, () -> show(new QuartzMenuScreen(this)));
        add(QuartzText.of("Wardrobe", "Garderoba"), left + half + 6, top + 52, half, false,
                () -> show(new QuartzWardrobeScreen(this)));
        add(Component.translatable("menu.options"), left, top + 78, half, false, () -> show(new OptionsScreen(this, minecraft.options)));
        add(Component.translatable("menu.quit"), left + half + 6, top + 78, half, false, minecraft::stop);
        add(QuartzText.of("Classic menu", "Klasyczne menu"), 8, height - 28, 112, false, () -> {
            QuartzConfig.data.classicMenu = true;
            QuartzConfig.save();
            show(new Classic());
        });
    }

    private QuartzButton add(Component label, int x, int y, int w, boolean accent, Runnable action) {
        return addRenderableWidget(new QuartzButton(label, x, y, w, 21, accent, action));
    }

    private void show(Screen screen) { minecraft.setScreen(screen); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float delta) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        QuartzTheme.background(g, width, height);
        QuartzTheme.card(g, 8, 8, width - 16, 23, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
        g.drawString(font, "QUARTZ  /  CLIENT", 17, 16, QuartzTheme.ACCENT, false);
        String tagline = font.plainSubstrByWidth("High Performance Minecraft Client", Math.max(0, width - 118));
        g.drawString(font, tagline, width - 17 - font.width(tagline), 16, QuartzTheme.MUTED, false);
        QuartzTheme.card(g, left - 10, top - 19, panelWidth + 20, 127, 0xFFFAF6FE, QuartzTheme.OUTLINE);
        super.render(g, mouseX, mouseY, delta);
    }
}
