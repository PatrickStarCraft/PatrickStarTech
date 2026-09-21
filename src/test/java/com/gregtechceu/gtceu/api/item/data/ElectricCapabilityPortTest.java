package com.gregtechceu.gtceu.api.item.data;

import com.gregtechceu.gtceu.api.capability.ElectricItemCapabilities;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.capability.ElectricItem;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElectricCapabilityPortTest {

    @BeforeAll
    static void register() throws Exception {
        // Real event/provider dispatch, isolated from GT's unfinished mod bootstrap.
        var constructor = RegisterCapabilitiesEvent.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var event = constructor.newInstance();
        for (var item : new net.minecraft.world.item.Item[] { Items.STICK, Items.BLAZE_ROD, Items.BONE }) {
            if (!item.builtInRegistryHolder().areComponentsBound()) {
                item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                        .set(DataComponents.MAX_STACK_SIZE, 64).build());
            }
        }
        ElectricItemCapabilities.register(event, Items.STICK,
                stack -> new ElectricItem(stack, 1000, 1, true, true));
        ElectricItemCapabilities.register(event, Items.BLAZE_ROD,
                stack -> new ElectricItem(stack, 1000, 1, true, false));
    }

    private static IElectricItem handler(ItemStack stack) {
        return stack.getCapability(ElectricItemCapabilities.ELECTRIC_ITEM);
    }

    @Test
    void lookupBindsActualStackAndDoesNotShareCopies() {
        var stack = new ItemStack(Items.STICK);
        var first = handler(stack);
        assertNotNull(first);
        assertEquals(200, first.charge(200, 1, true, false));
        assertEquals(200, ElectricItemData.getCharge(stack, 1000));
        assertEquals(200, handler(stack).getCharge());
        var copy = stack.copy();
        handler(copy).discharge(50, 1, true, true, false);
        assertEquals(200, first.getCharge());
        assertEquals(150, handler(copy).getCharge());
        ElectricItemData.setCharge(stack, 80);
        assertEquals(80, first.getCharge());
    }

    @Test
    void simulationTierAndTransferLimitsPreserveState() {
        var stack = new ItemStack(Items.STICK);
        var item = handler(stack);
        assertEquals(0, item.charge(100, 0, false, false));
        assertEquals(32, item.charge(100, 1, false, true));
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
        assertEquals(32, item.charge(100, 1, false, false));
        assertEquals(32, item.discharge(100, 1, false, true, true));
        assertEquals(32, item.getCharge());
        assertEquals(0, item.discharge(100, 0, true, true, false));
        assertEquals(32, item.discharge(100, 1, false, true, false));
        assertEquals(0, item.getCharge());
    }

    @Test
    void emptyUnsupportedAndMultipleItemStacks() {
        assertNull(handler(ItemStack.EMPTY));
        assertNull(handler(new ItemStack(Items.BONE)));
        var stack = new ItemStack(Items.STICK, 2);
        var item = handler(stack);
        assertEquals(0, item.charge(100, 1, true, false));
        ElectricItemData.setCharge(stack, 100);
        assertEquals(0, item.discharge(100, 1, true, true, false));
        assertEquals(100, item.getCharge());
    }

    @Test
    void capacityExternalDischargeAndEnergyConservation() {
        var source = handler(new ItemStack(Items.STICK));
        var target = handler(new ItemStack(Items.BLAZE_ROD));
        source.charge(200, 1, true, false);
        target.charge(990, 1, true, false);
        long available = source.discharge(32, 1, false, true, true);
        long accepted = target.charge(available, 1, false, true);
        assertEquals(10, accepted);
        long removed = source.discharge(accepted, 1, false, true, false);
        assertEquals(removed, target.charge(removed, 1, false, false));
        assertEquals(1190, source.getCharge() + target.getCharge());
        assertEquals(0, target.discharge(10, 1, true, true, false));
        assertEquals(10, target.discharge(10, 1, true, false, false));
    }
}
