package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.api.misc.ImageCache;
import com.gregtechceu.gtceu.common.network.GTNetwork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;

public class CPacketImageRequest implements GTNetwork.INetPacket {

    private final String url;

    public CPacketImageRequest(String url) {
        this.url = url;
    }

    public CPacketImageRequest(RegistryFriendlyByteBuf buf) {
        this.url = buf.readUtf();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(url);
    }

    @Override
    public void execute(IPayloadContext context) {
        ImageCache.queryServerImage(url, image -> {
            try {
                SPacketImageResponse.sendImage(url, image, context);
            } catch (IOException ignored) {}
        });
    }
}
