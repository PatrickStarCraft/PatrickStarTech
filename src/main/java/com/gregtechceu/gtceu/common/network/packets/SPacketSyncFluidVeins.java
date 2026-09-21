package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.common.network.GTNetwork;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class SPacketSyncFluidVeins implements GTNetwork.INetPacket {

    private final Map<Identifier, BedrockFluidDefinition> veins;

    @SuppressWarnings("unused")
    public SPacketSyncFluidVeins() {
        this.veins = new HashMap<>();
    }

    public SPacketSyncFluidVeins(RegistryFriendlyByteBuf buf) {
        this();
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, GTRegistries.builtinRegistry());
        Stream.generate(() -> {
            Identifier id = buf.readIdentifier();
            CompoundTag tag = buf.readNbt();
            BedrockFluidDefinition def = BedrockFluidDefinition.FULL_CODEC.parse(ops, tag).getOrThrow(message -> { GTCEu.LOGGER.error(message); return new RuntimeException(message); });
            return Map.entry(id, def);
        }).limit(buf.readVarInt()).forEach(entry -> veins.put(entry.getKey(), entry.getValue()));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, GTRegistries.builtinRegistry());
        int size = veins.size();
        buf.writeVarInt(size);
        for (var entry : veins.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            CompoundTag tag = (CompoundTag) BedrockFluidDefinition.FULL_CODEC.encodeStart(ops, entry.getValue())
                    .getOrThrow(message -> { GTCEu.LOGGER.error(message); return new RuntimeException(message); });
            buf.writeNbt(tag);
        }
    }

    @Override
    public void execute(IPayloadContext context) {
        ClientProxy.CLIENT_FLUID_VEINS.clear();
        ClientProxy.CLIENT_FLUID_VEINS.putAll(veins);
    }
}
