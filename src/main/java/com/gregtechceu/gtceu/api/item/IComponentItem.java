package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public interface IComponentItem extends ItemLike {

    List<IItemComponent> getComponents();

    void attachComponents(IItemComponent... components);

    default void fillItemCategory(CreativeModeTab category, NonNullList<ItemStack> items) {}
}
