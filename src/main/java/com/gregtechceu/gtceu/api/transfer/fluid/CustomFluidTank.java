package com.gregtechceu.gtceu.api.transfer.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import com.gregtechceu.gtceu.api.sync_system.ValueIOPersistence;
import com.gregtechceu.gtceu.api.sync_system.NBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class CustomFluidTank extends FluidTank implements IFluidHandlerModifiable, NBTSerializable<CompoundTag> {

    @Getter
    @Setter
    protected @NotNull Runnable onContentsChanged = () -> {};

    public CustomFluidTank(int capacity) {
        super(capacity, e -> true);
    }

    public CustomFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
    }

    public CustomFluidTank(FluidStack stack) {
        super(stack.getAmount());
        setFluid(stack);
    }

    @Override
    protected void onContentsChanged() {
        onContentsChanged.run();
    }

    @Override
    public void setFluidInTank(int tank, FluidStack stack) {
        setFluid(stack);
    }

    @Override
    public void setFluid(FluidStack stack) {
        super.setFluid(stack);
        this.onContentsChanged();
    }

    @Override
    public CompoundTag serializeNBT() {
        return serializeNBT(ValueIOPersistence.builtInRegistries());
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        return ValueIOPersistence.write(this, registries);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        deserializeNBT(nbt, ValueIOPersistence.builtInRegistries());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        var loaded = new FluidTank(getCapacity());
        ValueIOPersistence.read(loaded, nbt, registries);
        setFluid(loaded.getFluid());
    }
}
