package com.gregtechceu.gtceu.common.entity;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTEntityTypes;
import com.gregtechceu.gtceu.common.data.GTItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class GTBoat extends Boat {

    private static final EntityDataAccessor<Integer> DATA_ID_GT_BOAT_TYPE =
            SynchedEntityData.defineId(GTBoat.class, EntityDataSerializers.INT);

    public GTBoat(EntityType<? extends Boat> entityType, Level level) {
        this(entityType, level, new AtomicReference<>());
    }

    private GTBoat(EntityType<? extends Boat> entityType, Level level, AtomicReference<GTBoat> self) {
        super(entityType, level, dropItemSupplier(self));
        self.set(this);
    }

    public GTBoat(Level level, double x, double y, double z) {
        this(GTEntityTypes.BOAT.get(), level);
        this.setInitialPos(x, y, z);
    }

    private static Supplier<Item> dropItemSupplier(AtomicReference<GTBoat> self) {
        return () -> {
            GTBoat boat = self.get();
            if (boat == null) return GTItems.RUBBER_BOAT.get();
            return switch (boat.getBoatType()) {
                case RUBBER -> GTItems.RUBBER_BOAT.get();
                case TREATED_WOOD -> GTItems.TREATED_WOOD_BOAT.get();
            };
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID_GT_BOAT_TYPE, BoatType.RUBBER.ordinal());
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return super.getCustomName();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Type", getBoatType().getName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Keep the legacy string key so existing GregTech boats retain their wood type when loaded.
        setBoatType(BoatType.byName(input.getStringOr("Type", "")));
    }

    public void setBoatType(BoatType type) {
        this.entityData.set(DATA_ID_GT_BOAT_TYPE, type.ordinal());
    }

    public BoatType getBoatType() {
        return BoatType.byId(this.entityData.get(DATA_ID_GT_BOAT_TYPE));
    }

    public enum BoatType {

        RUBBER("rubber", GTBlocks.RUBBER_PLANK.get()),
        TREATED_WOOD("treated", GTBlocks.TREATED_WOOD_PLANK.get());

        private final String name;
        private final Block planks;

        private static final BoatType[] VALUES = values();

        BoatType(String name, Block planks) {
            this.name = name;
            this.planks = planks;
        }

        public String getName() {
            return this.name;
        }

        public Block getPlanks() {
            return this.planks;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static BoatType byId(int id) {
            if (id < 0 || id >= VALUES.length) id = 0;
            return VALUES[id];
        }

        public static BoatType byName(String name) {
            return Arrays.stream(VALUES).filter(type -> type.getName().equals(name)).findFirst().orElse(VALUES[0]);
        }
    }
}
