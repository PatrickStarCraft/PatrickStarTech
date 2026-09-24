package com.gregtechceu.gtceu.client.model.runtimegen;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.item.armor.GTArmorItem;
import com.gregtechceu.gtceu.common.item.armor.GTDyeableArmorItem;
import com.gregtechceu.gtceu.data.model.builder.RuntimeModelResources;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Item;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ArmorItemModelGenerator {

    private static final Set<ArmorItemModelGenerator> MODELS = new HashSet<>();
    private static final String[] TRIM_MATERIALS = {
            "quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst"
    };

    public static void reinitModels() {
        for (ArmorItemModelGenerator model : MODELS) {
            // read the base armor model JSON
            Identifier modelLocation = GTCEu.id("models/item/armor/%s.json".formatted(model.armorType.getName()));
            JsonObject original;
            try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(modelLocation)) {
                original = GsonHelper.parse(reader);
            } catch (IOException e) {
                GTCEu.LOGGER.warn("Unable to load model: '{}': {}", modelLocation, e);
                continue;
            }

            RuntimeModelResources.emitItem(BuiltInRegistries.ITEM.getKey(model.item),
                    createItemDefinition(model, original), GTDynamicResourcePack::addResource);
        }
    }

    private static JsonObject createItemDefinition(ArmorItemModelGenerator armor, JsonObject legacyModel) {
        JsonObject select = new JsonObject();
        select.addProperty("type", "minecraft:select");
        select.addProperty("property", "minecraft:trim_material");

        JsonArray cases = new JsonArray();
        JsonArray overrides = legacyModel.getAsJsonArray("overrides");
        if (overrides != null) {
            for (var overrideElement : overrides) {
                JsonObject override = overrideElement.getAsJsonObject();
                JsonObject predicate = override.getAsJsonObject("predicate");
                if (predicate == null || !predicate.has("trim_type")) {
                    continue;
                }

                int trimIndex = Math.round(predicate.get("trim_type").getAsFloat() * 10) - 1;
                if (trimIndex < 0 || trimIndex >= TRIM_MATERIALS.length) {
                    continue;
                }

                String modelId = override.get("model").getAsString();
                JsonObject entry = new JsonObject();
                entry.addProperty("when", "minecraft:" + TRIM_MATERIALS[trimIndex]);
                entry.add("model", tintedModel(modelId, armor.item));
                cases.add(entry);
            }
        }
        select.add("cases", cases);
        select.add("fallback", tintedModel(GTCEu.id("item/armor/" + armor.armorType.getName()).toString(), armor.item));

        JsonObject definition = new JsonObject();
        definition.add("model", select);
        return definition;
    }

    private static JsonObject tintedModel(String modelId, Item item) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelId);

        if (GTArmorItem.class.isInstance(item)) {
            GTArmorItem armor = GTArmorItem.class.cast(item);
            JsonObject tint = new JsonObject();
            if (GTDyeableArmorItem.class.isInstance(item)) {
                tint.addProperty("type", "minecraft:dye");
                tint.addProperty("default", armor.material.getLayerARGB(0));
            } else {
                tint.addProperty("type", "minecraft:constant");
                tint.addProperty("value", armor.material.getLayerARGB(0));
            }
            JsonArray tints = new JsonArray();
            tints.add(tint);
            model.add("tints", tints);
        }
        return model;
    }

    private final Item item;
    private final ArmorType armorType;

    protected ArmorItemModelGenerator(Item item, ArmorType armorType) {
        this.item = item;
        this.armorType = armorType;
    }

    public static void add(Item item, ArmorType armorType) {
        MODELS.add(new ArmorItemModelGenerator(item, armorType));
    }
}
