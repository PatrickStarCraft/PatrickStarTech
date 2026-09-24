package com.gregtechceu.gtceu.api.misc;

import com.gregtechceu.gtceu.common.data.GTDataComponents;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilteredFluidResourceHandlerPortTest {

    private static final int CAPACITY = 1000;

    @BeforeAll
    static void bindVanillaComponents() {
        if (!Items.PAPER.builtInRegistryHolder().areComponentsBound()) {
            Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
        for (Fluid fluid : new Fluid[] { Fluids.WATER, Fluids.LAVA }) {
            if (!fluid.builtInRegistryHolder().areComponentsBound()) {
                fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
            }
        }
    }

    private static FilteredFluidResourceHandler waterTank(ItemStack stack) {
        return new FilteredFluidResourceHandler(ItemAccess.forStack(stack), CAPACITY,
                fluid -> fluid.getFluid() == Fluids.WATER);
    }

    private static int insert(FilteredFluidResourceHandler handler, FluidResource fluid, int amount,
                              boolean commit) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(0, fluid, amount, transaction);
            if (commit) transaction.commit();
            return inserted;
        }
    }

    private static CompoundTag legacyFluid(String id, int amount) {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("FluidName", id);
        legacy.putInt("Amount", amount);
        CompoundTag tag = new CompoundTag();
        tag.put("Fluid", legacy);
        tag.putString("OtherMod", "keep");
        return tag;
    }

    private static FluidResource taggedWater(String dataKey, String dataValue) {
        CompoundTag fluidData = new CompoundTag();
        fluidData.putString(dataKey, dataValue);
        FluidStack stack = new FluidStack(Fluids.WATER, 1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(fluidData));
        return FluidResource.of(stack);
    }

    @Test
    void filterRejectsLavaAndSimulationDoesNotWriteTheStack() {
        ItemStack stack = new ItemStack(Items.PAPER);
        FilteredFluidResourceHandler handler = waterTank(stack);

        assertEquals(0, insert(handler, FluidResource.of(Fluids.LAVA), 500, true));
        assertEquals(400, insert(handler, FluidResource.of(Fluids.WATER), 400, false));

        assertEquals(0, handler.getAmountAsLong(0));
        assertTrue(handler.getResource(0).isEmpty());
        assertFalse(stack.has(GTDataComponents.FLUID_CONTENT.get()));
    }

    @Test
    void committedInsertStoresFluidComponentAndAbortRollsBackIt() {
        ItemStack stack = new ItemStack(Items.PAPER);
        FilteredFluidResourceHandler handler = waterTank(stack);
        FluidResource water = FluidResource.of(Fluids.WATER);

        try (Transaction root = Transaction.openRoot()) {
            try (Transaction nested = Transaction.open(root)) {
                assertEquals(600, handler.insert(0, water, 600, nested));
                nested.commit();
            }
            assertEquals(600, handler.getAmountAsLong(0));
        }

        assertEquals(0, handler.getAmountAsLong(0));
        assertFalse(stack.has(GTDataComponents.FLUID_CONTENT.get()));

        assertEquals(600, insert(handler, water, 600, true));
        assertEquals(600, handler.getAmountAsLong(0));
        SimpleFluidContent content = stack.get(GTDataComponents.FLUID_CONTENT.get());
        assertNotNull(content);
        assertTrue(content.matches(new FluidStack(Fluids.WATER, 600)));
    }

    @Test
    void firstCommittedMutationMigratesLegacyFluidAndPreservesUnrelatedData() {
        ItemStack stack = new ItemStack(Items.PAPER);
        CompoundTag initial = legacyFluid("minecraft:water", 300);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(initial));
        FilteredFluidResourceHandler handler = waterTank(stack);

        assertEquals(300, handler.getAmountAsLong(0));
        assertEquals(200, insert(handler, FluidResource.of(Fluids.WATER), 200, false));
        assertEquals(initial, stack.get(DataComponents.CUSTOM_DATA).copyTag());
        assertFalse(stack.has(GTDataComponents.FLUID_CONTENT.get()));

        assertEquals(200, insert(handler, FluidResource.of(Fluids.WATER), 200, true));
        assertEquals(500, handler.getAmountAsLong(0));
        assertTrue(stack.has(GTDataComponents.FLUID_CONTENT.get()));
        CompoundTag remaining = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertFalse(remaining.contains("Fluid"));
        assertEquals("keep", remaining.getStringOr("OtherMod", ""));
    }

    @Test
    void explicitlyStoredEmptyComponentTakesPrecedenceOverStaleLegacyFluid() {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(GTDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyFluid("minecraft:water", 300)));

        FilteredFluidResourceHandler handler = waterTank(stack);

        assertEquals(0, handler.getAmountAsLong(0));
        assertTrue(handler.getResource(0).isEmpty());
        assertTrue(stack.get(DataComponents.CUSTOM_DATA).copyTag().contains("Fluid"));
    }

    @Test
    void stackedContainersStoreTheSamePerItemContentsWithoutLosingCount() {
        ItemStack stack = new ItemStack(Items.PAPER, 2);
        FilteredFluidResourceHandler handler = waterTank(stack);

        assertEquals(1200, insert(handler, FluidResource.of(Fluids.WATER), 1200, true));
        assertEquals(2, stack.getCount());
        assertEquals(1200, handler.getAmountAsLong(0));
        assertEquals(400, handler.extract(0, FluidResource.of(Fluids.WATER), 400, null));
        assertEquals(2, stack.getCount());
        assertEquals(800, handler.getAmountAsLong(0));
        assertEquals(400, stack.get(GTDataComponents.FLUID_CONTENT.get()).getAmount());
    }

    @Test
    void legacyFluidComponentsSurviveLazyMigration() {
        ItemStack stack = new ItemStack(Items.PAPER);
        CompoundTag legacyRoot = legacyFluid("minecraft:water", 300);
        CompoundTag legacyFluid = legacyRoot.getCompound("Fluid").orElseThrow();
        CompoundTag oldFluidData = new CompoundTag();
        oldFluidData.putString("Grade", "filtered");
        legacyFluid.put("Tag", oldFluidData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyRoot));
        FilteredFluidResourceHandler handler = waterTank(stack);

        assertEquals(100, insert(handler, taggedWater("Grade", "filtered"), 100, true));

        assertEquals(400, handler.getAmountAsLong(0));
        assertEquals("filtered", handler.getResource(0).toStack(1)
                .get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("Grade", ""));
        CompoundTag remaining = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertFalse(remaining.contains("Fluid"));
        assertEquals("keep", remaining.getStringOr("OtherMod", ""));
    }
}
