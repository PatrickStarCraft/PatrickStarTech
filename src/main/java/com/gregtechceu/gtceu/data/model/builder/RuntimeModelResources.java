package com.gregtechceu.gtceu.data.model.builder;

import net.minecraft.resources.Identifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.BiConsumer;

/** Shared JSON path and item model helpers for runtime and datagen resources. */
public final class RuntimeModelResources {

    private RuntimeModelResources() {}

    /** Returns the resource location for a logical model ID such as {@code gtceu:block/example}. */
    public static Identifier modelPath(Identifier modelId) {
        return modelId.withPrefix("models/").withSuffix(".json");
    }

    /** Returns the resource location for a blockstate ID such as {@code gtceu:example}. */
    public static Identifier blockStatePath(Identifier blockId) {
        return blockId.withPrefix("blockstates/").withSuffix(".json");
    }

    /** Returns the resource location for an item definition ID such as {@code gtceu:example}. */
    public static Identifier itemDefinitionPath(Identifier itemId) {
        return itemId.withPrefix("items/").withSuffix(".json");
    }

    public static JsonObject simpleBlockState(Identifier modelId) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modelId.toString());

        JsonObject variants = new JsonObject();
        variants.add("", variant);

        JsonObject blockState = new JsonObject();
        blockState.add("variants", variants);
        return blockState;
    }

    public static JsonObject itemDefinition(Identifier modelId) {
        return itemDefinition(modelId, null);
    }

    public static JsonObject itemDefinition(Identifier modelId, JsonArray tints) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelId.toString());
        if (tints != null && !tints.isEmpty()) {
            model.add("tints", tints.deepCopy());
        }

        JsonObject definition = new JsonObject();
        definition.add("model", model);
        return definition;
    }

    public static JsonArray constantTints(int... colors) {
        JsonArray tints = new JsonArray();
        for (int color : colors) {
            JsonObject tint = new JsonObject();
            tint.addProperty("type", "minecraft:constant");
            tint.addProperty("value", color);
            tints.add(tint);
        }
        return tints;
    }

    public static JsonArray dynamicTint(Identifier type) {
        JsonArray tints = new JsonArray();
        JsonObject tint = new JsonObject();
        tint.addProperty("type", type.toString());
        tints.add(tint);
        return tints;
    }

    public static void emitModel(Identifier modelId, JsonElement json,
                                 BiConsumer<Identifier, JsonElement> sink) {
        sink.accept(modelPath(modelId), json);
    }

    public static void emitBlockState(Identifier blockId, JsonElement json,
                                      BiConsumer<Identifier, JsonElement> sink) {
        sink.accept(blockStatePath(blockId), json);
    }

    public static void emitItem(Identifier itemId, Identifier modelId,
                                BiConsumer<Identifier, JsonElement> sink) {
        emitItem(itemId, modelId, null, sink);
    }

    public static void emitItem(Identifier itemId, Identifier modelId, JsonArray tints,
                                BiConsumer<Identifier, JsonElement> sink) {
        JsonObject alias = new JsonObject();
        alias.addProperty("parent", modelId.toString());
        emitItem(itemId, alias, itemDefinition(modelId, tints), sink);
    }

    /** Emits both the legacy model alias and its modern item definition. */
    public static void emitItem(Identifier itemId, JsonElement modelJson, JsonElement definition,
                                BiConsumer<Identifier, JsonElement> sink) {
        emitModel(itemId.withPrefix("item/"), modelJson, sink);
        emitItem(itemId, definition, sink);
    }

    /** Emits only the modern item definition for already-authored JSON. */
    public static void emitItem(Identifier itemId, JsonElement definition,
                                BiConsumer<Identifier, JsonElement> sink) {
        sink.accept(itemDefinitionPath(itemId), definition);
    }

}
