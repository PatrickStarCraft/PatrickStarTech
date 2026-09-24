package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.data.ItemStackData;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SmithingTransformRecipe.class)
public class SmithingTransformRecipeMixin {

    @ModifyReturnValue(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;",
                       at = @At("RETURN"))
    private ItemStack gtceu$fixGtToolSmithing(ItemStack output) {
        if (!(output.getItem() instanceof IGTTool gtTool)) return output;

        // Copy stats from the upgraded tool
        ItemStack newStack = ToolHelper.get(gtTool.getToolType(), gtTool.getMaterial());
        CompoundTag newStats = ItemStackData.read(newStack).getCompound("GT.Tool").orElse(null);
        if (newStats != null) {
            ItemStackData.update(output, tag -> tag.put("GT.Tool", newStats.copy()));
        }
        return output;
    }
}
