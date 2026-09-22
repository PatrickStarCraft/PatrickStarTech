package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.tag.TagUtil;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.VoidFluidHandler;

import com.mojang.serialization.MapCodec;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public final class FluidContainerIngredient implements ICustomIngredient {

    public static final Identifier TYPE = GTCEu.id("fluid_container");
    public static final MapCodec<FluidContainerIngredient> CODEC = FluidIngredient.CODEC.fieldOf("fluid")
            .xmap(FluidContainerIngredient::new, ingredient -> ingredient.fluid);
    public static final IngredientType<FluidContainerIngredient> INGREDIENT_TYPE = new IngredientType<>(CODEC);

    private final FluidIngredient fluid;

    public FluidContainerIngredient(FluidIngredient fluid) {
        this.fluid = fluid;
    }

    public FluidContainerIngredient(FluidStack fluidStack) {
        this(FluidIngredient.of(TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).getPath()),
                fluidStack.getAmount(), com.gregtechceu.gtceu.api.transfer.fluid.FluidStackData.readNullable(fluidStack)));
    }

    public FluidContainerIngredient(TagKey<Fluid> tag, int amount) {
        this(FluidIngredient.of(tag, amount, null));
    }

    public FluidIngredient getFluid() {
        return fluid;
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Arrays.stream(getItems()).filter(stack -> !stack.isEmpty()).map(ItemStack::typeHolder).distinct();
    }

    public ItemStack[] getItems() {
        return Arrays.stream(this.fluid.getStacks())
                .map(FluidUtil::getFilledBucket)
                .filter(stack -> !stack.isEmpty())
                .toArray(ItemStack[]::new);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return FluidUtil.getFluidContained(stack).map(contained -> fluid.test(contained) &&
                contained.getAmount() >= fluid.getAmount()).orElse(false) &&
                FluidUtil.tryEmptyContainer(stack, VoidFluidHandler.INSTANCE, fluid.getAmount(), null, false)
                        .isSuccess();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<FluidContainerIngredient> getType() {
        return INGREDIENT_TYPE;
    }

    public ItemStack getExtractedStack(ItemStack input) {
        FluidActionResult result = FluidUtil.tryEmptyContainer(input, VoidFluidHandler.INSTANCE, fluid.getAmount(),
                null, true);
        return result.isSuccess() ? result.getResult() : input;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof FluidContainerIngredient other && fluid.equals(other.fluid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fluid);
    }
}
