package com.quartz.client;

import net.minecraft.client.Minecraft;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QuartzFiles {
    private QuartzFiles() {}

    public static void openFolder(String name) {
        try {
            Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve(name);
            Files.createDirectories(folder);
            Desktop.getDesktop().open(folder.toFile());
        } catch (Exception ignored) { }
    }
}
