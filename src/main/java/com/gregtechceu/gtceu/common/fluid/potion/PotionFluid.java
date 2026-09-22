package com.gregtechceu.gtceu.common.fluid.potion;

import com.gregtechceu.gtceu.common.data.GTFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import java.util.Collection;
import java.util.List;

public abstract class PotionFluid extends BaseFlowingFluid {

    public PotionFluid(Properties properties) {
        super(properties);
    }

    public static FluidStack of(int amount, Potion potion) {
        FluidStack fluidStack = new FluidStack(GTFluids.POTION.get()
                .getSource(), amount);
        addPotionToFluidStack(fluidStack, potion);
        return fluidStack;
    }

    public static FluidStack withEffects(int amount, Potion potion, List<MobEffectInstance> customEffects) {
        FluidStack fluidStack = of(amount, potion);
        appendEffects(fluidStack, customEffects);
        return fluidStack;
    }

    public static FluidStack addPotionToFluidStack(FluidStack fluidStack, Potion potion) {
        if (potion == null) {
            fluidStack.remove(DataComponents.POTION_CONTENTS);
            return fluidStack;
        }
        fluidStack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(potion)));
        return fluidStack;
    }

    public static FluidStack appendEffects(FluidStack fluidStack, Collection<MobEffectInstance> customEffects) {
        if (customEffects.isEmpty())
            return fluidStack;
        PotionContents contents = fluidStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance effect : customEffects) contents = contents.withEffectAdded(effect);
        fluidStack.set(DataComponents.POTION_CONTENTS, contents);
        return fluidStack;
    }

    public static class Flowing extends PotionFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends PotionFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class PotionFluidType extends FluidType {
        public PotionFluidType(Properties properties) {
            super(properties);
        }

        @Override
        public String getDescriptionId(FluidStack stack) {
            return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                    .getName(Items.POTION.getDescriptionId() + ".effect.").getString();
        }
    }
}
