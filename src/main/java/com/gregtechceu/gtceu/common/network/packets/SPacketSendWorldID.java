package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.common.capability.WorldIDSaveData;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.integration.map.ClientCacheManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SPacketSendWorldID implements GTNetwork.INetPacket {

    private String worldId;

    public SPacketSendWorldID(RegistryFriendlyByteBuf buf) {
        worldId = buf.readUtf();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(WorldIDSaveData.getWorldID());
    }

    @Override
    public void execute(IPayloadContext context) {
        ClientCacheManager.init(worldId);
    }
}
