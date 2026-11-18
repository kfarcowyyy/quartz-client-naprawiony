package com.quartz.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class QuartzShotPreview {
    private static final QuartzShotPreview POPUP = new QuartzShotPreview("screenshot_popup");
    private static Path last;
    private static int ticks;
    private static long until;
    private final Identifier id;
    private int imageWidth;
    private int imageHeight;

    public QuartzShotPreview(String name) {
        id = Identifier.fromNamespaceAndPath("quartz", name);
    }

    public static Path folder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
    }

    public static List<Path> list() {
        if (!Files.isDirectory(folder())) return List.of();
        try (var files = Files.list(folder())) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed()).toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    public static void start() {
        var shots = list();
        last = shots.isEmpty() ? null : shots.getFirst();
    }

    public static void tick() {
        if (until > 0 && System.nanoTime() >= until) {
            until = 0;
            POPUP.close();
        }
        if (++ticks < 10) return;
        ticks = 0;
        var shots = list();
        if (!shots.isEmpty() && !shots.getFirst().equals(last) && POPUP.load(shots.getFirst())) {
            last = shots.getFirst();
            until = System.nanoTime() + 3_500_000_000L;
        }
    }

    public static void popup(GuiGraphics graphics) {
        if (until > 0) POPUP.draw(graphics, graphics.guiWidth() - 132, 14, 120, 72);
    }

    public boolean load(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(stream);
            close();
            DynamicTexture texture = new DynamicTexture(path.getFileName()::toString, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        if (imageWidth == 0 || width <= 0 || height <= 0) return;
        double scale = Math.min((double) width / imageWidth, (double) height / imageHeight);
        int w = Math.max(1, (int) (imageWidth * scale));
        int h = Math.max(1, (int) (imageHeight * scale));
        int left = x + (width - w) / 2;
        int top = y + (height - h) / 2;
        graphics.fill(left - 1, top - 1, left + w + 1, top + h + 1, 0xFF9B80C4);
        graphics.blit(RenderPipelines.GUI_TEXTURED, id, left, top, 0f, 0f, w, h,
                imageWidth, imageHeight, imageWidth, imageHeight);
    }

    public void close() {
        if (imageWidth != 0) Minecraft.getInstance().getTextureManager().release(id);
        imageWidth = 0;
        imageHeight = 0;
    }
}
