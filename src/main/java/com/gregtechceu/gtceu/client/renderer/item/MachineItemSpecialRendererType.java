package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.GTCEu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class MachineItemSpecialRendererType {

    private MachineItemSpecialRendererType() {}

    @SubscribeEvent
    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(GTCEu.id("machine_dynamic"), MachineItemSpecialRenderer.Unbaked.MAP_CODEC);
    }
}
