package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class QuartzModListScreen extends Screen {
    private final Screen parent;
    private final List<Path> mods = new ArrayList<>();

    public QuartzModListScreen(Screen parent) {
        super(QuartzText.of("Installed mods", "Zainstalowane mody"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        mods.clear();
        Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve("mods");
        try {
            Files.createDirectories(folder);
            try (var stream = Files.list(folder)) {
                stream.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".jar") || n.endsWith(".jar.disabled");
                }).sorted().forEach(mods::add);
            }
        } catch (Exception ignored) { }

        int maxRows = Math.max(1, (height - 112) / 27);
        for (int i = 0; i < Math.min(mods.size(), maxRows); i++) {
            Path file = mods.get(i);
            String name = file.getFileName().toString();
            boolean enabled = name.endsWith(".jar");
            String cleanName = name.replaceFirst("\\.jar(?:\\.disabled)?$", "");
            String label = (enabled ? "[ON] " : "[OFF] ") + cleanName;
            addRenderableWidget(Button.builder(Component.literal(label), button -> toggle(file, name, enabled))
                    .bounds(width / 2 - 170, 42 + i * 27, 340, 23).build());
        }
        addRenderableWidget(Button.builder(QuartzText.of("Open mods folder", "Otworz folder modow"), b ->
                QuartzFiles.openFolder("mods"))
                .bounds(width / 2 - 130, height - 62, 125, 24).build());
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), b -> onClose())
                .bounds(width / 2 + 5, height - 62, 125, 24).build());
    }

    private void toggle(Path file, String name, boolean enabled) {
        try {
            Path next = enabled
                    ? file.resolveSibling(name + ".disabled")
                    : file.resolveSibling(name.substring(0, name.length() - ".disabled".length()));
            Files.move(file, next);
            Minecraft.getInstance().gui.setScreen(new QuartzModListScreen(parent));
        } catch (Exception ignored) { }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        QuartzGraphics.center(graphics, font, title, width / 2, 18, 0xFFFFFFFF);
        String count = mods.size() > Math.max(1, (height - 112) / 27)
                ? QuartzText.of("Showing the first entries. Open the mods folder for the full list.",
                    "Widoczne sa pierwsze pozycje. Otworz folder, aby zobaczyc cala liste.").getString()
                : QuartzText.of("Changes take effect after restarting the game.",
                    "Zmiany zaczna dzialac po ponownym uruchomieniu gry.").getString();
        QuartzGraphics.center(graphics, font, count, width / 2, height - 34, 0xFFB8C7F2);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
