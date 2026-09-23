package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.misc.FilteredFluidResourceHandler;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class FilteredFluidContainer implements IItemComponent, IAddInformation {

    public final int capacity;
    public final boolean allowPartialFill;
    public Predicate<FluidStack> filter;

    public FilteredFluidContainer(int capacity, boolean allowPartialFill, Predicate<FluidStack> filter) {
        this.allowPartialFill = allowPartialFill;
        this.capacity = capacity;
        this.filter = filter;
    }

    public ResourceHandler<FluidResource> createHandler(ItemAccess access) {
        return new FilteredFluidResourceHandler(access, capacity, filter);
    }

    @Nullable
    public static FilteredFluidContainer find(Item item) {
        if (item instanceof IComponentItem componentItem) {
            for (IItemComponent component : componentItem.getComponents()) {
                if (component instanceof FilteredFluidContainer fluidContainer) {
                    return fluidContainer;
                }
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        FluidUtil.getFluidContained(stack).ifPresent(fluid -> tooltipComponents
                .add(Component.translatable("gtceu.universal.tooltip.fluid_stored", fluid.getHoverName(),
                        fluid.getAmount())));
    }
}
