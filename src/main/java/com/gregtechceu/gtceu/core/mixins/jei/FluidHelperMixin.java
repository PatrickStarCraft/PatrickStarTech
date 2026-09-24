package com.gregtechceu.gtceu.core.mixins.jei;

import com.gregtechceu.gtceu.client.TooltipsHandler;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "mezz.jei.neoforge.platform.FluidHelper", remap = false)
public class FluidHelperMixin {

    @Inject(method = "getTooltip(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
            at = @At("TAIL"),
            require = 0)
    private void gtceu$injectFluidTooltips(FluidStack ingredient, Player player, Item.TooltipContext tooltipContext,
                                           TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> tooltips = new ArrayList<>(cir.getReturnValue());
        TooltipsHandler.appendFluidTooltips(ingredient, tooltips::add, tooltipFlag);
        cir.setReturnValue(tooltips);
    }
}
