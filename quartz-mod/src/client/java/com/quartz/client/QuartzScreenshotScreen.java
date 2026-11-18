package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class QuartzScreenshotScreen extends Screen {
    private final Screen parent;
    private Path folder;
    private List<Path> images = List.of();

    public QuartzScreenshotScreen(Screen parent) {
        super(QuartzText.of("Screenshots", "Zrzuty ekranu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        folder = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
        try {
            Files.createDirectories(folder);
            try (var stream = Files.list(folder)) {
                images = stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                        .sorted((a, b) -> b.getFileName().toString().compareToIgnoreCase(a.getFileName().toString())).toList();
            }
        } catch (Exception ignored) { images = List.of(); }

        int rows = Math.max(1, (height - 118) / 28);
        for (int i = 0; i < Math.min(rows, images.size()); i++) {
            Path image = images.get(i);
            addRenderableWidget(Button.builder(Component.literal(image.getFileName().toString()), b -> open(image))
                    .bounds(width / 2 - 170, 44 + i * 28, 340, 24).build());
        }
        addRenderableWidget(Button.builder(QuartzText.of("Open screenshots folder", "Otworz folder zrzutow"), b -> open(folder))
                .bounds(width / 2 - 170, height - 66, 160, 24).build());
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), b -> onClose())
                .bounds(width / 2 + 10, height - 66, 160, 24).build());
    }

    private static void open(Path path) {
        try { Desktop.getDesktop().open(path.toFile()); } catch (Exception ignored) { }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFFFF);
        if (images.isEmpty()) graphics.drawCenteredString(font,
                QuartzText.of("Press F2 to take a screenshot", "Nacisnij F2, aby zrobic zrzut ekranu"),
                width / 2, height / 2, 0xFFB8C7F2);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
