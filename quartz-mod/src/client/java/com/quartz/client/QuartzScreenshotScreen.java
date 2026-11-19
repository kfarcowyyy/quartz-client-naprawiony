package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public final class QuartzScreenshotScreen extends QuartzSafeScreen {
    private final Screen parent;
    private final QuartzShotPreview image = new QuartzShotPreview("screenshot_gallery");
    private List<Path> files = List.of();
    private int selection;
    private boolean loaded;

    public QuartzScreenshotScreen(Screen parent) {
        super(QuartzText.of("Screenshots", "Zrzuty ekranu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        files = QuartzShotPreview.list();
        selection = Math.min(selection, Math.max(0, files.size() - 1));
        if (!files.isEmpty()) change(0);
        int left = width / 2;
        int bottom = height - 27;
        var previous = addRenderableWidget(Button.builder(Component.literal("←"), b -> change(-1))
                .bounds(left - 111, bottom, 22, 20).build());
        var next = addRenderableWidget(Button.builder(Component.literal("→"), b -> change(1))
                .bounds(left - 87, bottom, 22, 20).build());
        previous.active = next.active = files.size() > 1;
        addRenderableWidget(Button.builder(QuartzText.of("Open folder", "Otwórz folder"), b -> QuartzFiles.openFolder("screenshots"))
                .bounds(left - 61, bottom, 97, 20).build());
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), b -> onClose())
                .bounds(left + 40, bottom, 70, 20).build());
    }

    private void change(int step) {
        selection = Math.floorMod(selection + step, files.size());
        image.close();
        loaded = image.load(files.get(selection));
    }

    @Override
    protected void drawQuartz(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        QuartzTheme.background(graphics, width, height);
        graphics.drawCenteredString(font, title, width / 2, 8, QuartzTheme.INK);
        if (files.isEmpty()) {
            graphics.drawCenteredString(font, QuartzText.of("Press F2 to take a screenshot", "Naciśnij F2, aby zrobić zrzut"),
                    width / 2, height / 2, QuartzTheme.MUTED);
        } else {
            String name = (selection + 1) + " / " + files.size() + "   " + files.get(selection).getFileName();
            graphics.drawCenteredString(font, name, width / 2, 22, QuartzTheme.MUTED);
            if (loaded) image.draw(graphics, 16, 36, width - 32, height - 68);
            else graphics.drawCenteredString(font, QuartzText.of("Image unavailable", "Nie można otworzyć pliku"),
                    width / 2, height / 2, 0xFFAA395C);
        }
        drawWidgets(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void removed() { image.close(); }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
