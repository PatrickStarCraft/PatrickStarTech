package com.gregtechceu.gtceu.common.network.packets.prospecting;

import com.gregtechceu.gtceu.api.item.component.prospector.ProspectorMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SPacketProspectBedrockOre extends SPacketProspect<ProspectorMode.BedrockOreInfo> {

    @SuppressWarnings("unused")
    public SPacketProspectBedrockOre() {
        super();
    }

    public SPacketProspectBedrockOre(RegistryFriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public void encodeData(RegistryFriendlyByteBuf buf, ProspectorMode.BedrockOreInfo data) {
        ProspectorMode.BEDROCK_ORE.serialize(data, buf);
    }

    @Override
    public ProspectorMode.BedrockOreInfo decodeData(RegistryFriendlyByteBuf buf) {
        return ProspectorMode.BEDROCK_ORE.deserialize(buf);
    }

    @Override
    public void execute(IPayloadContext context) {
        // todo: add cache for bedrock ore veins
    }
}
