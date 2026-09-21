package com.gregtechceu.gtceu.common.network.packets.hazard;

import com.gregtechceu.gtceu.client.EnvironmentalHazardClientHandler;
import com.gregtechceu.gtceu.common.capability.EnvironmentalHazardSavedData;
import com.gregtechceu.gtceu.common.network.GTNetwork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class SPacketAddHazardZone implements GTNetwork.INetPacket {

    private ChunkPos pos;
    private EnvironmentalHazardSavedData.HazardZone zone;

    public SPacketAddHazardZone(RegistryFriendlyByteBuf buf) {
        pos = buf.readChunkPos();
        zone = EnvironmentalHazardSavedData.HazardZone.fromNetwork(buf);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeChunkPos(pos);
        zone.toNetwork(buf);
    }

    @Override
    public void execute(IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            EnvironmentalHazardClientHandler.INSTANCE.addHazardZone(pos, zone);
        }
    }
}
