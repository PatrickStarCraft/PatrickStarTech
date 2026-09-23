package com.gregtechceu.gtceu.api.misc;

import com.gregtechceu.gtceu.common.data.GTDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Transactional fluid storage for component-backed fluid containers, with lazy migration from the old
 * {@code CustomData.Fluid} representation.
 */
public final class FilteredFluidResourceHandler extends ItemAccessResourceHandler<FluidResource> {

    private static final String LEGACY_FLUID_KEY = "Fluid";
    private static final String LEGACY_FLUID_NAME_KEY = "FluidName";
    private static final String LEGACY_AMOUNT_KEY = "Amount";
    private static final String LEGACY_TAG_KEY = "Tag";

    private final Item validItem;
    private final DataComponentType<SimpleFluidContent> component;
    private final int capacity;
    private final Predicate<FluidStack> filter;

    public FilteredFluidResourceHandler(ItemAccess access, int capacity, Predicate<FluidStack> filter) {
        super(access, 1);
        this.validItem = access.getResource().getItem();
        this.component = GTDataComponents.FLUID_CONTENT.get();
        this.capacity = capacity;
        this.filter = filter;
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        if (!accessResource.is(validItem)) return FluidResource.EMPTY;
        if (isComponentExplicitlyPresent(accessResource)) {
            return FluidResource.of(accessResource.getOrDefault(component, SimpleFluidContent.EMPTY).copy());
        }
        FluidStack legacyStack = getLegacyFluid(accessResource);
        if (!legacyStack.isEmpty()) {
            return FluidResource.of(legacyStack);
        }
        return FluidResource.of(accessResource.getOrDefault(component, SimpleFluidContent.EMPTY).copy());
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        if (!accessResource.is(validItem)) return 0;
        if (isComponentExplicitlyPresent(accessResource)) {
            return accessResource.getOrDefault(component, SimpleFluidContent.EMPTY).getAmount();
        }
        FluidStack legacyStack = getLegacyFluid(accessResource);
        if (!legacyStack.isEmpty()) {
            return legacyStack.getAmount();
        }
        return accessResource.getOrDefault(component, SimpleFluidContent.EMPTY).getAmount();
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        ItemResource updated = accessResource.with(component,
                SimpleFluidContent.copyOf(newResource.toStack(newAmount)));
        CustomData customData = accessResource.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(LEGACY_FLUID_KEY)) {
            return updated;
        }

        CompoundTag root = customData.copyTag();
        root.remove(LEGACY_FLUID_KEY);
        return updated.with(DataComponents.CUSTOM_DATA, root.isEmpty() ? null : CustomData.of(root));
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return itemAccess.getResource().is(validItem) && (resource.isEmpty() || filter.test(resource.toStack(1)));
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return capacity;
    }

    private boolean isComponentExplicitlyPresent(ItemResource accessResource) {
        return accessResource.getComponentsPatch().getPatch(component) != null;
    }

    private FluidStack getLegacyFluid(ItemResource accessResource) {
        if (!accessResource.is(validItem)) {
            return FluidStack.EMPTY;
        }
        CustomData customData = accessResource.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return FluidStack.EMPTY;
        }

        CompoundTag root = customData.copyTag();
        CompoundTag legacy = root.getCompound(LEGACY_FLUID_KEY).orElse(null);
        if (legacy == null) {
            return FluidStack.EMPTY;
        }

        Identifier fluidId = Identifier.tryParse(legacy.getStringOr(LEGACY_FLUID_NAME_KEY, ""));
        int amount = legacy.getIntOr(LEGACY_AMOUNT_KEY, 0);
        if (fluidId == null || amount <= 0) {
            return FluidStack.EMPTY;
        }

        Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }

        FluidStack stack = new FluidStack(fluid, amount);
        @Nullable CompoundTag legacyFluidData = legacy.getCompound(LEGACY_TAG_KEY).orElse(null);
        if (legacyFluidData != null && !legacyFluidData.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyFluidData));
        }
        return stack;
    }
}
