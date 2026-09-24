package com.gregtechceu.gtceu.common.item.tool.behavior;

import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlungerBehaviorPortTest {

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

    @BeforeEach
    void resetDamageObserver() {
        ToolHelper.resetDamageCalls();
    }

    private static net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> sink(ItemStack stack) {
        return PlungerBehavior.INSTANCE.createFluidHandler(ItemAccess.forStack(stack));
    }

    private static int insert(net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> sink,
                              Transaction transaction, int amount) {
        return sink.insert(0, FluidResource.of(Fluids.WATER), amount, transaction);
    }

    @Test
    void nestedCommittedInsertDoesNotDamageUntilRootCommitAndRootRollbackCancelsIt() {
        var sink = sink(new ItemStack(Items.PAPER));

        try (Transaction root = Transaction.openRoot()) {
            try (Transaction nested = Transaction.open(root)) {
                assertEquals(250, insert(sink, nested, 250));
                nested.commit();
            }
            assertEquals(0, ToolHelper.damageCalls());
        }

        assertEquals(0, ToolHelper.damageCalls());

        try (Transaction root = Transaction.openRoot()) {
            try (Transaction nested = Transaction.open(root)) {
                assertEquals(250, insert(sink, nested, 250));
                nested.commit();
            }
            assertEquals(0, ToolHelper.damageCalls());
            root.commit();
        }

        assertEquals(1, ToolHelper.damageCalls());
    }

    @Test
    void eachAcceptedFluidInsertionRequestsOneDamageOnlyAfterCommit() {
        var sink = sink(new ItemStack(Items.PAPER));

        try (Transaction root = Transaction.openRoot()) {
            assertEquals(100, insert(sink, root, 100));
            assertEquals(150, insert(sink, root, 150));
            assertEquals(0, ToolHelper.damageCalls());
            root.commit();
        }

        assertEquals(2, ToolHelper.damageCalls());
    }

    @Test
    void extractionDoesNotDamageAndInvalidIndicesAreRejected() {
        var sink = sink(new ItemStack(Items.PAPER));
        try (Transaction root = Transaction.openRoot()) {
            assertEquals(0, sink.extract(0, FluidResource.of(Fluids.WATER), 100, root));
            assertThrows(IndexOutOfBoundsException.class, () -> sink.getAmountAsLong(1));
            assertEquals(0, ToolHelper.damageCalls());
            root.commit();
        }
        assertEquals(0, ToolHelper.damageCalls());
    }
}
