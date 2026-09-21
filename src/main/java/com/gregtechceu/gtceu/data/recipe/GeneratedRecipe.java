package com.gregtechceu.gtceu.data.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/** Immutable JSON resource emitted into GT's runtime data pack, not a Minecraft recipe implementation. */
public record GeneratedRecipe(Identifier id, JsonObject recipeJson,
                              @Nullable Identifier advancementId, @Nullable JsonObject advancementJson) {

    public static final FileToIdConverter RECIPES = FileToIdConverter.json("recipe");
    public static final FileToIdConverter ADVANCEMENTS = FileToIdConverter.json("advancement");

    public GeneratedRecipe {
        Objects.requireNonNull(id);
        recipeJson = Objects.requireNonNull(recipeJson).deepCopy();
        if ((advancementId == null) != (advancementJson == null)) {
            throw new IllegalArgumentException("Advancement ID and JSON must be supplied together");
        }
        if (advancementJson != null) advancementJson = advancementJson.deepCopy();
    }

    @Override
    public JsonObject recipeJson() { return recipeJson.deepCopy(); }

    @Override
    public @Nullable JsonObject advancementJson() {
        return advancementJson == null ? null : advancementJson.deepCopy();
    }

    public static GeneratedRecipe create(Identifier id, RecipeSerializer<?> serializer, Consumer<JsonObject> writer) {
        var serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
        if (serializerId == null) throw new IllegalArgumentException("Unregistered recipe serializer for " + id);
        var json = new JsonObject();
        writer.accept(json);
        // The selected serializer owns outer dispatch. Machine-specific fields stay in the body.
        json.addProperty("type", serializerId.toString());
        return new GeneratedRecipe(id, json, null, null);
    }
}
