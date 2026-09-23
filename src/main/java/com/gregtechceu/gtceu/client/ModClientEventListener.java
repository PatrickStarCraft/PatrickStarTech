package com.gregtechceu.gtceu.client;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTMenuTypes;

import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import brachy.modularui.screen.ContainerScreenWrapper;
import brachy.modularui.screen.ModularContainerMenu;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public class ModClientEventListener {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(GTMenuTypes.MODULAR_CONTAINER.get(),
                (MenuScreens.ScreenConstructor<ModularContainerMenu, ContainerScreenWrapper>) ContainerScreenWrapper::new);
    }

}
