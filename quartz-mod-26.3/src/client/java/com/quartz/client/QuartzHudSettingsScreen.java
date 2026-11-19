package com.quartz.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

final class QuartzHudSettingsScreen extends QuartzSafeScreen {
    private final Screen parent;
    private final QuartzHudModule module;
    private int left, span, top, preview, controls, contentBottom, scroll;

    QuartzHudSettingsScreen(Screen parent, QuartzHudModule module) {
        super(module.label());
        this.parent = parent;
        this.module = module;
    }

    @Override protected void init() {
        span = Math.min(width - 16, 552);
        left = (width - span) / 2;
        top = Math.max(6, Math.min(26, (height - 208) / 2));
        preview = Math.max(88, span / 3);
        controls = span - preview - 12;
        contentBottom = height - 35;
        scroll = Math.min(scroll, Math.max(0, top + 212 - contentBottom));
        var setting = module.settings();
        int button = Math.min(74, span / 5);
        add(Component.literal("‹"), left + 3, top + 3, 20, false, this::onClose);
        add(setting.enabled ? QuartzText.of("On", "Wł.") : QuartzText.of("Off", "Wył."), left + span - 2 * button - 9, top + 3,
                button, setting.enabled, () -> {
            setting.enabled = !setting.enabled;
            rebuildWidgets();
        });
        add(QuartzText.of("Reset", "Resetuj"), left + span - button - 3, top + 3, button, false, () -> {
            var replacement = new QuartzConfig.Hud(module);
            replacement.enabled = setting.enabled;
            replacement.x = setting.x;
            replacement.y = setting.y;
            replacement.oldX = setting.oldX;
            replacement.oldY = setting.oldY;
            QuartzConfig.data.modules.put(module.name(), replacement);
            rebuildWidgets();
        });
        int start = top + 49 - scroll;
        int half = (controls - 6) / 2;
        option(setting.background ? QuartzText.of("Background", "Tło") : setting.brackets ? QuartzText.of("Brackets", "Nawiasy")
                        : QuartzText.of("Just text", "Sam tekst"), left, start, half, setting.background, () -> {
            if (module.icons()) setting.background = !setting.background;
            else if (setting.background) {
                setting.background = false;
                setting.brackets = true;
            } else if (setting.brackets) setting.brackets = false;
            else setting.background = true;
            rebuildWidgets();
        });
        option(setting.shadow ? QuartzText.of("Shadow on", "Cień wł.") : QuartzText.of("Shadow off", "Cień wył."),
                left + half + 6, start, half, setting.shadow, () -> {
            setting.shadow = !setting.shadow;
            rebuildWidgets();
        });
        field(left, top + 93 - scroll, half, QuartzText.of("Background color", "Kolor tła"), hex(setting.bg), 9,
                value -> setColor(value, color -> setting.bg = color));
        field(left + half + 6, top + 93 - scroll, half, QuartzText.of("Text color", "Kolor tekstu"), hex(setting.fg), 9,
                value -> setColor(value, color -> setting.fg = color));
        int y = top + 139 - scroll;
        if (!module.icons()) {
            field(left, y, controls, QuartzText.of("Template", "Wzór tekstu"), setting.text == null ? module.template : setting.text,
                    72, value -> setting.text = value);
        } else if (module != QuartzHudModule.ARMOR_BAR) {
            Component value = module == QuartzHudModule.ARMOR ? QuartzText.of("Durability", "Trwałość") : QuartzText.of("Timer", "Czas");
            option(Component.empty().append(value).append(setting.showValue ? " ✓" : " ×"), left, y, controls, setting.showValue, () -> {
                setting.showValue = !setting.showValue;
                rebuildWidgets();
            });
        }
        if (module == QuartzHudModule.ITEM_COUNT) {
            field(left, top + 185 - scroll, controls, QuartzText.of("Item ID", "ID przedmiotu"),
                    setting.target == null ? "" : setting.target, 80, value -> setting.target = value.strip());
        }
        add(QuartzText.of("Move HUD", "Przesuń HUD"), left, height - 27, Math.min(110, controls), false,
                () -> minecraft.gui.setScreen(new QuartzHudEditorScreen(this, module)));
        add(QuartzText.of("Done", "Gotowe"), left + span - 62, height - 27, 62, true, this::onClose);
    }

    private boolean inside(int y) { return y >= top + 30 && y + 19 <= contentBottom; }

    private void field(int x, int y, int w, Component label, String value, int max, Consumer<String> onChange) {
        if (!inside(y)) return;
        var field = addRenderableWidget(new EditBox(font, x, y, w, 19, label));
        field.setMaxLength(max);
        field.setValue(value);
        field.setHint(label);
        field.setResponder(onChange);
    }

    private void option(Component label, int x, int y, int w, boolean accent, Runnable action) {
        if (inside(y)) add(label, x, y, w, accent, action);
    }

    private void add(Component label, int x, int y, int w, boolean accent, Runnable action) {
        addRenderableWidget(new QuartzButton(label, x, y, w, 19, accent, action));
    }

    private static String hex(int color) { return String.format(Locale.ROOT, "%08X", color); }

    private static void setColor(String value, java.util.function.IntConsumer accept) {
        String hex = value.strip().replaceFirst("^#", "");
        if (!hex.matches("(?i)[0-9a-f]{6}([0-9a-f]{2})?")) return;
        accept.accept((int) Long.parseLong(hex.length() == 6 ? "FF" + hex : hex, 16));
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int next = Math.max(0, Math.min(Math.max(0, top + 212 - contentBottom), scroll + (dy < 0 ? 19 : -19)));
        if (dy == 0 || next == scroll) return super.mouseScrolled(x, y, dx, dy);
        scroll = next;
        rebuildWidgets();
        return true;
    }

    @Override protected void drawQuartz(GuiGraphicsExtractor g, int mx, int my, float delta) {
        QuartzTheme.background(g, width, height);
        QuartzTheme.card(g, left, top, span, 26, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
        g.text(font, font.plainSubstrByWidth(title.getString(), Math.max(0, span / 2 - 22)), left + 28, top + 9, QuartzTheme.ACCENT, false);
        int x = left + controls + 12, y = top + 31, h = Math.max(42, contentBottom - y);
        QuartzTheme.card(g, x, y, preview, h, 0xFFEADAF4, QuartzTheme.OUTLINE);
        g.text(font, QuartzText.of("Live preview", "Podgląd"), x + 7, y + 8, QuartzTheme.ACCENT, false);
        var panel = QuartzHud.panel(module, minecraft, true, preview - 8, Math.max(27, h - 23));
        g.enableScissor(x + 2, y + 22, x + preview - 2, y + h - 2);
        try {
            QuartzHud.draw(g, minecraft, module, panel, x + (preview - panel.width()) / 2,
                    y + 22 + Math.max(0, (h - 22 - panel.height()) / 2));
        } finally {
            g.disableScissor();
        }
        if (inside(top + 49 - scroll)) g.text(font, QuartzText.of("Style", "Styl"), left, top + 36 - scroll, QuartzTheme.ACCENT, false);
        int half = (controls - 6) / 2;
        if (inside(top + 93 - scroll)) {
            g.text(font, QuartzText.of("Background AARRGGBB", "Tło AARRGGBB"), left, top + 81 - scroll, QuartzTheme.MUTED, false);
            g.text(font, QuartzText.of("Text AARRGGBB", "Tekst AARRGGBB"), left + half + 6, top + 81 - scroll, QuartzTheme.MUTED, false);
        }
        if (!module.icons() && inside(top + 139 - scroll))
            g.text(font, QuartzText.of("Text · {value} inserts the live value", "Tekst · {value} wstawia aktualną wartość"), left, top + 127 - scroll, QuartzTheme.MUTED, false);
        if (module == QuartzHudModule.ITEM_COUNT && inside(top + 185 - scroll))
            g.text(font, QuartzText.of("Item ID · blank uses held item", "ID przedmiotu · puste liczy trzymany"), left, top + 173 - scroll, QuartzTheme.MUTED, false);
        drawWidgets(g, mx, my, delta);
    }

    @Override public void removed() { QuartzConfig.save(); }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
