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
public class SPacketSyncHazardZoneStrength implements GTNetwork.INetPacket {

    public ChunkPos pos;
    public float newAmount;

    public SPacketSyncHazardZoneStrength(RegistryFriendlyByteBuf buf) {
        pos = buf.readChunkPos();
        this.newAmount = buf.readFloat();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeChunkPos(pos);
        buf.writeFloat(newAmount);
    }

    @Override
    public void execute(IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            EnvironmentalHazardClientHandler.INSTANCE.updateHazardStrength(pos, newAmount);
        }
    }
}
