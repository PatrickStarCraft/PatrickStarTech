package com.gregtechceu.gtceu.data.model.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemModelBuilder extends ModelBuilder<ItemModelBuilder> {
    private final List<OverrideData> overrides = new ArrayList<>();

    public ItemModelBuilder(Identifier location, ModelFileHelper helper) {
        super(location, helper);
    }

    public OverrideBuilder override() { return new OverrideBuilder(); }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    public JsonObject toItemDefinition() {
        JsonObject definition = new JsonObject();
        if (overrides.isEmpty()) {
            definition.add("model", modelReference(getLocation()));
            return definition;
        }
        Identifier property = overrides.get(0).predicates.keySet().iterator().next();
        JsonObject dispatch = new JsonObject();
        dispatch.addProperty("type", "minecraft:range_dispatch");
        dispatch.addProperty("property", property.toString());
        dispatch.addProperty("scale", 1.0f);
        JsonArray entries = new JsonArray();
        for (OverrideData override : overrides) {
            if (override.predicates.size() != 1 || !override.predicates.containsKey(property)) {
                throw new IllegalStateException("A range dispatch requires all overrides to use the same single property");
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("threshold", override.predicates.get(property));
            entry.add("model", modelReference(override.model.getLocation()));
            entries.add(entry);
        }
        dispatch.add("entries", entries);
        dispatch.add("fallback", modelReference(getLocation()));
        definition.add("model", dispatch);
        return definition;
    }

    private static JsonObject modelReference(Identifier location) {
        JsonObject reference = new JsonObject();
        reference.addProperty("type", "minecraft:model");
        reference.addProperty("model", location.toString());
        return reference;
    }

    private static final class OverrideData {
        final Map<Identifier, Float> predicates = new LinkedHashMap<>();
        ModelFile model;
    }

    public final class OverrideBuilder {
        private final OverrideData data = new OverrideData();
        public OverrideBuilder predicate(Identifier predicate, float value) {
            data.predicates.put(predicate, value);
            return this;
        }
        public OverrideBuilder model(ModelFile model) { data.model = model; return this; }
        public ItemModelBuilder end() {
            if (data.predicates.isEmpty()) throw new IllegalStateException("Item override needs a predicate");
            if (data.model == null) throw new IllegalStateException("Item override needs a model");
            overrides.add(data);
            return ItemModelBuilder.this;
        }
    }
}
