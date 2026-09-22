package com.gregtechceu.gtceu.common.item.behavior;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class ConsumedBehaviour {

    public final int totalUses;

    public ConsumedBehaviour(int totalUses) {
        this.totalUses = totalUses;
    }

    public boolean useItemDurability(Player player, InteractionHand hand, ItemStack stack, ItemStack replacementStack) {
        int usesLeft = getUsesLeft(stack);
        if (!player.isCreative()) {
            if (--usesLeft <= 0) {
                if (replacementStack.isEmpty()) {
                    // if replacement stack is empty, just shrink resulting stack
                    stack.shrink(1);
                } else {
                    // otherwise, update held item to replacement stack
                    player.setItemInHand(hand, replacementStack);
                }
                return true;
            }
            setUsesLeft(stack, usesLeft);
        }
        return true;
    }

    public final int getUsesLeft(ItemStack stack) {
        return com.gregtechceu.gtceu.api.item.data.ItemStackData.read(stack).getIntOr("GT.UsesLeft", totalUses);
    }

    public static void setUsesLeft(ItemStack itemStack, int usesLeft) {
        com.gregtechceu.gtceu.api.item.data.ItemStackData.update(itemStack, tag -> tag.putInt("GT.UsesLeft", usesLeft));
    }
}
