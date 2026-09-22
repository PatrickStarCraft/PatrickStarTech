package com.gregtechceu.gtceu.integration.recipeviewer.jei.subtype;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;

import java.util.List;

/* From JEI's Potion item subtype interpreter */
public class PotionFluidSubtypeInterpreter implements IIngredientSubtypeInterpreter<FluidStack> {

    @Override
    public String apply(FluidStack ingredient, UidContext context) {
        PotionContents contents = ingredient.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.potion().isEmpty() && contents.customEffects().isEmpty())
            return IIngredientSubtypeInterpreter.NONE;
        String potionTypeString = contents.potion().map(potion -> potion.value().name()).orElse("");

        StringBuilder stringBuilder = new StringBuilder(potionTypeString);
        List<MobEffectInstance> effects = contents.customEffects();

        contents.potion().ifPresent(potion -> potion.value().getEffects().forEach(effect -> stringBuilder.append(";")
                .append(effect)));
        for (MobEffectInstance effect : effects) {
            stringBuilder.append(";")
                    .append(effect);
        }
        return stringBuilder.toString();
    }
}
