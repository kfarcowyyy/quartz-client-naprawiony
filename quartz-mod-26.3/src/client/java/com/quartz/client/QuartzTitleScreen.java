package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class QuartzTitleScreen extends Screen {
    private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath("quartz", "textures/wardrobe/quartz-crystal.png");
    private static TitleScreen classic;
    private final TitleScreen original;
    private int center;
    private int actionsTop;
    private int panelWidth;

    private QuartzTitleScreen(TitleScreen original) {
        super(Component.literal("Quartz Client"));
        this.original = original;
    }

    public static void replaceVanilla(Minecraft client) {
        if (!client.isDemo() && client.gui.screen() instanceof TitleScreen title && title != classic) {
            classic = null;
            client.gui.setScreen(new QuartzTitleScreen(title));
        }
    }

    @Override
    protected void init() {
        center = width / 2;
        actionsTop = Math.max(48, (height - 104) / 2);
        panelWidth = Math.min(240, width - 16);
        int half = (panelWidth - 6) / 2;
        int left = center - panelWidth / 2;
        add(Component.translatable("menu.singleplayer"), left, actionsTop, panelWidth,
                () -> show(new SelectWorldScreen(this)));
        var multiplayer = add(Component.translatable("menu.multiplayer"), left, actionsTop + 24, panelWidth,
                () -> show(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
        multiplayer.active = minecraft.allowsMultiplayer();
        add(QuartzText.of("Mods", "Mody"), left, actionsTop + 48, half, () -> show(new QuartzMenuScreen(this)));
        add(QuartzText.of("Cosmetics", "Kosmetyki"), left + half + 6, actionsTop + 48, half,
                () -> show(new QuartzWardrobeScreen(this)));
        add(QuartzText.of("Packs & shaders", "Paczki i shadery"), left, actionsTop + 72, half,
                () -> show(new QuartzPacksScreen(this)));
        add(QuartzText.of("Screenshots", "Zrzuty ekranu"), left + half + 6, actionsTop + 72, half,
                () -> show(new QuartzScreenshotScreen(this)));
        add(Component.translatable("menu.options"), left, actionsTop + 96, half,
                () -> show(new OptionsScreen(this, minecraft.options)));
        add(Component.translatable("menu.quit"), left + half + 6, actionsTop + 96, half, minecraft::stop);
        add(QuartzText.of("Classic menu", "Klasyczne menu"), 8, height - 27, 84, () -> {
            classic = original;
            show(original);
        });
    }

    private Button add(Component label, int x, int y, int buttonWidth, Runnable action) {
        return addRenderableWidget(Button.builder(label, button -> action.run()).bounds(x, y, buttonWidth, 20).build());
    }

    private void show(Screen screen) { minecraft.gui.setScreen(screen); }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xFF131524, 0xFF2B2042);
        for (int y = 4; y < height; y += 12) {
            for (int x = (y % 24 == 4 ? 4 : 10); x < width; x += 12) {
                graphics.fill(x, y, x + 1, y + 1, 0x443D3262);
            }
        }
        String caption = "High Performance Minecraft Client";
        graphics.text(font, caption, width - font.width(caption) - 8, 8, 0xFFCBB9E8, false);
        int markY = Math.max(14, actionsTop - 37);
        int markX = center - (font.width(title) + 25) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRYSTAL, markX, markY, 0, 0, 16, 24, 16, 24);
        graphics.text(font, title, markX + 22, markY + 8, 0xFFF7EEFF, true);
        int left = center - panelWidth / 2 - 5;
        graphics.fill(left, actionsTop - 5, left + panelWidth + 10, actionsTop + 121, 0x99333250);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
