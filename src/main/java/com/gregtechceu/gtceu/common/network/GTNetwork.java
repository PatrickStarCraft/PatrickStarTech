package com.gregtechceu.gtceu.common.network;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.network.packets.*;
import com.gregtechceu.gtceu.common.network.packets.hazard.*;
import com.gregtechceu.gtceu.common.network.packets.prospecting.*;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class GTNetwork {

    private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new ConcurrentHashMap<>();

    public static void sendToServer(INetPacket packet) {
        ClientPacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayersInLevel(ResourceKey<Level> level, INetPacket packet) {
        ServerLevel serverLevel = Objects.requireNonNull(GTCEu.getMinecraftServer()).getLevel(level);
        if (serverLevel != null) PacketDistributor.sendToPlayersInDimension(serverLevel, packet);
    }

    public static void sendToPlayersNearPoint(ServerLevel level, ServerPlayer excluded,
                                             double x, double y, double z, double radius, INetPacket packet) {
        PacketDistributor.sendToPlayersNear(level, excluded, x, y, z, radius, packet);
    }

    public static void sendToAllPlayersTrackingEntity(Entity entity, boolean includeSelf, INetPacket packet) {
        if (includeSelf) PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
        else PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }

    public static void sendToAllPlayersTrackingChunk(LevelChunk chunk, INetPacket packet) {
        if (chunk.getLevel() instanceof ServerLevel level) {
            PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), packet);
        }
    }

    public static void sendToAll(INetPacket packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendToPlayer(ServerPlayer player, INetPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void reply(IPayloadContext context, INetPacket packet) {
        context.reply(packet);
    }

    public interface INetPacket extends CustomPacketPayload {
        void encode(RegistryFriendlyByteBuf buffer);
        void execute(IPayloadContext context);

        @Override
        default Type<? extends CustomPacketPayload> type() {
            return Objects.requireNonNull(TYPES.get(getClass()), "Unregistered GT payload: " + getClass().getName());
        }
    }

    private static <T extends INetPacket> void register(PayloadRegistrar registrar, String name,
                                                       Class<T> cls, Function<RegistryFriendlyByteBuf, T> decode,
                                                       PacketFlow direction) {
        CustomPacketPayload.Type<T> type = new CustomPacketPayload.Type<>(GTCEu.id(name));
        if (TYPES.putIfAbsent(cls, type) != null) throw new IllegalStateException("Duplicate GT payload: " + name);
        StreamCodec<RegistryFriendlyByteBuf, T> codec = StreamCodec.of((buf, packet) -> packet.encode(buf), decode::apply);
        // The registrar defaults to MAIN; handlers may safely access worlds and inventories.
        if (direction == PacketFlow.CLIENTBOUND) registrar.playToClient(type, codec, INetPacket::execute);
        else if (direction == PacketFlow.SERVERBOUND) registrar.playToServer(type, codec, INetPacket::execute);
        else registrar.playBidirectional(type, codec, INetPacket::execute, INetPacket::execute);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("26.2-1");
        register(registrar, "monitor_group_change", SCPacketMonitorGroupNBTChange.class, SCPacketMonitorGroupNBTChange::new, null);
        register(registrar, "image_request", CPacketImageRequest.class, CPacketImageRequest::new, PacketFlow.SERVERBOUND);
        register(registrar, "image_response", SPacketImageResponse.class, SPacketImageResponse::new, PacketFlow.CLIENTBOUND);
        register(registrar, "key_down", CPacketKeyDown.class, CPacketKeyDown::new, PacketFlow.SERVERBOUND);
        register(registrar, "ore_veins", SPacketSyncOreVeins.class, SPacketSyncOreVeins::new, PacketFlow.CLIENTBOUND);
        register(registrar, "fluid_veins", SPacketSyncFluidVeins.class, SPacketSyncFluidVeins::new, PacketFlow.CLIENTBOUND);
        register(registrar, "bedrock_ore_veins", SPacketSyncBedrockOreVeins.class, SPacketSyncBedrockOreVeins::new, PacketFlow.CLIENTBOUND);
        register(registrar, "add_hazard_zone", SPacketAddHazardZone.class, SPacketAddHazardZone::new, PacketFlow.CLIENTBOUND);
        register(registrar, "remove_hazard_zone", SPacketRemoveHazardZone.class, SPacketRemoveHazardZone::new, PacketFlow.CLIENTBOUND);
        register(registrar, "hazard_strength", SPacketSyncHazardZoneStrength.class, SPacketSyncHazardZoneStrength::new, PacketFlow.CLIENTBOUND);
        register(registrar, "level_hazards", SPacketSyncLevelHazards.class, SPacketSyncLevelHazards::new, PacketFlow.CLIENTBOUND);
        register(registrar, "prospect_ore", SPacketProspectOre.class, SPacketProspectOre::new, PacketFlow.CLIENTBOUND);
        register(registrar, "prospect_bedrock_ore", SPacketProspectBedrockOre.class, SPacketProspectBedrockOre::new, PacketFlow.CLIENTBOUND);
        register(registrar, "prospect_bedrock_fluid", SPacketProspectBedrockFluid.class, SPacketProspectBedrockFluid::new, PacketFlow.CLIENTBOUND);
        register(registrar, "world_id", SPacketSendWorldID.class, SPacketSendWorldID::new, PacketFlow.CLIENTBOUND);
        register(registrar, "cape_change", SPacketNotifyCapeChange.class, SPacketNotifyCapeChange::new, PacketFlow.CLIENTBOUND);
        register(registrar, "share_prospection", SCPacketShareProspection.class, SCPacketShareProspection::new, null);
        register(registrar, "start_prospection_share", SPacketStartProspectionShare.class, SPacketStartProspectionShare::new, PacketFlow.CLIENTBOUND);
    }
}
