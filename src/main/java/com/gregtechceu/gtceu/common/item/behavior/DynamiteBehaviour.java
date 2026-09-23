package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.common.entity.DynamiteEntity;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.server.level.ServerLevel;

public class DynamiteBehaviour implements IInteractionItem {

    @Override
    public void onAttached(Item item) {
        DispenserBlock.registerBehavior(item, new DefaultDispenseItemBehavior() {

            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction direction = source.state().getValue(DispenserBlock.FACING);
                Position position = DispenserBlock.getDispensePosition(source);
                ServerLevel level = source.level();
                DynamiteEntity projectile = new DynamiteEntity(position.x(), position.y(), position.z(), level);
                projectile.setItem(stack.copyWithCount(1));
                Projectile.spawnProjectileUsingShoot(projectile, level, stack,
                        direction.getStepX(), direction.getStepY(), direction.getStepZ(), 1.1F, 6.0F);
                stack.shrink(1);
                return stack;
            }

            @Override
            protected void playSound(BlockSource source) {
                source.level().levelEvent(1002, source.pos(), 0);
            }
        });
    }

    @Override
    public InteractionResult use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);

        if (!player.isCreative()) {
            itemstack.shrink(1);
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS.heldItemTransformedTo(itemstack);
        }

        DynamiteEntity entity = new DynamiteEntity(player, level);
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.7F, 1.0F);

        level.addFreshEntity(entity);

        return InteractionResult.SUCCESS.heldItemTransformedTo(itemstack);
    }
}
