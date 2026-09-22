package com.gregtechceu.gtceu.data.model.builder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ItemModelProvider extends ModelProvider<ItemModelBuilder> {
    private final Map<Identifier, JsonElement> itemDefinitions = new LinkedHashMap<>();
    private final Map<Identifier, ItemModelBuilder> itemDefinitionBuilders = new LinkedHashMap<>();

    public ItemModelProvider(String namespace, ModelFileHelper helper) { super(namespace, helper); }

    @Override
    protected ItemModelBuilder create(Identifier id) { return new ItemModelBuilder(id, existingFileHelper); }
    @Override
    protected String getFolder() { return "item"; }

    public ItemModelBuilder withExistingParent(String name, Identifier parent) {
        return getBuilder(name).parent(getExistingFile(parent));
    }

    public ItemModelBuilder generated(String name, Identifier texture) {
        return getBuilder(name).parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", texture);
    }

    public ItemModelBuilder generated(Identifier itemId, Identifier texture) {
        Identifier modelId = Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
        ItemModelBuilder model = generated(modelId.toString(), texture);
        bindItem(itemId, model);
        return model;
    }

    public void bindItem(Identifier itemId, ModelFile model) {
        if (model instanceof ItemModelBuilder builder) {
            itemDefinitionBuilders.put(itemId, builder);
            itemDefinitions.remove(itemId);
        } else {
            JsonObject modelRef = new JsonObject();
            modelRef.addProperty("type", "minecraft:model");
            modelRef.addProperty("model", model.getLocation().toString());
            JsonObject definition = new JsonObject();
            definition.add("model", modelRef);
            bindItemDefinition(itemId, definition);
        }
    }

    public void bindItemDefinition(Identifier itemId, JsonElement definition) {
        itemDefinitionBuilders.remove(itemId);
        itemDefinitions.put(itemId, definition.deepCopy());
    }

    public void emitItemDefinitions(BiConsumer<Identifier, JsonElement> sink) {
        itemDefinitionBuilders.forEach((item, builder) -> sink.accept(
                Identifier.fromNamespaceAndPath(item.getNamespace(), "items/" + item.getPath() + ".json"),
                builder.toItemDefinition()));
        itemDefinitions.forEach((item, definition) -> sink.accept(
                Identifier.fromNamespaceAndPath(item.getNamespace(), "items/" + item.getPath() + ".json"),
                definition.deepCopy()));
    }

    @Override
    public void clear() {
        super.clear();
        itemDefinitions.clear();
        itemDefinitionBuilders.clear();
    }
}
