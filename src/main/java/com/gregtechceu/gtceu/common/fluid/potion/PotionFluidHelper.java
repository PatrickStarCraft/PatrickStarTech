package com.gregtechceu.gtceu.common.fluid.potion;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTFluids;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import com.mojang.datafixers.util.Pair;
import com.google.common.collect.Lists;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PotionFluidHelper {

    public static final int BOTTLE_AMOUNT = FluidType.BUCKET_VOLUME / 4;
    public static final int MB_PER_RECIPE = BOTTLE_AMOUNT * 3;

    private static ItemStack potionStack(Potion potion) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder(potion)));
        return stack;
    }

    public static Potion getPotionFromItemStack(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion().map(Holder::value).orElse(null);
    }

    public static Pair<FluidStack, ItemStack> emptyPotion(ItemStack stack, boolean simulate) {
        FluidStack fluid = getFluidFromPotionItem(stack, BOTTLE_AMOUNT);
        if (!simulate)
            stack.shrink(1);
        return Pair.of(fluid, new ItemStack(Items.GLASS_BOTTLE));
    }

    public static FluidIngredient potionIngredient(Potion potion, int amount) {
        FluidStack stack = PotionFluidHelper
                .getFluidFromPotionItem(potionStack(potion), amount);
        stack.setAmount(amount);
        return FluidIngredient.of(stack);
    }

    public static FluidIngredient getPotionFluidIngredientFrom(Ingredient potion, int amount) {
        List<FluidStack> fluids = new ArrayList<>();
        for (ItemStack stack : com.gregtechceu.gtceu.api.recipe.ingredient.IngredientStacks.getItems(potion)) {
            FluidStack fluidStack = getFluidFromPotionItem(stack, amount);
            if (!fluidStack.isEmpty()) {
                fluids.add(fluidStack);
            }
        }
        return FluidIngredient.of(fluids);
    }

    public static FluidStack getFluidFromPotionItem(ItemStack stack, int amount) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Potion potion = getPotionFromItemStack(stack);
        if (contents.potion().isEmpty()) {
            return FluidStack.EMPTY;
        }
        List<MobEffectInstance> list = contents.customEffects();
        if (contents.potion().filter(holder -> holder.is(Potions.WATER)).isPresent() && list.isEmpty())
            return new FluidStack(Fluids.WATER, amount);
        return PotionFluid.withEffects(amount, potion, list);
    }

    public static FluidStack getFluidFromPotion(Potion potion, int amount) {
        if (net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder(potion).is(Potions.WATER))
            return new FluidStack(Fluids.WATER, amount);
        return PotionFluid.of(amount, potion);
    }

    public static ItemStack fillBottle(ItemStack stack, FluidStack availableFluid) {
        if (stack.is(Items.GLASS_BOTTLE)) {
            int count = stack.getCount();
            var components = stack.getComponents();
            stack = new ItemStack(Items.POTION);
            stack.setCount(count);
            stack.applyComponents(components);
        }
        PotionContents contents = availableFluid.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    // Modified version of PotionUtils#addPotionTooltip
    @OnlyIn(Dist.CLIENT)
    public static void addPotionTooltip(FluidStack fs, Consumer<Component> tooltip) {
        PotionContents.addPotionTooltip(
                fs.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects(),
                tooltip, 1.0f, 20.0f);
    }

    public static Component formatDuration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) {
            return Component.translatable("effect.duration.infinite");
        } else {
            int time = effect.getDuration();
            Instant start = Instant.now();
            Instant max = Instant.now().plusSeconds(time / 20);
            Duration durationMax = Duration.between(start, max);

            Component unit;

            if (durationMax.getSeconds() <= 60) {
                time = GTMath.saturatedCast(durationMax.getSeconds());
                unit = Component.translatable("item.gtceu.battery.charge_unit.second");
            } else if (durationMax.toMinutes() <= 60) {
                time = GTMath.saturatedCast(durationMax.toMinutes());
                unit = Component.translatable("item.gtceu.battery.charge_unit.minute");
            } else {
                time = GTMath.saturatedCast(durationMax.toHours());
                unit = Component.translatable("item.gtceu.battery.charge_unit.hour");
            }

            return Component.literal(FormattingUtil.formatNumbers(time)).append(unit);
        }
    }
}
