package com.gregtechceu.gtceu.common.data.materials;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import com.mojang.datafixers.util.Pair;

import java.util.List;

public class GTFoods {

    public static final List<Pair<MobEffectInstance, Float>> CHOCOLATE_EFFECTS = List.of(
            Pair.of(new MobEffectInstance(MobEffects.SPEED, 200, 1), 0.1F));
    public static final Consumable CHOCOLATE_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(
                    CHOCOLATE_EFFECTS.get(0).getFirst(), CHOCOLATE_EFFECTS.get(0).getSecond()))
            .build();
    public static final FoodProperties CHOCOLATE = new FoodProperties.Builder()
            .alwaysEdible().nutrition(4).saturationModifier(0.3F).build();

    public static final List<Pair<MobEffectInstance, Float>> DRINK_EFFECTS = List.of(
            Pair.of(new MobEffectInstance(MobEffects.HASTE, 800, 1), 0.9F));
    public static final Consumable DRINK_CONSUMABLE = Consumables.defaultDrink()
            .onConsume(new ApplyStatusEffectsConsumeEffect(
                    DRINK_EFFECTS.get(0).getFirst(), DRINK_EFFECTS.get(0).getSecond()))
            .build();
    public static final FoodProperties DRINK = new FoodProperties.Builder()
            .alwaysEdible().nutrition(4).saturationModifier(0.3F).build();

    public static final FoodProperties ANTIDOTE = new FoodProperties.Builder()
            .alwaysEdible().build();
    public static final Consumable ANTIDOTE_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(0.8F)
            .build();

    public static void init() {}
}
