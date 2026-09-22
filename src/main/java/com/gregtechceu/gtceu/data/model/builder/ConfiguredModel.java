package com.gregtechceu.gtceu.data.model.builder;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** A model reference plus the blockstate transforms applied to it. */
public final class ConfiguredModel {
    public static final int DEFAULT_WEIGHT = 1;

    public final ModelFile model;
    public final int rotationX;
    public final int rotationY;
    public final int rotationZ;
    public final boolean uvLock;
    public final int weight;

    private ConfiguredModel(Builder<?> builder) {
        this.model = Objects.requireNonNull(builder.model, "modelFile");
        this.rotationX = normalizeRotation(builder.rotationX, "x");
        this.rotationY = normalizeRotation(builder.rotationY, "y");
        this.rotationZ = normalizeRotation(builder.rotationZ, "z");
        this.uvLock = builder.uvLock;
        if (builder.weight <= 0) throw new IllegalArgumentException("Model weight must be positive");
        this.weight = builder.weight;
    }

    private static int normalizeRotation(int rotation, String axis) {
        if (rotation % 90 != 0) throw new IllegalArgumentException("Rotation " + axis + " must be a multiple of 90: " + rotation);
        return Math.floorMod(rotation, 360);
    }

    public ModelFile getModel() { return model; }
    public int getRotationX() { return rotationX; }
    public int getRotationY() { return rotationY; }
    public int getRotationZ() { return rotationZ; }
    public boolean isUvLock() { return uvLock; }
    public boolean getUvLock() { return uvLock; }
    public int getWeight() { return weight; }

    /** Serialize a normal blockstate model reference; machine models may inline it themselves. */
    public JsonObject toJson(boolean includeWeight) {
        model.assertExists();
        JsonObject json = new JsonObject();
        json.addProperty("model", model.getLocation().toString());
        if (rotationX != 0) json.addProperty("x", rotationX);
        if (rotationY != 0) json.addProperty("y", rotationY);
        if (rotationZ != 0) json.addProperty("z", rotationZ);
        if (uvLock) json.addProperty("uvlock", true);
        if (includeWeight && weight != DEFAULT_WEIGHT) json.addProperty("weight", weight);
        return json;
    }

    public JsonObject toJSON(boolean includeWeight) { return toJson(includeWeight); }
    public JsonObject toJson() { return toJson(false); }

    public static Builder<ConfiguredModel[]> builder() {
        return new Builder<>(Function.identity());
    }

    public static <P> Builder<P> builder(Function<ConfiguredModel[], P> onAdd) {
        return new Builder<>(onAdd);
    }

    public static <P> Builder<P> builder(P ignoredParent, Function<ConfiguredModel[], P> onAdd) {
        return new Builder<>(onAdd);
    }

    public static final class Builder<P> {
        private final Function<ConfiguredModel[], P> onAdd;
        private final List<ConfiguredModel> alternatives;
        private ModelFile model;
        private int rotationX;
        private int rotationY;
        private int rotationZ;
        private boolean uvLock;
        private int weight = DEFAULT_WEIGHT;

        public Builder(Function<ConfiguredModel[], P> onAdd) {
            this(onAdd, new ArrayList<>());
        }

        private Builder(Function<ConfiguredModel[], P> onAdd, List<ConfiguredModel> alternatives) {
            this.onAdd = Objects.requireNonNull(onAdd, "onAdd");
            this.alternatives = alternatives;
        }

        private ConfiguredModel buildOne() { return new ConfiguredModel(this); }
        public Builder<P> modelFile(ModelFile model) { this.model = model; return this; }
        public Builder<P> modelFile(Identifier model) { return modelFile(new ModelFile.UncheckedModelFile(model)); }
        public Builder<P> rotationX(int rotation) { this.rotationX = rotation; return this; }
        public Builder<P> rotationY(int rotation) { this.rotationY = rotation; return this; }
        public Builder<P> rotationZ(int rotation) { this.rotationZ = rotation; return this; }
        public Builder<P> uvLock(boolean uvLock) { this.uvLock = uvLock; return this; }
        public Builder<P> weight(int weight) { this.weight = weight; return this; }
        public ConfiguredModel buildLast() { return buildOne(); }

        public Builder<P> nextModel() {
            alternatives.add(buildOne());
            model = null;
            rotationX = rotationY = rotationZ = 0;
            uvLock = false;
            weight = DEFAULT_WEIGHT;
            return new Builder<>(onAdd, alternatives);
        }

        public ConfiguredModel[] build() {
            List<ConfiguredModel> result = new ArrayList<>(alternatives);
            result.add(buildOne());
            return result.toArray(ConfiguredModel[]::new);
        }

        public P addModel() {
            alternatives.add(buildOne());
            return onAdd.apply(alternatives.toArray(ConfiguredModel[]::new));
        }

        public P end() { return addModel(); }
    }
}
