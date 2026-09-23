package com.gregtechceu.gtceu.common.entity;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTEntityTypes;
import com.gregtechceu.gtceu.common.data.GTItems;

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

import java.util.Arrays;
import java.util.function.Supplier;

public class GTBoat extends Boat {

    private static final EntityDataAccessor<Integer> DATA_GT_BOAT_TYPE =
            SynchedEntityData.defineId(GTBoat.class, EntityDataSerializers.INT);

    private final DropItemSupplier dropItemSupplier;

    public GTBoat(EntityType<? extends GTBoat> entityType, Level level) {
        this(entityType, level, new DropItemSupplier(GTItems.RUBBER_BOAT.get()));
    }

    private GTBoat(EntityType<? extends GTBoat> entityType, Level level, DropItemSupplier dropItemSupplier) {
        super(entityType, level, dropItemSupplier);
        this.dropItemSupplier = dropItemSupplier;
    }

    public GTBoat(Level level, double x, double y, double z) {
        this(GTEntityTypes.BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_GT_BOAT_TYPE, BoatType.RUBBER.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Type", getBoatType().getName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setBoatType(BoatType.byName(input.getStringOr("Type", BoatType.RUBBER.getName())));
    }

    public void setBoatType(BoatType type) {
        this.entityData.set(DATA_GT_BOAT_TYPE, type.ordinal());
        updateDropItem();
    }

    public BoatType getBoatType() {
        return BoatType.byId(this.entityData.get(DATA_GT_BOAT_TYPE));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_GT_BOAT_TYPE.equals(accessor)) {
            updateDropItem();
        }
    }

    private void updateDropItem() {
        this.dropItemSupplier.set(switch (getBoatType()) {
            case RUBBER -> GTItems.RUBBER_BOAT.get();
            case TREATED_WOOD -> GTItems.TREATED_WOOD_BOAT.get();
        });
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

    public static final class DropItemSupplier implements Supplier<Item> {

        private Item item;

        DropItemSupplier(Item item) {
            this.item = item;
        }

        void set(Item item) {
            this.item = item;
        }

        @Override
        public Item get() {
            return this.item;
        }
    }
}
