package com.gregtechceu.gtceu.client.model.item;

import com.gregtechceu.gtceu.GTCEu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class FacadeItemModelType {

    private FacadeItemModelType() {}

    @SubscribeEvent
    public static void register(RegisterItemModelsEvent event) {
        event.register(GTCEu.id("facade"), FacadeItemModel.Unbaked.MAP_CODEC);
    }
}
