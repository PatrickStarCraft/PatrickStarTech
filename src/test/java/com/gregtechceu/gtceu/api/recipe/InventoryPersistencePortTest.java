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

    @Test void unknownOwnedStackIdentifierFailsWithoutChangingTheSavedPayload() {
        var malformed = new CompoundTag();
        malformed.putString("id", "gtceu:missing_item_for_persistence_test");
        malformed.putInt("count", 1);
        var original = malformed.copy();

        assertThrows(IllegalStateException.class,
                () -> com.gregtechceu.gtceu.utils.data.StackPersistence.loadItem(malformed));
        assertEquals(original, malformed);
    }

    @Test void malformedFluidHasStrictAndOptionalDecodePolicies() {
        var malformed = new CompoundTag();
        malformed.putString("id", "not a fluid identifier");
        malformed.putInt("amount", 100);
        var original = malformed.copy();

        assertTrue(com.gregtechceu.gtceu.utils.data.StackPersistence.tryLoadFluid(malformed).isEmpty());
        assertThrows(IllegalStateException.class,
                () -> com.gregtechceu.gtceu.utils.data.StackPersistence.loadFluid(malformed));
        assertEquals(original, malformed);
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

    @Test void fluidHandlerListIntegerDrainHonorsItsFluidFilter() {
        var rejectedTank = new CustomFluidTank(1000);
        rejectedTank.setFluid(waterWithGrade("rejected", 300));
        var firstAcceptedTank = new CustomFluidTank(1000);
        firstAcceptedTank.setFluid(waterWithGrade("accepted", 200));
        var secondAcceptedTank = new CustomFluidTank(1000);
        secondAcceptedTank.setFluid(waterWithGrade("accepted", 400));
        var handlers = new com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList(
                rejectedTank, firstAcceptedTank, secondAcceptedTank);
        handlers.setFilter(fluid -> fluid.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getStringOr("grade", "").equals("accepted"));

        var simulated = handlers.drain(500,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        assertEquals(500, simulated.getAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(waterWithGrade("accepted", 500), simulated));
        assertEquals(300, rejectedTank.getFluidAmount());
        assertEquals(200, firstAcceptedTank.getFluidAmount());
        assertEquals(400, secondAcceptedTank.getFluidAmount());

        var drained = handlers.drain(500,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        assertEquals(500, drained.getAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(waterWithGrade("accepted", 500), drained));
        assertEquals(300, rejectedTank.getFluidAmount());
        assertTrue(firstAcceptedTank.isEmpty());
        assertEquals(100, secondAcceptedTank.getFluidAmount());
    }

    @Test void fluidHandlerListFillFiltersSimulatesAndAggregatesTankCapacity() {
        var first = new CustomFluidTank(200);
        var second = new CustomFluidTank(400);
        var handlers = new com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList(first, second);
        handlers.setFilter(fluid -> fluid.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getStringOr("grade", "").equals("accepted"));

        assertEquals(0, handlers.fill(waterWithGrade("rejected", 100),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        var source = waterWithGrade("accepted", 800);
        assertEquals(600, handlers.fill(source,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE));
        assertEquals(800, source.getAmount());
        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());

        assertEquals(600, handlers.fill(source,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        assertEquals(800, source.getAmount());
        assertEquals(200, first.getFluidAmount());
        assertEquals(400, second.getFluidAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(source, first.getFluid()));
        assertTrue(FluidStack.isSameFluidSameComponents(source, second.getFluid()));
    }

    @Test void ioFluidHandlerListSetsTheRequestedCombinedTankIndex() {
        var first = new CustomFluidTank(1000);
        var second = new CustomFluidTank(2000);
        var handlers = new com.gregtechceu.gtceu.api.misc.IOFluidHandlerList(java.util.List.of(first, second),
                com.gregtechceu.gtceu.api.capability.recipe.IO.BOTH, fluid -> true, fluid -> true);

        handlers.setFluidInTank(1, new FluidStack(Fluids.WATER, 123));

        assertTrue(first.isEmpty());
        assertEquals(123, second.getFluidAmount());
    }

    @Test void ioFluidHandlerListEnforcesInputDirectionAndFilter() {
        var tank = new CustomFluidTank(1000);
        var handlers = new com.gregtechceu.gtceu.api.misc.IOFluidHandlerList(java.util.List.of(tank),
                com.gregtechceu.gtceu.api.capability.recipe.IO.IN,
                fluid -> fluid.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                        .getStringOr("grade", "").equals("accepted"),
                fluid -> true);

        assertEquals(0, handlers.fill(waterWithGrade("rejected", 300),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        assertEquals(200, handlers.fill(waterWithGrade("accepted", 200),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        assertEquals(200, tank.getFluidAmount());
        assertTrue(handlers.drain(100, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)
                .isEmpty());
        assertEquals(200, tank.getFluidAmount());
    }

    @Test void ioFluidHandlerListEnforcesOutputFilterForBothDrainForms() {
        var tank = new CustomFluidTank(1000);
        tank.setFluid(waterWithGrade("rejected", 300));
        var handlers = new com.gregtechceu.gtceu.api.misc.IOFluidHandlerList(java.util.List.of(tank),
                com.gregtechceu.gtceu.api.capability.recipe.IO.OUT, fluid -> true,
                fluid -> fluid.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                        .getStringOr("grade", "").equals("accepted"));

        assertTrue(handlers.drain(100, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)
                .isEmpty());
        assertTrue(handlers.drain(waterWithGrade("rejected", 100),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).isEmpty());
        assertEquals(300, tank.getFluidAmount());

        tank.setFluid(waterWithGrade("accepted", 300));
        var simulated = handlers.drain(200,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        assertEquals(200, simulated.getAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(waterWithGrade("accepted", 200), simulated));
        assertEquals(300, tank.getFluidAmount());
        var drained = handlers.drain(waterWithGrade("accepted", 150),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        assertEquals(150, drained.getAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(waterWithGrade("accepted", 150), drained));
        assertEquals(150, tank.getFluidAmount());
    }

    private static FluidStack waterWithGrade(String grade, int amount) {
        var data = new CompoundTag();
        data.putString("grade", grade);
        var stack = new FluidStack(Fluids.WATER, amount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
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

    @Test void clearingItemStoragePreservesItsSlotShapeAndNotifiesOnce() {
        var target = new CustomItemStackHandler(2);
        target.setStackInSlot(0, new ItemStack(Items.STONE, 3));
        target.setStackInSlot(1, new ItemStack(Items.DIAMOND, 2));
        int[] notifications = { 0 };
        target.setOnContentsChanged(() -> notifications[0]++);

        target.clear();

        assertEquals(2, target.getSlots());
        assertTrue(target.getStackInSlot(0).isEmpty());
        assertTrue(target.getStackInSlot(1).isEmpty());
        assertEquals(1, notifications[0]);
        target.setStackInSlot(1, new ItemStack(Items.STONE));
        assertEquals(2, target.getSlots());
    }

    @Test void itemHandlerFilterAndSimulationKeepComponentStacksUnchanged() {
        var target = new CustomItemStackHandler(1);
        target.setFilter(stack -> stack.is(Items.STONE));
        int[] notifications = { 0 };
        target.setOnContentsChanged(() -> notifications[0]++);

        var rejected = new ItemStack(Items.DIAMOND, 3);
        assertTrue(ItemStack.matches(rejected, target.insertItem(0, rejected, false)));
        assertTrue(target.getStackInSlot(0).isEmpty());

        var data = new CompoundTag();
        data.putLong("batch", 9876543210L);
        var source = new ItemStack(Items.STONE, 5);
        source.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        assertTrue(target.insertItem(0, source, true).isEmpty());
        assertTrue(target.getStackInSlot(0).isEmpty());
        assertEquals(0, notifications[0]);

        assertTrue(target.insertItem(0, source, false).isEmpty());
        assertEquals(5, source.getCount());
        assertTrue(ItemStack.matches(source, target.getStackInSlot(0)));
        assertEquals(1, notifications[0]);

        var preview = target.extractItem(0, 2, true);
        assertEquals(2, preview.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(source, preview));
        assertEquals(5, target.getStackInSlot(0).getCount());
        assertEquals(1, notifications[0]);

        var extracted = target.extractItem(0, 2, false);
        assertEquals(2, extracted.getCount());
        assertEquals(3, target.getStackInSlot(0).getCount());
        var changedData = new CompoundTag();
        changedData.putLong("batch", 1L);
        extracted.set(DataComponents.CUSTOM_DATA, CustomData.of(changedData));
        assertEquals(data, target.getStackInSlot(0).get(DataComponents.CUSTOM_DATA).copyTag());
        assertEquals(2, notifications[0]);
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
