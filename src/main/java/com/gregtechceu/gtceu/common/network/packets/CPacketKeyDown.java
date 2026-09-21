package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMapping;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

public class CPacketKeyDown implements GTNetwork.INetPacket {

    private final Int2BooleanMap updateKeys;

    public CPacketKeyDown(Int2BooleanMap updateKeys) {
        this.updateKeys = updateKeys;
    }

    public CPacketKeyDown(RegistryFriendlyByteBuf buf) {
        this.updateKeys = new Int2BooleanOpenHashMap();
        int size = buf.readInt();
        if (size < 0 || size > 256) throw new IllegalArgumentException("Invalid key update count: " + size);
        for (int i = 0; i < size; i++) {
            updateKeys.put(buf.readInt(), buf.readBoolean());
        }
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(updateKeys.size());
        for (var entry : updateKeys.int2BooleanEntrySet()) {
            buf.writeInt(entry.getIntKey());
            buf.writeBoolean(entry.getBooleanValue());
        }
    }

    @Override
    public void execute(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            for (var entry : updateKeys.int2BooleanEntrySet()) {
                SyncedKeyMapping keyMapping = SyncedKeyMapping.getFromSyncId(entry.getIntKey());
                if (keyMapping != null) keyMapping.serverActivate(entry.getBooleanValue(), player);
            }
        }
    }
}
