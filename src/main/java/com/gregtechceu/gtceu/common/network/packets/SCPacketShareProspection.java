package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.integration.map.ClientCacheManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import lombok.AllArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
public class SCPacketShareProspection implements GTNetwork.INetPacket {

    private UUID sender;
    private UUID receiver;
    private String cacheName;
    private String key;
    private boolean isDimCache;
    private ResourceKey<Level> dimension;
    private CompoundTag data;
    private boolean first;

    @SuppressWarnings("unused")
    public SCPacketShareProspection() {}

    public SCPacketShareProspection(RegistryFriendlyByteBuf buf) {
        sender = buf.readUUID();
        receiver = buf.readUUID();
        cacheName = buf.readUtf();
        key = buf.readUtf();
        isDimCache = buf.readBoolean();
        dimension = buf.readResourceKey(Registries.DIMENSION);
        data = buf.readNbt();
        first = buf.readBoolean();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(sender);
        buf.writeUUID(receiver);
        buf.writeUtf(cacheName);
        buf.writeUtf(key);
        buf.writeBoolean(isDimCache);
        buf.writeResourceKey(dimension);
        buf.writeNbt(data);
        buf.writeBoolean(first);
    }

    @Override
    public void execute(IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            if (first) {
                PlayerInfo senderInfo = Objects.requireNonNull(Minecraft.getInstance().getConnection())
                        .getPlayerInfo(sender);
                if (senderInfo == null) {
                    return;
                }

                Component playerName = senderInfo.getTabListDisplayName() != null ? senderInfo.getTabListDisplayName() :
                        Component.literal(senderInfo.getProfile().name());

                assert Minecraft.getInstance().player != null;
                Minecraft.getInstance().player.sendSystemMessage(Component
                        .translatable("command.gtceu.share_prospection_data.notification", playerName));
            }
            ClientCacheManager.processProspectionShare(cacheName, key, isDimCache, dimension, data);
        } else {
            var recipient = GTCEu.getMinecraftServer().getPlayerList().getPlayer(receiver);
            if (recipient == null) return;
            SCPacketShareProspection newPacket = new SCPacketShareProspection(context.player().getUUID(), receiver,
                    cacheName, key,
                    isDimCache, dimension,
                    data, first);
            GTNetwork.sendToPlayer(recipient, newPacket);
        }
    }
}
