package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.IntFunction;

public final class QuartzMenuScreen extends Screen {
    private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath("quartz", "textures/wardrobe/quartz-crystal.png");
    private final Screen parent;
    private int panelWidth;
    private int left;
    private int top;

    public QuartzMenuScreen(Screen parent) {
        super(Component.literal("Quartz Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(344, width - 16);
        left = (width - panelWidth) / 2;
        top = Math.max(8, (height - 158) / 2);
        button(0, 0, "Mods", "Mody", () -> open(new QuartzModListScreen(this)));
        button(1, 0, "Cosmetics", "Kosmetyki", () -> open(new QuartzWardrobeScreen(this)));
        button(0, 1, "HUD layout", "Pozycja HUD", () -> open(new QuartzHudEditorScreen(this)));
        button(1, 1, "HUD settings", "Ustawienia HUD", () -> open(new QuartzSettingsScreen(this)));
        button(0, 2, "Screenshots", "Zrzuty ekranu", () -> open(new QuartzScreenshotScreen(this)));
        button(1, 2, "Packs & shaders", "Paczki i shadery", () -> open(new QuartzPacksScreen(this)));
        addRenderableWidget(Button.builder(Component.literal("×"), button -> onClose())
                .bounds(left + panelWidth - 29, top + 8, 20, 20).build());
    }

    private void button(int column, int row, String en, String pl, Runnable action) {
        int size = (panelWidth - 34) / 2;
        addRenderableWidget(Button.builder(QuartzText.of(en, pl), button -> action.run())
                .bounds(left + 12 + column * (size + 10), top + 43 + 30 * row, size, 24).build());
    }

    private static void open(Screen page) { Minecraft.getInstance().gui.setScreen(page); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xF2101528, 0xF2251837);
        graphics.fill(left, top, left + panelWidth, top + 151, 0xFF695289);
        graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 150, 0xFF241E37);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRYSTAL, left + 10, top + 8, 0, 0, 12, 18, 16, 24, 16, 24);
        graphics.text(font, title, left + 25, top + 15, 0xFFF6EEFF, false);
        QuartzGraphics.center(graphics, font, QuartzText.of("Change the menu shortcut in Controls", "Zmień skrót menu w Sterowaniu"),
                width / 2, top + 136, 0xFFC8B7E5);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { open(parent); }
}
