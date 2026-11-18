package com.quartz.network;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuartzWardrobeSync implements ModInitializer {
    public record Look(int skin, int cape, int accessory) {
        public static final Look DEFAULT = new Look(0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Look> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Look::skin, ByteBufCodecs.VAR_INT, Look::cape,
                ByteBufCodecs.VAR_INT, Look::accessory, Look::new);

        boolean valid() {
            return skin >= 0 && skin <= 3 && cape >= 0 && cape <= 3 && accessory >= 0 && accessory <= 2
                    && (skin != 0 || accessory == 0);
        }
    }

    public record Equip(Look look) implements CustomPacketPayload {
        public static final Type<Equip> TYPE = new Type<>(Identifier.fromNamespaceAndPath("quartz", "equip_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Equip> CODEC = Look.CODEC.map(Equip::new, Equip::look);
        @Override public Type<Equip> type() { return TYPE; }
    }

    public record Appearance(UUID player, Look look) implements CustomPacketPayload {
        public static final Type<Appearance> TYPE = new Type<>(Identifier.fromNamespaceAndPath("quartz", "appearance_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Appearance> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Appearance::player, Look.CODEC, Appearance::look, Appearance::new);
        @Override public Type<Appearance> type() { return TYPE; }
    }

    private record Selection(Look look, long updated) {}

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(Equip.TYPE, Equip.CODEC);
        PayloadTypeRegistry.playS2C().register(Appearance.TYPE, Appearance.CODEC);

        Map<ServerPlayer, Selection> players = new HashMap<>();
        ServerPlayNetworking.registerGlobalReceiver(Equip.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            if (!packet.look().valid() || !ServerPlayNetworking.canSend(player, Appearance.TYPE)) return;
            long now = System.nanoTime();
            Selection previous = players.get(player);
            if (previous != null && (previous.look().equals(packet.look()) || now - previous.updated() < 500_000_000L)) return;
            players.put(player, new Selection(packet.look(), now));
            Appearance change = new Appearance(player.getUUID(), packet.look());
            for (ServerPlayer other : context.server().getPlayerList().getPlayers()) {
                if (other != player && ServerPlayNetworking.canSend(other, Appearance.TYPE)) {
                    ServerPlayNetworking.send(other, change);
                }
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler, Appearance.TYPE)) return;
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                Selection selection = players.get(other);
                if (selection != null) sender.sendPacket(new Appearance(other.getUUID(), selection.look()));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (players.remove(handler.player) == null) return;
            Appearance reset = new Appearance(handler.player.getUUID(), Look.DEFAULT);
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (other != handler.player && ServerPlayNetworking.canSend(other, Appearance.TYPE)) {
                    ServerPlayNetworking.send(other, reset);
                }
            }
        });
    }
}
