package com.gregtechceu.gtceu.common.entity;

import com.gregtechceu.gtceu.common.data.GTEntityTypes;
import com.gregtechceu.gtceu.common.data.GTItems;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GTChestBoat extends ChestBoat {

    private static final EntityDataAccessor<Integer> DATA_GT_BOAT_TYPE =
            SynchedEntityData.defineId(GTChestBoat.class, EntityDataSerializers.INT);

    private final GTBoat.DropItemSupplier dropItemSupplier;

    public GTChestBoat(EntityType<? extends GTChestBoat> entityType, Level level) {
        this(entityType, level, new GTBoat.DropItemSupplier(GTItems.RUBBER_CHEST_BOAT.get()));
    }

    private GTChestBoat(EntityType<? extends GTChestBoat> entityType, Level level,
                        GTBoat.DropItemSupplier dropItemSupplier) {
        super(entityType, level, dropItemSupplier);
        this.dropItemSupplier = dropItemSupplier;
    }

    public GTChestBoat(Level level, double x, double y, double z) {
        this(GTEntityTypes.CHEST_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_GT_BOAT_TYPE, GTBoat.BoatType.RUBBER.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Type", getBoatType().getName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setBoatType(GTBoat.BoatType.byName(input.getStringOr("Type", GTBoat.BoatType.RUBBER.getName())));
    }

    public void setBoatType(GTBoat.BoatType type) {
        this.entityData.set(DATA_GT_BOAT_TYPE, type.ordinal());
        updateDropItem();
    }

    public GTBoat.BoatType getBoatType() {
        return GTBoat.BoatType.byId(this.entityData.get(DATA_GT_BOAT_TYPE));
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
            case RUBBER -> GTItems.RUBBER_CHEST_BOAT.get();
            case TREATED_WOOD -> GTItems.TREATED_WOOD_CHEST_BOAT.get();
        });
    }
}
