package com.gregtechceu.gtceu.api.item.data;

import com.gregtechceu.gtceu.api.capability.BlockEnergyCapabilities;
import com.gregtechceu.gtceu.api.capability.EnergyNetworkTransfer;
import com.gregtechceu.gtceu.api.capability.IBlockEnergyProvider;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockEnergyPortTest {

    @BeforeAll
    static void register() throws Exception {
        var constructor = RegisterCapabilitiesEvent.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var event = constructor.newInstance();
        BlockEnergyCapabilities.register(event, Blocks.CHEST);
        assertTrue(event.isBlockRegistered(BlockEnergyCapabilities.ENERGY_CONTAINER, Blocks.CHEST));
    }

    private static IEnergyContainer query(FixtureEntity entity, Direction side) {
        // Direct real NeoForge dispatch with both state and entity supplied: no world lookup is needed.
        // This does not exercise Level's cache or chunk lifecycle.
        return BlockEnergyCapabilities.ENERGY_CONTAINER.getCapability(null, entity.getBlockPos(),
                entity.getBlockState(), entity, side);
    }

    @Test
    void registeredProviderHonorsSideAndChangedHandler() {
        var entity = new FixtureEntity();
        assertSame(entity.handler, query(entity, Direction.NORTH));
        assertSame(entity.handler, query(entity, null));
        assertNull(query(entity, Direction.SOUTH));
        entity.side = Direction.SOUTH;
        assertNull(query(entity, Direction.NORTH));
        assertSame(entity.handler, query(entity, Direction.SOUTH));
        var replacement = new Receiver(64, Direction.SOUTH);
        entity.handler = replacement;
        assertSame(replacement, query(entity, Direction.SOUTH));
        entity.handler = null;
        assertNull(query(entity, Direction.SOUTH));
    }

    @Test
    void removedAndUnsupportedEntitiesAreNotExposed() {
        var entity = new FixtureEntity();
        entity.setRemoved();
        assertNull(query(entity, null));
        assertNull(BlockEnergyCapabilities.resolve(null, null));
        assertNull(BlockEnergyCapabilities.resolve(
                new BlockEntity(BlockEntityTypes.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState()) {}, null));
    }

    @Test
    void combinedReceiversShareOnePacketBudget() {
        var first = new Receiver(32, Direction.NORTH);
        var second = new Receiver(320, Direction.NORTH);
        assertEquals(2, EnergyNetworkTransfer.distribute(List.of(first, second), Direction.NORTH, 32, 2));
        assertEquals(32, first.stored);
        assertEquals(32, second.stored);
        assertEquals(64, first.stored + second.stored);
    }

    @Test
    void distributionRetainsSideAndRejectsEmptyBudgets() {
        var receiver = new Receiver(320, Direction.NORTH);
        assertEquals(0, EnergyNetworkTransfer.distribute(List.of(receiver), Direction.SOUTH, 32, 2));
        assertEquals(0, EnergyNetworkTransfer.distribute(List.of(receiver), null, 0, 2));
        assertEquals(0, EnergyNetworkTransfer.distribute(List.of(receiver), null, 32, -1));
        assertEquals(0, receiver.stored);
        assertEquals(2, EnergyNetworkTransfer.distribute(List.of(receiver), null, 32, 2));
        assertEquals(64, receiver.stored);
    }

    private static final class FixtureEntity extends BlockEntity implements IBlockEnergyProvider {
        private Direction side = Direction.NORTH;
        private IEnergyContainer handler = new Receiver(320, Direction.NORTH);

        private FixtureEntity() {
            super(BlockEntityTypes.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        }

        @Override
        public IEnergyContainer getEnergyContainer(Direction side) {
            return side == null || side == this.side ? handler : null;
        }
    }

    /** Small GT receiver fixture, not a replacement Minecraft/NeoForge API. */
    private static final class Receiver implements IEnergyContainer {
        private final long capacity;
        private final Direction input;
        private long stored;

        private Receiver(long capacity, Direction input) {
            this.capacity = capacity;
            this.input = input;
        }

        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            if (!inputsEnergy(side)) return 0;
            long accepted = Math.min(amperage, (capacity - stored) / voltage);
            stored += accepted * voltage;
            return accepted;
        }

        @Override
        public boolean inputsEnergy(Direction side) { return side == null || side == input; }
        @Override
        public long changeEnergy(long delta) {
            long previous = stored;
            stored = Math.clamp(stored + delta, 0, capacity);
            return stored - previous;
        }
        @Override
        public long getEnergyStored() { return stored; }
        @Override
        public long getEnergyCapacity() { return capacity; }
        @Override
        public long getInputAmperage() { return 10; }
        @Override
        public long getInputVoltage() { return 32; }
    }
}
