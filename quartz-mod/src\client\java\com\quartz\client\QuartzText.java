package com.quartz.client;

import net.minecraft.network.chat.Component;

public final class QuartzText {
    private QuartzText() {}
    public static Component of(String en, String pl) {
        return Component.literal("pl".equals(QuartzConfig.data.language) ? pl : en);
    }
}
