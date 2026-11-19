package com.quartz.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class QuartzHudEditorScreen extends QuartzSafeScreen {
    private final Screen parent;
    private QuartzHudModule selected;
    private boolean dragging;
    private int dragX, dragY;

    public QuartzHudEditorScreen(Screen parent) { this(parent, QuartzHudModule.FPS); }

    QuartzHudEditorScreen(Screen parent, QuartzHudModule module) {
        super(QuartzText.of("HUD layout", "Układ HUD"));
        this.parent = parent;
        selected = module;
    }

    @Override protected void init() {
        int span = Math.min(width - 12, 360), left = (width - span) / 2, unit = (span - 12) / 3;
        addRenderableWidget(new QuartzButton(selected.label(), left, 5, unit, 20, true, () -> {
            var modules = QuartzHudModule.values();
            selected = modules[(selected.ordinal() + 1) % modules.length];
            rebuildWidgets();
        }));
        addRenderableWidget(new QuartzButton(QuartzText.of("Appearance", "Wygląd"), left + unit + 6, 5, unit, 20, false,
                () -> minecraft.setScreen(new QuartzHudSettingsScreen(this, selected))));
        addRenderableWidget(new QuartzButton(QuartzText.of("Done", "Gotowe"), left + 2 * (unit + 6), 5, unit, 20, false, this::onClose));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override protected void drawQuartz(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x341E152C);
        for (QuartzHudModule module : QuartzHudModule.values()) {
            if (module.settings().enabled && module != selected) panel(g, module, false);
        }
        panel(g, selected, true);
        drawWidgets(g, mx, my, delta);
    }

    private void panel(GuiGraphics g, QuartzHudModule module, boolean selected) {
        var panel = QuartzHud.panel(module, minecraft, true, width, height);
        var p = QuartzHud.position(module.settings(), panel, width, height);
        if (selected) {
            int x = p.x(), y = p.y(), right = x + panel.width(), bottom = y + panel.height();
            g.fill(x, y, right, y + 1, 0xFFE9C7FB);
            g.fill(x, bottom - 1, right, bottom, 0xFFE9C7FB);
            g.fill(x, y, x + 1, bottom, 0xFFE9C7FB);
            g.fill(right - 1, y, right, bottom, 0xFFE9C7FB);
        }
        QuartzHud.draw(g, minecraft, module, panel, p.x(), p.y());
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (super.mouseClicked(e, doubleClick)) return true;
        if (e.button() != 0) return false;
        if (select(e, selected)) return true;
        for (QuartzHudModule module : QuartzHudModule.values()) {
            if (module != selected && module.settings().enabled && select(e, module)) return true;
        }
        return false;
    }

    private boolean select(MouseButtonEvent e, QuartzHudModule module) {
        var panel = QuartzHud.panel(module, minecraft, true, width, height);
        var p = QuartzHud.position(module.settings(), panel, width, height);
        double x = e.x() - p.x(), y = e.y() - p.y();
        if (x < 0 || x >= panel.width() || y < 0 || y >= panel.height()) return false;
        selected = module;
        dragging = true;
        dragX = (int) x;
        dragY = (int) y;
        rebuildWidgets();
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (!dragging || e.button() != 0) return super.mouseDragged(e, dx, dy);
        var panel = QuartzHud.panel(selected, minecraft, true, width, height);
        QuartzHud.move(selected.settings(), panel, (int) e.x() - dragX, (int) e.y() - dragY, width, height);
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (!dragging || e.button() != 0) return super.mouseReleased(e);
        removed();
        return true;
    }

    @Override public void removed() {
        if (!dragging) return;
        dragging = false;
        QuartzConfig.save();
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}
