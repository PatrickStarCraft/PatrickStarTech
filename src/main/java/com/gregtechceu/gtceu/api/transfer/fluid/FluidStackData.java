package com.gregtechceu.gtceu.api.transfer.fluid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Consumer;

/** Access to GT and integration fluid fields stored in a fluid stack's custom-data component. */
public final class FluidStackData {
    private FluidStackData() {}

    public static CompoundTag read(FluidStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static CompoundTag readNullable(FluidStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static void update(FluidStack stack, Consumer<CompoundTag> mutation) {
        CompoundTag tag = read(stack);
        mutation.accept(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
