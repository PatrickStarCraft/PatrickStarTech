package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.item.component.SpoilContext;
import com.gregtechceu.gtceu.api.item.data.ItemStackData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpoilableItemStackPortTest {

    private static final String PERSISTED_STATE_KEY = "gtceu_spoilable";

    @BeforeAll
    static void bindTestItemComponents() {
        // The selected-source host does not load server data packs or bind built-in item defaults.
        if (!Items.PAPER.builtInRegistryHolder().areComponentsBound()) {
            Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
    }

    private static final class TestSpoilableStack extends SpoilableItemStack {

        private TestSpoilableStack(ItemStack stack) {
            super(stack);
        }

        @Override
        public long getSpoilTicks() {
            return 2400;
        }

        @Override
        public ItemStack spoilResult(SpoilContext context, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack freshStack() {
        return new ItemStack(Items.PAPER);
    }

    @Test
    void creationTickCommitsInsideNestedStateAndKeepsUnrelatedCustomData() {
        ItemStack stack = freshStack();
        ItemStackData.update(stack, root -> root.putString("owner_note", "preserve"));

        new TestSpoilableStack(stack).setCreationTick(123456789L);

        CompoundTag root = ItemStackData.read(stack);
        assertEquals("preserve", root.getStringOr("owner_note", ""));
        assertEquals(123456789L, root.getCompoundOrEmpty(PERSISTED_STATE_KEY)
                .getLongOr(SpoilableItemStack.CREATION_TICK_KEY, -1));
    }

    @Test
    void copiedStackReloadsCreationTickAndSpoilContext() {
        ItemStack stack = freshStack();
        TestSpoilableStack spoilable = new TestSpoilableStack(stack);
        spoilable.setCreationTick(87);
        spoilable.setSpoilContext(new SpoilContext().withSlot(5)
                .withItemHandlerData("marker", net.minecraft.nbt.StringTag.valueOf("machine")));

        TestSpoilableStack loadedCopy = new TestSpoilableStack(stack.copy());

        assertEquals(87, loadedCopy.getCreationTick());
        assertEquals(5, loadedCopy.getSpoilContext().slot());
        assertEquals("machine", loadedCopy.getSpoilContext().itemHandlerData()
                .getStringOr("marker", ""));
    }

    @Test
    void freezeAndUnfreezeArePersistedAcrossStackReload() {
        ItemStack stack = freshStack();
        TestSpoilableStack spoilable = new TestSpoilableStack(stack);
        spoilable.setCreationTick(42);
        spoilable.freezeSpoiling();

        ItemStack frozenStackCopy = stack.copy();
        TestSpoilableStack frozenCopy = new TestSpoilableStack(frozenStackCopy);
        assertTrue(frozenCopy.isFrozen());
        assertEquals(2400L, frozenCopy.getTicksUntilSpoiled());

        frozenCopy.unfreezeSpoiling();
        TestSpoilableStack thawedCopy = new TestSpoilableStack(frozenStackCopy.copy());

        assertFalse(thawedCopy.isFrozen());
        assertEquals(42, thawedCopy.getCreationTick());
        assertFalse(ItemStackData.read(frozenStackCopy).getCompoundOrEmpty(PERSISTED_STATE_KEY)
                .contains(SpoilableItemStack.FROZEN_TICKS_KEY));
    }

    @Test
    void malformedStateDoesNotEraseUnrelatedCustomDataOnNextWrite() {
        ItemStack stack = freshStack();
        ItemStackData.update(stack, root -> {
            root.putString("owner_note", "preserve");
            root.putString(PERSISTED_STATE_KEY, "malformed");
        });

        TestSpoilableStack spoilable = new TestSpoilableStack(stack);
        spoilable.setCreationTick(9);

        CompoundTag root = ItemStackData.read(stack);
        assertEquals("preserve", root.getStringOr("owner_note", ""));
        assertEquals(9, root.getCompoundOrEmpty(PERSISTED_STATE_KEY)
                .getLongOr(SpoilableItemStack.CREATION_TICK_KEY, -1));
    }
}
