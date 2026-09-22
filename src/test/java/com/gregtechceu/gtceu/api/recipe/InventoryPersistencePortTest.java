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
    @Test void ownedStackPayloadsPreserveComponentsOversizedCountsAndEmptySlots() {
        var stack = new ItemStack(Items.DIAMOND, 4096);
        var custom = new CompoundTag();
        custom.putLong("charge", 1234567890123L);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        var saved = com.gregtechceu.gtceu.utils.data.StackPersistence.saveItem(stack);
        var original = saved.copy();
        var loaded = com.gregtechceu.gtceu.utils.data.StackPersistence.loadItem(saved);
        assertEquals(4096, loaded.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(stack, loaded));
        assertEquals(original, saved);
        assertTrue(com.gregtechceu.gtceu.utils.data.StackPersistence.loadItem(
                com.gregtechceu.gtceu.utils.data.StackPersistence.saveItem(ItemStack.EMPTY)).isEmpty());

        var fluid = new FluidStack(Fluids.WATER, 123456);
        fluid.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        var loadedFluid = com.gregtechceu.gtceu.utils.data.StackPersistence.loadFluid(
                com.gregtechceu.gtceu.utils.data.StackPersistence.saveFluid(fluid));
        assertTrue(FluidStack.matches(fluid, loadedFluid));
        assertTrue(com.gregtechceu.gtceu.utils.data.StackPersistence.loadFluid(
                com.gregtechceu.gtceu.utils.data.StackPersistence.saveFluid(FluidStack.EMPTY)).isEmpty());
    }

    @Test void malformedOwnedStackPayloadFailsRatherThanLosingAnInventoryEntry() {
        var malformed = new CompoundTag();
        malformed.putString("id", "minecraft:diamond");
        malformed.putInt("count", -2);
        assertThrows(IllegalArgumentException.class,
                () -> com.gregtechceu.gtceu.utils.data.StackPersistence.loadItem(malformed));
    }

    @Test void textJsonRetainsTranslationArgumentsStylesAndSiblings() {
        var component = net.minecraft.network.chat.Component.translatable("test.translation", 42,
                net.minecraft.network.chat.Component.literal("argument").withStyle(net.minecraft.ChatFormatting.GOLD))
                .withStyle(net.minecraft.ChatFormatting.BOLD).append(" suffix");
        var json = com.gregtechceu.gtceu.utils.data.ComponentJson.toJson(component);
        var restored = com.gregtechceu.gtceu.utils.data.ComponentJson.fromJson(json);
        // JSON normalizes numeric argument wrapper types; compare the persisted representation and styling.
        assertEquals(json, com.gregtechceu.gtceu.utils.data.ComponentJson.toJson(restored));
        assertEquals(component.getStyle(), restored.getStyle());
        assertEquals(component.getSiblings(), restored.getSiblings());
        var contents = (net.minecraft.network.chat.contents.TranslatableContents) restored.getContents();
        assertEquals("test.translation", contents.getKey());
        assertEquals("42", contents.getArgs()[0].toString());
        assertEquals(net.minecraft.network.chat.Component.literal("argument").withStyle(net.minecraft.ChatFormatting.GOLD),
                contents.getArgs()[1]);
    }

    @Test void mixedFluidHandlerImplementationsKeepTheirSavedIndices() {
        var custom = new CustomFluidTank(2000);
        var nativeTank = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(2000);
        nativeTank.setFluid(new FluidStack(Fluids.WATER, 777));
        var source = new com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList(custom, nativeTank);
        var first = new CustomFluidTank(2000);
        var second = new CustomFluidTank(2000);
        var target = new com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList(first, second);
        target.deserializeNBT(source.serializeNBT());
        assertTrue(first.isEmpty());
        assertEquals(777, second.getFluidAmount());
        assertThrows(IllegalArgumentException.class,
                () -> new com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList(first)
                        .deserializeNBT(source.serializeNBT()));
    }

    @Test void typedListsRejectHeterogeneousElementsWithoutMutatingInput() {
        var root = new CompoundTag();
        var list = new net.minecraft.nbt.ListTag();
        list.add(net.minecraft.nbt.StringTag.valueOf("a"));
        root.put("entries", list);
        assertEquals(1, com.gregtechceu.gtceu.utils.data.TypedTagList
                .read(root, "entries", net.minecraft.nbt.Tag.TAG_STRING).size());
        list.add(net.minecraft.nbt.IntTag.valueOf(5));
        assertTrue(com.gregtechceu.gtceu.utils.data.TypedTagList
                .read(root, "entries", net.minecraft.nbt.Tag.TAG_STRING).isEmpty());
        assertEquals(2, list.size());
    }

    private static class SampleSavedData extends com.gregtechceu.gtceu.api.sync_system.CompoundTagSavedData {
        private final CompoundTag data;
        SampleSavedData() { this(new CompoundTag()); }
        SampleSavedData(CompoundTag data) { this.data = data.copy(); }
        @Override public CompoundTag save(CompoundTag tag) { return data.copy(); }
    }

    @Test void savedDataCodecPreservesOwnedSchemaAndNumericTypes() {
        var type = com.gregtechceu.gtceu.api.sync_system.CompoundTagSavedData
                .type(SampleSavedData::new, SampleSavedData::new, "test_save");
        var tag = new CompoundTag();
        tag.putLong("energy", 1234567890123L);
        tag.putByte("flags", (byte) 7);
        var codec = type.codecFactory().create(null);
        var loaded = codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
        var saved = codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, loaded).getOrThrow();
        assertEquals(tag, saved);
        assertEquals("gtceu:test_save", type.id().toString());
    }

    @BeforeAll
    static void bindComponents() {
        if (!Items.DIAMOND.builtInRegistryHolder().areComponentsBound()) {
            Items.DIAMOND.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
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
