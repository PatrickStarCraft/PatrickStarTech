package com.gregtechceu.gtceu.data.model.builder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Multipart output. Each part is independent; conditions in a part are ANDed unless useOr() is selected. */
public final class MultiPartBlockStateBuilder {

    private final Block block;
    private final List<PartBuilder> parts = new ArrayList<>();

    public MultiPartBlockStateBuilder(Block block) { this.block = block; }

    public PartBuilder part(ConfiguredModel... models) {
        var part = new PartBuilder(new ConfiguredModelList(models));
        parts.add(part);
        return part;
    }

    public PartBuilder part(ModelFile model) { return part(ConfiguredModel.builder().modelFile(model).build()); }

    public ConfiguredModel.Builder<PartBuilder> part() { return new ConfiguredModel.Builder<>(this::part); }

    public JsonObject toJson() {
        if (parts.isEmpty()) throw new IllegalStateException("No multipart models for " + block);
        JsonArray entries = new JsonArray();
        parts.forEach(part -> entries.add(part.toJson()));
        JsonObject result = new JsonObject();
        result.add("multipart", entries);
        return result;
    }

    public final class PartBuilder {

        private final ConfiguredModelList models;
        private final JsonObject conditions = new JsonObject();
        private boolean useOr;

        private PartBuilder(ConfiguredModelList models) { this.models = models; }

        @SafeVarargs
        public final <T extends Comparable<T>> PartBuilder condition(Property<T> property, T... values) {
            if (!block.getStateDefinition().getProperties().contains(property) || values.length == 0 ||
                    !property.getPossibleValues().containsAll(Arrays.asList(values))) {
                throw new IllegalArgumentException("Invalid multipart condition for " + block + ": " + property);
            }
            if (conditions.has(property.getName())) throw new IllegalArgumentException("Condition set twice: " + property);
            conditions.addProperty(property.getName(), Arrays.stream(values).map(property::getName).collect(Collectors.joining("|")));
            return this;
        }

        public PartBuilder useOr() { useOr = true; return this; }
        public MultiPartBlockStateBuilder end() { return MultiPartBlockStateBuilder.this; }

        private JsonObject toJson() {
            JsonObject result = new JsonObject();
            result.add("apply", models.toJson());
            if (!conditions.isEmpty()) {
                if (useOr) {
                    JsonArray any = new JsonArray();
                    conditions.entrySet().forEach(entry -> {
                        JsonObject term = new JsonObject();
                        term.add(entry.getKey(), entry.getValue());
                        any.add(term);
                    });
                    JsonObject condition = new JsonObject();
                    condition.add("OR", any);
                    result.add("when", condition);
                } else result.add("when", conditions.deepCopy());
            }
            return result;
        }
    }
}
