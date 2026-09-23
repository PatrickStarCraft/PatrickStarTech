package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.common.data.GTEntityTypes;
import com.gregtechceu.gtceu.common.entity.GTBoat;
import com.gregtechceu.gtceu.common.entity.GTChestBoat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class GTBoatItemDispenseBehaviour extends DefaultDispenseItemBehavior {

    private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
    private final GTBoat.BoatType type;
    private final boolean isChestBoat;

    public GTBoatItemDispenseBehaviour(GTBoat.BoatType type) {
        this(type, false);
    }

    public GTBoatItemDispenseBehaviour(GTBoat.BoatType type, boolean isChestBoat) {
        this.type = type;
        this.isChestBoat = isChestBoat;
    }

    public ItemStack execute(BlockSource source, ItemStack stack) {
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        Level level = source.level();
        EntityType<? extends AbstractBoat> entityType = isChestBoat ?
                GTEntityTypes.CHEST_BOAT.get() : GTEntityTypes.BOAT.get();
        double d0 = 0.5625 + (double) entityType.getWidth() / 2.0;
        var center = source.center();
        double d1 = center.x() + (double) direction.getStepX() * d0;
        double d2 = center.y() + (double) ((float) direction.getStepY() * 1.125F);
        double d3 = center.z() + (double) direction.getStepZ() * d0;
        BlockPos blockpos = source.pos().relative(direction);

        AbstractBoat boat;
        if (isChestBoat) {
            boat = new GTChestBoat(level, d1, d2, d3);
            ((GTChestBoat) boat).setBoatType(type);
        } else {
            boat = new GTBoat(level, d1, d2, d3);
            ((GTBoat) boat).setBoatType(type);
        }

        boat.setYRot(direction.toYRot());
        double d4;
        if (boat.canBoatInFluid(level.getFluidState(blockpos))) {
            d4 = 1.0;
        } else {
            if (!level.getBlockState(blockpos).isAir() ||
                    !boat.canBoatInFluid(level.getFluidState(blockpos.below()))) {
                return this.defaultDispenseItemBehavior.dispense(source, stack);
            }

            d4 = 0.0;
        }

        boat.setInitialPos(d1, d2 + d4, d3);
        level.addFreshEntity(boat);
        stack.shrink(1);
        return stack;
    }

    protected void playSound(BlockSource source) {
        source.level().levelEvent(1000, source.pos(), 0);
    }
}
