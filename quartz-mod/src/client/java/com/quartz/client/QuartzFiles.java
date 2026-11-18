package com.quartz.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

public final class QuartzFiles {
    private QuartzFiles() {}

    public static void openFolder(String name) {
        try {
            Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve(name);
            Files.createDirectories(folder);
            Util.getPlatform().openFile(folder.toFile());
        } catch (Exception ignored) { }
    }
}
