package com.gregtechceu.gtceu.common.network.packets.hazard;

import com.gregtechceu.gtceu.client.EnvironmentalHazardClientHandler;
import com.gregtechceu.gtceu.common.network.GTNetwork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class SPacketRemoveHazardZone implements GTNetwork.INetPacket {

    public ChunkPos pos;

    public SPacketRemoveHazardZone(RegistryFriendlyByteBuf buf) {
        pos = buf.readChunkPos();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeChunkPos(pos);
    }

    @Override
    public void execute(IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            EnvironmentalHazardClientHandler.INSTANCE.removeHazardZone(pos);
        }
    }
}
