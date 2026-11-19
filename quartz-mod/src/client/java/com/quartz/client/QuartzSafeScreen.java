package com.quartz.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

abstract class QuartzSafeScreen extends Screen {
    private boolean failed;

    protected QuartzSafeScreen(Component title) { super(title); }

    @Override
    public final void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // These screens draw their own background; vanilla blur must not be requested again.
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (failed) return;
        try {
            drawQuartz(graphics, mouseX, mouseY, delta);
        } catch (RuntimeException error) {
            failed = true;
            LoggerFactory.getLogger("Quartz Screen").error("Could not render {}; returning to the previous screen",
                    getClass().getSimpleName(), error);
        }
    }

    protected abstract void drawQuartz(GuiGraphics graphics, int mouseX, int mouseY, float delta);

    protected final void drawWidgets(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        if (failed) {
            failed = false;
            onClose();
            return;
        }
        super.tick();
    }
}
