package com.quartz.client;

import com.quartz.network.QuartzWardrobeSync.Appearance;
import com.quartz.network.QuartzWardrobeSync.Equip;
import com.quartz.network.QuartzWardrobeSync.Look;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuartzMultiplayerWardrobe {
    private static final Map<UUID, QuartzWardrobe.Textures> REMOTE = new HashMap<>();
    private static Look sent;
    private static int ticks;

    private QuartzMultiplayerWardrobe() {}

    public static void initialize() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            REMOTE.clear();
            sent = null;
        });
        ClientPlayNetworking.registerGlobalReceiver(Appearance.TYPE, (packet, context) -> {
            Look look = packet.look();
            if (Look.DEFAULT.equals(look)) {
                REMOTE.remove(packet.player());
                return;
            }
            var equipped = new QuartzWardrobe.Equipped(
                    id(QuartzWardrobe.SKINS, look.skin()), id(QuartzWardrobe.CAPES, look.cape()),
                    id(QuartzWardrobe.ACCESSORIES, look.accessory()));
            REMOTE.put(packet.player(), QuartzWardrobe.textures(equipped));
        });
    }

    private static String id(List<QuartzWardrobe.Item> options, int index) {
        return options.get(index >= 0 && index < options.size() ? index : 0).id();
    }

    private static int index(List<QuartzWardrobe.Item> options, String id) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().equals(id)) return i;
        }
        return 0;
    }

    public static void tick() {
        if (++ticks < 20) return;
        ticks = 0;
        if (!ClientPlayNetworking.canSend(Equip.TYPE)) return;
        var current = QuartzWardrobe.equipped();
        Look next = new Look(index(QuartzWardrobe.SKINS, current.skin()), index(QuartzWardrobe.CAPES, current.cape()),
                index(QuartzWardrobe.ACCESSORIES, current.accessory()));
        if (!next.equals(sent)) {
            ClientPlayNetworking.send(new Equip(next));
            sent = next;
        }
    }

    public static PlayerSkin apply(UUID id, PlayerSkin base) {
        var textures = REMOTE.get(id);
        return textures == null ? base : textures.apply(base);
    }
}
