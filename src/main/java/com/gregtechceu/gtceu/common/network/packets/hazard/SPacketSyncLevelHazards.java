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

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
public class SPacketSyncLevelHazards implements GTNetwork.INetPacket {

    private Map<ChunkPos, EnvironmentalHazardSavedData.HazardZone> map;

    public SPacketSyncLevelHazards(RegistryFriendlyByteBuf buf) {
        map = Stream.generate(() -> {
            ChunkPos pos = buf.readChunkPos();
            var zone = EnvironmentalHazardSavedData.HazardZone.fromNetwork(buf);
            return Map.entry(pos, zone);
        }).limit(buf.readVarInt()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(map.size());
        for (var entry : map.entrySet()) {
            buf.writeChunkPos(entry.getKey());
            entry.getValue().toNetwork(buf);
        }
    }

    @Override
    public void execute(IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            EnvironmentalHazardClientHandler.INSTANCE.updateHazardMap(this.map);
        }
    }
}
