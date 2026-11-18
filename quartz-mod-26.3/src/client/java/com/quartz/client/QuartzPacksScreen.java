package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

public final class QuartzPacksScreen extends Screen {
    private final Screen parent;
    private Component result;

    public QuartzPacksScreen(Screen parent) {
        super(QuartzText.of("Packs & shaders", "Paczki i shadery"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(226, width - 16);
        int x = (width - w) / 2;
        int y = height / 2 - 20;
        addRenderableWidget(Button.builder(QuartzText.of("Open resource packs folder", "Folder paczek zasobów"),
                button -> QuartzFiles.openFolder("resourcepacks")).bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(QuartzText.of("Open shader packs folder", "Folder shaderów"),
                button -> QuartzFiles.openFolder("shaderpacks")).bounds(x, y + 24, w, 20).build());
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), button -> onClose())
                .bounds(x, y + 48, w, 20).build());
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        int added = 0;
        for (Path path : files) {
            String name = path.getFileName().toString();
            if (!Files.isRegularFile(path) || !name.toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
            try (ZipFile zip = new ZipFile(path.toFile())) {
                String folder;
                if (zip.getEntry("pack.mcmeta") != null) folder = "resourcepacks";
                else if (zip.stream().anyMatch(entry -> entry.getName().startsWith("shaders/"))) folder = "shaderpacks";
                else continue;
                Path target = Minecraft.getInstance().gameDirectory.toPath().resolve(folder);
                Files.createDirectories(target);
                Files.copy(path, target.resolve(name));
                added++;
            } catch (IOException ignored) {
                // Existing files are left intact.
            }
        }
        result = QuartzText.of("Installed " + added + " of " + files.size(), "Zainstalowano " + added + " z " + files.size());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        QuartzGraphics.center(graphics, font, title, width / 2, height / 2 - 55, 0xFFF3EDFF);
        Component message = result == null
                ? QuartzText.of("Drop a resource pack or shader ZIP here", "Upuść tutaj ZIP z paczką lub shaderami") : result;
        QuartzGraphics.center(graphics, font, message, width / 2, height / 2 - 38, 0xFFC8B7E5);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
