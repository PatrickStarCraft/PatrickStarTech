package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import org.jetbrains.annotations.Nullable;

public class GTBucketItem extends BucketItem {

    final Material material;
    final String langKey;

    public GTBucketItem(Fluid fluid, Properties properties, Material material, String langKey) {
        super(fluid, properties);
        this.material = material;
        this.langKey = langKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component materialName = material.getLocalizedName();
        return Component.translatable("item.gtceu.bucket", Component.translatable(this.langKey, materialName));
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
        var property = material.getProperty(PropertyKey.FLUID);
        if (property != null) {
            var fluid = material.getFluid();
            if (fluid instanceof GTFluid gtFluid) {
                int burnTime = gtFluid.getBurnTime();
                if (burnTime >= 0) {
                    return burnTime;
                }
            }
        }
        return fuelValues.burnDuration(itemStack);
    }

    @Override
    public boolean emptyContents(@Nullable LivingEntity user, Level level, BlockPos pos,
                                 @Nullable BlockHitResult hitResult,
                                 @Nullable ItemStack containerItem) {
        Fluid content = this.getContent();
        if (!(content instanceof FlowingFluid flowingFluid)) return false;

        BlockState blockstate = level.getBlockState(pos);
        Block block = blockstate.getBlock();
        boolean mayReplace = blockstate.canBeReplaced(content);
        boolean shiftKeyDown = user != null && user.isShiftKeyDown();
        boolean placeLiquid = mayReplace || block instanceof LiquidBlockContainer blockContainer &&
                blockContainer.canPlaceLiquid(user, level, pos, blockstate, content);
        boolean canPlaceFluidInsideBlock = blockstate.isAir() || placeLiquid && (!shiftKeyDown || hitResult == null);

        if (!canPlaceFluidInsideBlock) {
            return hitResult != null && this.emptyContents(user, level,
                    hitResult.getBlockPos().relative(hitResult.getDirection()), null, containerItem);
        }

        FluidStack containedFluidStack = containerItem == null ? FluidStack.EMPTY :
                FluidUtil.getFirstStackContained(containerItem);
        var fluidType = content.getFluidType();
        if (!containedFluidStack.isEmpty() && fluidType.isVaporizedOnPlacement(level, pos, containedFluidStack)) {
            fluidType.onVaporize(user, level, pos, containedFluidStack);
            return true;
        }

        if (doesFluidVaporize(material, level, pos)) {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            level.playSound(user, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                    2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);

            for (int l = 0; l < 8; ++l) {
                double xi = i + level.getRandom().nextFloat();
                double xj = j + level.getRandom().nextFloat();
                double xk = k + level.getRandom().nextFloat();
                level.addParticle(ParticleTypes.LARGE_SMOKE, xi, xj, xk, 0.0D, 0.0D, 0.0D);
            }
            return true;
        }

        if (block instanceof LiquidBlockContainer blockContainer &&
                blockContainer.canPlaceLiquid(user, level, pos, blockstate, content)) {
            blockContainer.placeLiquid(level, pos, blockstate, flowingFluid.getSource(false));
            this.playEmptySound(user, level, pos);
            return true;
        } else {
            if (!level.isClientSide() && mayReplace && !blockstate.liquid()) {
                level.destroyBlock(pos, true);
            }

            var fluidBlockState = content.defaultFluidState().createLegacyBlock();
            if (hasFluidBlock(material) && level.setBlock(pos, fluidBlockState, Block.UPDATE_ALL_IMMEDIATE) &&
                    fluidBlockState.getFluidState().isSource()) {
                this.playEmptySound(user, level, pos);
                return true;
            }
        }
        return false;
    }

    private static boolean hasFluidBlock(Material mat) {
        var fluidStorage = mat.getProperty(PropertyKey.FLUID).getStorage();

        for (var key : FluidStorageKey.allKeys()) {
            var fluidEntry = fluidStorage.getEntry(key);
            if (fluidEntry != null) {
                var fluidBuilder = fluidEntry.getBuilder();
                if (fluidBuilder != null && fluidBuilder.hasFluidBlock()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doesFluidVaporize(Material mat, Level level, BlockPos pos) {
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos) &&
                this.getContent().is(FluidTags.WATER)) {
            return true;
        }
        var fluidStorage = mat.getProperty(PropertyKey.FLUID).getStorage();
        var plasmaEntry = fluidStorage.getEntry(FluidStorageKeys.PLASMA);
        var gasEntry = fluidStorage.getEntry(FluidStorageKeys.GAS);
        if (plasmaEntry != null) {
            var plasmaBuilder = plasmaEntry.getBuilder();
            return plasmaBuilder != null && plasmaBuilder.hasFluidBlock();
        } else if (gasEntry != null) {
            var gasBuilder = gasEntry.getBuilder();
            return gasBuilder != null && gasBuilder.hasFluidBlock();
        }
        return false;
    }
}
