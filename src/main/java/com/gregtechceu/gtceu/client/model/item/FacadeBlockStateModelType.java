package com.gregtechceu.gtceu.client.model.item;

import com.gregtechceu.gtceu.GTCEu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class FacadeBlockStateModelType {

    private FacadeBlockStateModelType() {}

    @SubscribeEvent
    public static void register(RegisterBlockStateModels event) {
        event.registerModel(GTCEu.id("facade"), UnbakedFacadeBlockStateModel.CODEC);
    }
}
