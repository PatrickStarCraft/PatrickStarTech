package com.gregtechceu.gtceu.core.mixins.emi;

import com.gregtechceu.gtceu.client.TooltipsHandler;
import com.gregtechceu.gtceu.utils.GTMath;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

@Mixin(value = FluidEmiStack.class, remap = false)
public class FluidEmiStackMixin {

    @Shadow
    @Final
    private Fluid fluid;
    @Inject(method = "getTooltip",
            at = @At("RETURN"),
            remap = false,
            cancellable = true)
    private void gtceu$addFluidTooltip(CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        List<ClientTooltipComponent> list = new ArrayList<>(cir.getReturnValue());
        var components = ((FluidEmiStack) (Object) this).getComponentChanges();
        TooltipsHandler.appendFluidTooltips(new FluidStack(this.fluid,
                Math.max(GTMath.saturatedCast(((EmiStack) (Object) this).getAmount()), 1),
                components),
                text -> list.add(EmiTooltipComponents.of(text)),
                TooltipFlag.NORMAL);
        cir.setReturnValue(list);
    }
}
