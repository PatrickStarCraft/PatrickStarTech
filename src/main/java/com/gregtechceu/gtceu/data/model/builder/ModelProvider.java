package com.gregtechceu.gtceu.data.model.builder;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/** Namespace-scoped model resource collector. */
public abstract class ModelProvider<T extends ModelBuilder<T>> {
    public final Map<Identifier, T> generatedModels = new LinkedHashMap<>();
    protected final String namespace;
    protected final ModelFileHelper existingFileHelper;
    private long nestedModelSequence;

    protected ModelProvider(String namespace, ModelFileHelper helper) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.existingFileHelper = Objects.requireNonNull(helper, "helper");
    }

    protected abstract T create(Identifier id);
    protected abstract String getFolder();

    protected Identifier id(String path) {
        Identifier id = path.indexOf(':') >= 0 ? Identifier.parse(path) : Identifier.fromNamespaceAndPath(namespace, path);
        return id.getPath().contains("/") ? id : id.withPrefix(getFolder() + "/");
    }

    public T getBuilder(String path) { return getBuilder(id(path)); }

    public T getBuilder(Identifier location) {
        T model = generatedModels.get(location);
        if (model == null) {
            model = create(location);
            generatedModels.put(location, model);
            existingFileHelper.trackGenerated(location, ModelFileHelper.ResourceType.MODEL);
        }
        return model;
    }

    public T nested() {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, "__nested/" + nestedModelSequence++);
        return create(id);
    }

    public ModelFile getExistingFile(Identifier location) {
        return new ModelFile.ExistingModelFile(location, existingFileHelper);
    }

    public T withExistingParent(String name, Identifier parent) {
        return getBuilder(name).parent(getExistingFile(parent));
    }

    public T withExistingParent(String name, ModelFile parent) {
        return getBuilder(name).parent(parent);
    }

    public String getNamespace() { return namespace; }
    public ModelFileHelper getExistingFileHelper() { return existingFileHelper; }
    public Identifier modLoc(String path) { return Identifier.fromNamespaceAndPath(namespace, path); }
    public Identifier mcLoc(String path) { return Identifier.fromNamespaceAndPath("minecraft", path); }

    public void emitModels(BiConsumer<Identifier, JsonElement> sink) {
        generatedModels.forEach((id, model) -> sink.accept(
                Identifier.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json"),
                model.toJson()));
    }

    public void clear() { generatedModels.clear(); }
}
