package com.quartz.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.UUID;

final class QuartzPackImages {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("Quartz Modrinth");
    private static final class Picture {
        Identifier id;
        int width, height;
    }
    private final HashMap<String, Picture> pictures = new HashMap<>();

    void draw(GuiGraphics g, String url, int x, int y, int width, int height) {
        if (url == null || url.isBlank() || width < 1 || height < 1) return;
        Picture picture = pictures.get(url);
        if (picture == null && pictures.size() < 30) {
            picture = new Picture();
            pictures.put(url, picture);
            load(url, picture);
        }
        if (picture == null || picture.id == null) return;
        float scale = Math.min((float) width / picture.width, (float) height / picture.height);
        int tw = Math.max(1, (int) (picture.width * scale)), th = Math.max(1, (int) (picture.height * scale));
        g.blit(RenderPipelines.GUI_TEXTURED, picture.id, x + (width - tw) / 2, y + (height - th) / 2,
                0, 0, tw, th, picture.width, picture.height, picture.width, picture.height);
    }

    private void load(String url, Picture picture) {
        Minecraft client = Minecraft.getInstance();
        QuartzPackDownloads.image(url).thenAccept(bytes -> client.execute(() -> {
            if (bytes == null || pictures.get(url) != picture) return;
            try (var stream = new ByteArrayInputStream(bytes)) {
                NativeImage image = NativeImage.read(stream);
                picture.width = image.getWidth();
                picture.height = image.getHeight();
                Identifier id = Identifier.fromNamespaceAndPath("quartz", "modrinth/" + UUID.randomUUID());
                client.getTextureManager().register(id, new DynamicTexture(() -> "Modrinth image", image));
                picture.id = id;
            } catch (java.io.IOException | RuntimeException error) {
                LOG.debug("Could not load a Modrinth image", error);
            }
        }));
    }

    void clear() {
        for (Picture picture : pictures.values()) {
            if (picture.id != null) Minecraft.getInstance().getTextureManager().release(picture.id);
        }
        pictures.clear();
    }
}
