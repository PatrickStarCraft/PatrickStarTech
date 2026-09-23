package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.client.color.GTItemTintSources;

import net.minecraft.client.ClientBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientBootstrap.class)
public abstract class ClientBootstrapMixin {

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void gtceu$registerItemTintSources(CallbackInfo ci) {
        GTItemTintSources.bootstrap();
    }
}
