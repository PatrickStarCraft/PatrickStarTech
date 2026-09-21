package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryPersistencePortTest {
    @BeforeAll
    static void bindComponents() {
        if (!Fluids.WATER.builtInRegistryHolder().areComponentsBound()) {
            Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
        if (!Items.STONE.builtInRegistryHolder().areComponentsBound()) {
            Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
    }

    @Test void itemComponentsAndCountsRoundTripWithoutChangingInputTag() {
        var source = new CustomItemStackHandler(2);
        var stack = new ItemStack(Items.STONE, 37);
        var data = new CompoundTag();
        data.putLong("Charge", 1000L);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        source.setStackInSlot(1, stack);
        var saved = source.serializeNBT();
        var before = saved.copy();
        var target = new CustomItemStackHandler(3);
        target.deserializeNBT(saved);
        assertEquals(before, saved);
        assertEquals(3, target.getSlots());
        assertTrue(target.getStackInSlot(0).isEmpty());
        assertTrue(ItemStack.matches(stack, target.getStackInSlot(1)));
        assertTrue(target.getStackInSlot(2).isEmpty());
    }

    @Test void loadingEmptyInventoryClearsExistingContents() {
        var target = new CustomItemStackHandler(2);
        target.setStackInSlot(0, new ItemStack(Items.STONE));
        target.deserializeNBT(new CustomItemStackHandler(2).serializeNBT());
        assertTrue(target.getStackInSlot(0).isEmpty());
    }

    @Test void fluidComponentsAndAmountRoundTrip() {
        var source = new CustomFluidTank(8000);
        var fluid = new FluidStack(Fluids.WATER, 3456);
        var data = new CompoundTag();
        data.putLong("batch", 1000L);
        fluid.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        source.setFluid(fluid);
        var target = new CustomFluidTank(8000);
        target.deserializeNBT(source.serializeNBT());
        assertTrue(FluidStack.matches(fluid, target.getFluid()));
    }

    @Test void emptyFluidSaveClearsExistingContentsAndNotifiesOnce() {
        var target = new CustomFluidTank(new FluidStack(Fluids.WATER, 1000));
        int[] notifications = {0};
        target.setOnContentsChanged(() -> notifications[0]++);
        target.deserializeNBT(new CustomFluidTank(1000).serializeNBT());
        assertTrue(target.isEmpty());
        assertEquals(1, notifications[0]);
    }
}
