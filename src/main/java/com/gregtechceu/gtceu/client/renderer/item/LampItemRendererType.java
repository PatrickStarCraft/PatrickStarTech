package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.GTCEu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class LampItemRendererType {

    private LampItemRendererType() {}

    @SubscribeEvent
    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(GTCEu.id("lamp"), LampItemRenderer.Unbaked.MAP_CODEC);
    }
}
