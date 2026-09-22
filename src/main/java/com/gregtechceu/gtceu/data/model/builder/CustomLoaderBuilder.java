package com.gregtechceu.gtceu.data.model.builder;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

/** Adds a loader id and loader-specific fields to a parent model builder. */
public abstract class CustomLoaderBuilder<T extends ModelBuilder<T>> {
    protected final Identifier loaderId;
    protected final T parent;
    protected final ModelFileHelper existingFileHelper;

    protected CustomLoaderBuilder(Identifier loaderId, T parent, ModelFileHelper existingFileHelper) {
        this.loaderId = loaderId;
        this.parent = parent;
        this.existingFileHelper = existingFileHelper;
    }

    public JsonObject toJson(JsonObject json) {
        json.addProperty("loader", loaderId.toString());
        return json;
    }

    public T end() {
        parent.attachCustomLoader(this);
        return parent;
    }
}
