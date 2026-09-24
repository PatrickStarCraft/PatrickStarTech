package com.gregtechceu.gtceu.api.item.tool.behavior;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Compile-only behavior contract; world interaction callbacks are not executed by this suite. */
public interface IToolBehavior {

    boolean shouldOpenUIAfterUse(UseOnContext context);

    InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context);

    void addInformation(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip,
                        @NotNull TooltipFlag flag);
}
