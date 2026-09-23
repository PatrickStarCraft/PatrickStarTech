package com.gregtechceu.gtceu.api.transfer.fluid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Access to GT and integration fluid fields stored in a fluid stack's custom-data component. */
public final class FluidStackData {
    private FluidStackData() {}

    /** Creates a modern fluid stack from legacy GT/KubeJS fluid custom data. */
    public static FluidStack fromLegacyNbt(Fluid fluid, int amount, @Nullable CompoundTag tag) {
        DataComponentPatch components = tag == null ? DataComponentPatch.EMPTY : DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
                .build();
        return new FluidStack(fluid, amount, components);
    }

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
