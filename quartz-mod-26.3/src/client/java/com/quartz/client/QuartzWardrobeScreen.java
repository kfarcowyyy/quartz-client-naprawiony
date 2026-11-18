package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public final class QuartzWardrobeScreen extends Screen {
    private final Screen parent;
    private Component error;

    public QuartzWardrobeScreen(Screen parent) {
        super(QuartzText.of("Cosmetics", "Kosmetyki"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QuartzWardrobe.refresh();
        QuartzWardrobe.Equipped look = QuartzWardrobe.equipped();
        int buttonWidth = Math.min(224, width - 24);
        int left = (width - buttonWidth) / 2;
        int top = height / 2 - 49;
        addChoice(left, top, buttonWidth, QuartzText.of("Skin", "Skin"), QuartzWardrobe.SKINS, look.skin(),
                value -> new QuartzWardrobe.Equipped(value, look.cape(), "account".equals(value) ? "none" : look.accessory()));
        addChoice(left, top + 26, buttonWidth, QuartzText.of("Cape", "Peleryna"), QuartzWardrobe.CAPES, look.cape(),
                value -> new QuartzWardrobe.Equipped(look.skin(), value, look.accessory()));
        addChoice(left, top + 52, buttonWidth, QuartzText.of("Accessory", "Dodatek"), QuartzWardrobe.ACCESSORIES, look.accessory(),
                value -> new QuartzWardrobe.Equipped(look.skin(), look.cape(), value));
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), button -> onClose())
                .bounds(left, top + 87, buttonWidth, 20).build());
    }

    private void addChoice(int x, int y, int width, Component label, List<QuartzWardrobe.Item> options,
                           String equipped, Function<String, QuartzWardrobe.Equipped> change) {
        int index = 0;
        while (!options.get(index).id().equals(equipped)) index++;
        QuartzWardrobe.Item item = options.get(index);
        QuartzWardrobe.Item next = options.get((index + 1) % options.size());
        Component text = Component.empty().append(label).append(": ").append(QuartzText.of(item.english(), item.polish()));
        addRenderableWidget(Button.builder(text, button -> {
            try {
                QuartzWardrobe.equip(change.apply(next.id()));
                Minecraft.getInstance().gui.setScreen(new QuartzWardrobeScreen(parent));
            } catch (IOException error) {
                this.error = QuartzText.of("Could not save your look", "Nie udało się zapisać wyglądu");
            }
        }).bounds(x, y, width, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        QuartzGraphics.center(graphics, font, title, width / 2, Math.max(3, height / 2 - 74), 0xFFF3EDFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (error != null) QuartzGraphics.center(graphics, font, error, width / 2, height - 13, 0xFFFFA5AF);
    }

    @Override
    public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
