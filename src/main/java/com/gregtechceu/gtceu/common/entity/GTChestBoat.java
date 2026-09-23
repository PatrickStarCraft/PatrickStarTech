package com.gregtechceu.gtceu.common.entity;

import com.gregtechceu.gtceu.common.data.GTEntityTypes;
import com.gregtechceu.gtceu.common.data.GTItems;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class GTChestBoat extends ChestBoat {

    private static final EntityDataAccessor<Integer> DATA_ID_GT_BOAT_TYPE =
            SynchedEntityData.defineId(GTChestBoat.class, EntityDataSerializers.INT);

    public GTChestBoat(EntityType<? extends ChestBoat> entityType, Level level) {
        this(entityType, level, new AtomicReference<>());
    }

    private GTChestBoat(EntityType<? extends ChestBoat> entityType, Level level, AtomicReference<GTChestBoat> self) {
        super(entityType, level, dropItemSupplier(self));
        self.set(this);
    }

    public GTChestBoat(Level level, double x, double y, double z) {
        this(GTEntityTypes.CHEST_BOAT.get(), level);
        this.setInitialPos(x, y, z);
    }

    private static Supplier<Item> dropItemSupplier(AtomicReference<GTChestBoat> self) {
        return () -> {
            GTChestBoat boat = self.get();
            if (boat == null) return GTItems.RUBBER_CHEST_BOAT.get();
            return switch (boat.getBoatType()) {
                case RUBBER -> GTItems.RUBBER_CHEST_BOAT.get();
                case TREATED_WOOD -> GTItems.TREATED_WOOD_CHEST_BOAT.get();
            };
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID_GT_BOAT_TYPE, GTBoat.BoatType.RUBBER.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Type", getBoatType().getName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Keep the legacy string key so existing GregTech chest boats retain their wood type.
        setBoatType(GTBoat.BoatType.byName(input.getStringOr("Type", "")));
    }

    public void setBoatType(GTBoat.BoatType type) {
        this.entityData.set(DATA_ID_GT_BOAT_TYPE, type.ordinal());
    }

    public GTBoat.BoatType getBoatType() {
        return GTBoat.BoatType.byId(this.entityData.get(DATA_ID_GT_BOAT_TYPE));
    }
}
