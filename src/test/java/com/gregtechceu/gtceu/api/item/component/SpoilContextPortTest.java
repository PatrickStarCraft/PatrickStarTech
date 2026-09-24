package com.gregtechceu.gtceu.api.item.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpoilContextPortTest {

    private static CompoundTag handlerData() {
        CompoundTag nested = new CompoundTag();
        nested.putString("mode", "locked");
        CompoundTag data = new CompoundTag();
        data.putInt("slot", 4);
        data.put("nested", nested);
        return data;
    }

    @Test
    void witherDetachesMutableHandlerData() {
        CompoundTag source = handlerData();

        SpoilContext context = new SpoilContext().withItemHandlerData(source);
        source.putInt("slot", 9);
        source.getCompoundOrEmpty("nested").putString("mode", "changed");

        assertEquals(4, context.itemHandlerData().getIntOr("slot", -1));
        assertEquals("locked", context.itemHandlerData().getCompoundOrEmpty("nested")
                .getStringOr("mode", ""));
    }

    @Test
    void accessorReturnsDetachedMutableHandlerData() {
        SpoilContext context = new SpoilContext().withItemHandlerData(handlerData());

        context.itemHandlerData().putInt("slot", 18);

        assertEquals(4, context.itemHandlerData().getIntOr("slot", -1));
    }

    @Test
    void serializationReturnsDetachedNestedData() {
        SpoilContext context = new SpoilContext().withItemHandlerData(handlerData());

        CompoundTag serialized = context.serializeNBT();
        serialized.getCompoundOrEmpty("handlerData").putInt("slot", 12);

        assertEquals(4, context.itemHandlerData().getIntOr("slot", -1));
    }

    @Test
    void deserializationDetachesItsSourceTag() {
        CompoundTag serialized = new SpoilContext().withItemHandlerData(handlerData()).serializeNBT();

        SpoilContext decoded = SpoilContext.deserializeNBT(serialized);
        serialized.getCompoundOrEmpty("handlerData").putInt("slot", 15);

        assertEquals(4, decoded.itemHandlerData().getIntOr("slot", -1));
    }

    @Test
    void handlerSourcePositionSlotAndSideRoundTrip() {
        SpoilContext source = new SpoilContext(null, new BlockPos(2, 3, 4))
                .withSlot(7)
                .withItemHandlerSide(Direction.NORTH);

        SpoilContext decoded = SpoilContext.deserializeNBT(source.serializeNBT());

        assertEquals(new BlockPos(2, 3, 4), decoded.pos());
        assertEquals(7, decoded.slot());
        assertSame(SpoilContext.ItemHandlerSource.BLOCK_CAPABILITY, decoded.itemHandlerSource());
        assertEquals("north", decoded.itemHandlerData().getStringOr("side", ""));
    }
}
