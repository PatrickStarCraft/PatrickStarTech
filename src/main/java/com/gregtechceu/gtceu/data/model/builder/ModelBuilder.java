package com.gregtechceu.gtceu.data.model.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** Mutable JSON builder for block and item model resources. */
public abstract class ModelBuilder<T extends ModelBuilder<T>> extends ModelFile {
    protected final ModelFileHelper existingFileHelper;
    protected ModelFile parent;
    protected final Map<String, String> textures = new LinkedHashMap<>();
    protected final List<ElementData> elements = new ArrayList<>();
    protected final JsonObject display = new JsonObject();
    protected String guiLight;
    protected Identifier renderType;
    protected CustomLoaderBuilder<T> customLoader;
    private int anonymousElement;

    protected ModelBuilder(Identifier location, ModelFileHelper helper) {
        super(location);
        this.existingFileHelper = Objects.requireNonNull(helper, "helper");
    }

    @SuppressWarnings("unchecked")
    private T self() { return (T) this; }

    @Override
    public void assertExists() {}

    public T parent(ModelFile parent) { this.parent = Objects.requireNonNull(parent); return self(); }
    public T parent(Identifier parent) { return parent(new ModelFile.UncheckedModelFile(parent)); }
    public T guiLight(String guiLight) { this.guiLight = Objects.requireNonNull(guiLight); return self(); }
    public T texture(String key, Identifier value) { return texture(key, value.toString()); }
    public T texture(String key, String value) {
        String texture = value.startsWith("#") || value.indexOf(':') >= 0 ? value : getLocation().getNamespace() + ":" + value;
        textures.put(key, texture);
        return self();
    }
    public T renderType(Identifier renderType) { this.renderType = renderType; return self(); }
    public T renderType(String renderType) { return renderType(Identifier.parse(renderType)); }

    public <L extends CustomLoaderBuilder<T>> L customLoader(
            BiFunction<T, ModelFileHelper, L> factory) {
        L loader = factory.apply(self(), existingFileHelper);
        this.customLoader = loader;
        return loader;
    }

    public <L extends CustomLoaderBuilder<T>> L customLoader(
            java.util.function.Function<T, L> factory) {
        L loader = factory.apply(self());
        this.customLoader = loader;
        return loader;
    }

    void attachCustomLoader(CustomLoaderBuilder<T> loader) { this.customLoader = loader; }

    public ElementBuilder element() {
        ElementData element = new ElementData();
        element.name = "element_" + anonymousElement++;
        elements.add(element);
        return new ElementBuilder(element);
    }

    public JsonObject toJson() {
        JsonObject json = baseJson();
        return customLoader == null ? json : customLoader.toJson(json);
    }

    JsonObject baseJson() {
        JsonObject json = new JsonObject();
        if (parent != null) {
            parent.assertExists();
            json.addProperty("parent", parent.getLocation().toString());
        }
        if (guiLight != null) json.addProperty("gui_light", guiLight);
        if (!textures.isEmpty()) {
            JsonObject textureJson = new JsonObject();
            textures.forEach(textureJson::addProperty);
            json.add("textures", textureJson);
        }
        if (!elements.isEmpty()) {
            JsonArray array = new JsonArray();
            for (ElementData element : elements) array.add(element.toJson());
            json.add("elements", array);
        }
        if (!display.isEmpty()) json.add("display", display.deepCopy());
        if (renderType != null) json.addProperty("render_type", renderType.toString());
        return json;
    }

    protected static final class ElementData {
        String name;
        float[] from = { 0, 0, 0 };
        float[] to = { 16, 16, 16 };
        Direction.Axis rotationAxis;
        float rotationAngle;
        float rotationOrigin = 8;
        boolean shade = true;
        final Map<String, FaceData> faces = new LinkedHashMap<>();

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.add("from", vector(from));
            json.add("to", vector(to));
            if (rotationAxis != null) {
                JsonObject rotation = new JsonObject();
                rotation.add("origin", new JsonArray());
                JsonArray origin = new JsonArray();
                origin.add(rotationOrigin); origin.add(rotationOrigin); origin.add(rotationOrigin);
                rotation.add("origin", origin);
                rotation.addProperty("axis", rotationAxis.getName().toLowerCase(java.util.Locale.ROOT));
                rotation.addProperty("angle", rotationAngle);
                rotation.addProperty("rescale", false);
                json.add("rotation", rotation);
            }
            if (!shade) json.addProperty("shade", false);
            JsonObject facesJson = new JsonObject();
            faces.forEach((key, value) -> facesJson.add(key, value.toJson()));
            json.add("faces", facesJson);
            return json;
        }

        private static JsonArray vector(float[] vector) {
            JsonArray array = new JsonArray();
            for (float value : vector) array.add(value);
            return array;
        }
    }

    public final class ElementBuilder {
        private final ElementData data;
        private ElementBuilder(ElementData data) { this.data = data; }
        public ElementBuilder from(float x, float y, float z) { data.from = new float[]{x,y,z}; return this; }
        public ElementBuilder to(float x, float y, float z) { data.to = new float[]{x,y,z}; return this; }
        public ElementBuilder rotation(Direction.Axis axis, float angle, float origin) {
            data.rotationAxis = axis;
            data.rotationAngle = angle; data.rotationOrigin = origin; return this;
        }
        public ElementBuilder shade(boolean shade) { data.shade = shade; return this; }
        public FaceBuilder face(Direction direction) {
            return new FaceBuilder(data, data.faces.computeIfAbsent(direction.getName(), ignored -> new FaceData()));
        }
        public ElementBuilder faces(BiConsumer<Direction, FaceBuilder> configurator) {
            for (Direction direction : Direction.values()) configurator.accept(direction, face(direction));
            return this;
        }
        public T end() { return self(); }
    }

    private static final class FaceData {
        String texture;
        Direction cullface;
        int rotation;
        Integer tintIndex;
        float[] uv;
        Integer lightEmission;
        Boolean ambientOcclusion;
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (texture != null) json.addProperty("texture", texture);
            if (cullface != null) json.addProperty("cullface", cullface.getName());
            if (rotation != 0) json.addProperty("rotation", rotation);
            if (tintIndex != null) json.addProperty("tintindex", tintIndex);
            if (uv != null) {
                JsonArray array = new JsonArray();
                for (float value : uv) array.add(value);
                json.add("uv", array);
            }
            if (lightEmission != null || ambientOcclusion != null) {
                JsonObject extraData = new JsonObject();
                if (lightEmission != null) extraData.addProperty("light_emission", lightEmission);
                if (ambientOcclusion != null) extraData.addProperty("ambient_occlusion", ambientOcclusion);
                json.add("neoforge_data", extraData);
            }
            return json;
        }
    }

    public final class FaceBuilder {
        private final ElementData element;
        private final FaceData data;
        private FaceBuilder(ElementData element, FaceData data) { this.element = element; this.data = data; }
        public FaceBuilder texture(String texture) { data.texture = texture; return this; }
        public FaceBuilder cullface(Direction direction) { data.cullface = direction; return this; }
        public FaceBuilder rotation(int rotation) { data.rotation = rotation; return this; }
        public FaceBuilder tintindex(int tintIndex) { data.tintIndex = tintIndex; return this; }
        public FaceBuilder uv(float u1, float v1, float u2, float v2) { data.uv = new float[]{u1,v1,u2,v2}; return this; }
        public FaceBuilder uvs(float u1, float v1, float u2, float v2) { return uv(u1, v1, u2, v2); }
        public FaceBuilder emissivity(int min, int max) {
            if (min != max) {
                throw new IllegalArgumentException("Minecraft 26.2 face light emission requires one value, got " + min + ".." + max);
            }
            if (min < 0 || min > 15) {
                throw new IllegalArgumentException("Minecraft 26.2 face light emission must be between 0 and 15: " + min);
            }
            data.lightEmission = min;
            return this;
        }
        public FaceBuilder ao(boolean ambientOcclusion) { data.ambientOcclusion = ambientOcclusion; return this; }
        public ElementBuilder end() { return new ElementBuilder(element); }
    }
}
