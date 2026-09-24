package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.api.cosmetics.CapeRegistry;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies the currently selected GregTech cape to the extracted player skin. */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerCapeMixin {

    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    private PlayerSkin gtceu$applySelectedCape(PlayerSkin original) {
        Identifier cape = CapeRegistry.getPlayerCapeTexture(((AbstractClientPlayer) (Object) this).getUUID());
        if (cape == null) return original;
        return new PlayerSkin(original.body(), new ClientAsset.ResourceTexture(cape, cape), original.elytra(),
                original.model(), original.secure());
    }
}
