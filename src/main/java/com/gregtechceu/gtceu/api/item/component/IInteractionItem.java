package com.gregtechceu.gtceu.api.item.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import org.jetbrains.annotations.Nullable;

public interface IInteractionItem extends IItemComponent {

    default InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        return InteractionResult.PASS;
    }

    default InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    default InteractionResult use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
        return consumable != null ? consumable.startConsuming(player, itemStack, usedHand) : InteractionResult.PASS;
    }

    default ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    default @Nullable ItemUseAnimation getUseAnimation(ItemStack stack) {
        return null;
    }

    default void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {}

    default InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget,
                                                   InteractionHand usedHand) {
        return InteractionResult.PASS;
    }

    default boolean sneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return false;
    }

    default boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return false;
    }
}
