package com.gregtechceu.gtceu.data.model.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.List;

/** Ordered alternative models assigned to one state or multipart part. */
public final class ConfiguredModelList {
    private final List<ConfiguredModel> models;

    public ConfiguredModelList(ConfiguredModel... models) {
        if (models.length == 0) throw new IllegalArgumentException("A configured model list needs at least one model");
        this.models = List.of(models.clone());
    }

    public List<ConfiguredModel> getModels() {
        return models;
    }

    public JsonElement toJson() {
        if (models.size() == 1) return models.get(0).toJson(false);
        JsonArray array = new JsonArray();
        for (ConfiguredModel model : models) array.add(model.toJson(true));
        return array;
    }
}
