package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.api.cosmetics.CapeRegistry;
import com.gregtechceu.gtceu.common.network.GTNetwork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class SPacketNotifyCapeChange implements GTNetwork.INetPacket {

    public UUID uuid;
    public Identifier cape;

    public SPacketNotifyCapeChange(RegistryFriendlyByteBuf buf) {
        uuid = buf.readUUID();
        cape = buf.readBoolean() ? buf.readIdentifier() : null;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.uuid);
        buf.writeBoolean(this.cape != null);
        if (this.cape != null) {
            buf.writeIdentifier(this.cape);
        }
    }

    @Override
    public void execute(IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            CapeRegistry.giveRawCape(uuid, cape);
        }
    }
}
