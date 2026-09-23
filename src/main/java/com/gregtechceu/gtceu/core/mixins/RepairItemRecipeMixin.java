package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.item.IGTTool;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin extends CustomRecipe {

    /**
     * It's a hack to prevent the tool from being returned
     * 
     * @param input the crafting input
     */
    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput input) {
        var result = super.getRemainingItems(input);
        for (ItemStack stack : result) {
            if (stack.getItem() instanceof IGTTool) {
                stack.setCount(0);
            }
        }
        return result;
    }

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    public void gtceu$matches(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || input.ingredientCount() != 2) return;

        ItemStack first = ItemStack.EMPTY;
        ItemStack second = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (first.isEmpty()) first = stack;
                else {
                    second = stack;
                    break;
                }
            }
        }

        if (first.getItem() instanceof IGTTool firstTool && second.getItem() instanceof IGTTool secondTool) {
            // do not allow repairing electric tools
            if (firstTool.isElectric() || secondTool.isElectric()) {
                cir.setReturnValue(false);
            }
            // do not allow repairing tools if both have full durability
            if (!first.isDamaged() && !second.isDamaged()) {
                cir.setReturnValue(false);
            }
        }
    }

    @WrapOperation(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",
                   at = @At(value = "NEW", target = "net/minecraft/world/item/ItemStack"))
    private ItemStack gtceu$copyToolItem(ItemLike item, Operation<ItemStack> original) {
        if (item instanceof IGTTool tool) {
            return tool.get();
        }
        return original.call(item);
    }
}
