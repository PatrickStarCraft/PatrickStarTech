package com.gregtechceu.gtceu.api.item.component;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public interface IAddInformation extends IItemComponent {

    void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                         TooltipFlag isAdvanced);

    default void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                 Consumer<Component> tooltip, TooltipFlag flag) {
        List<Component> components = new ArrayList<>();
        appendHoverText(stack, null, components, flag);
        components.forEach(tooltip);
    }
}
