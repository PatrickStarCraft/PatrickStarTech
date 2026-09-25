package com.gregtechceu.gtceu.utils.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.common.data.models.GTModels;
import com.gregtechceu.gtceu.common.registry.GTRegistration;
import com.gregtechceu.gtceu.data.model.builder.ModelBuilder;
import com.gregtechceu.gtceu.data.model.builder.ModelProvider;
import com.gregtechceu.gtceu.data.model.builder.RuntimeModelResources;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import com.google.gson.JsonElement;
import com.tterrag.registrate.AbstractRegistrate;

import java.util.function.BiConsumer;

/** The same GT generators as datagen, writing complete resource paths into the runtime pack. */
public class RuntimeBlockstateProvider extends GTBlockstateProvider {

    public static final RuntimeBlockstateProvider INSTANCE = new RuntimeBlockstateProvider(
            GTRegistration.REGISTRATE, new PackOutput(GTCEu.GTCEU_FOLDER), GTDynamicResourcePack::addResource);

    protected final BiConsumer<Identifier, JsonElement> consumer;

    public RuntimeBlockstateProvider(AbstractRegistrate<?> parent, PackOutput packOutput,
                                     BiConsumer<Identifier, JsonElement> consumer) {
        super(parent, packOutput, RuntimeExistingFileHelper.INSTANCE);
        this.consumer = consumer;
    }

    // Runtime callers enqueue block models directly; item definitions still come from Registrate callbacks.
    @Override
    protected void registerStatesAndModels() {
        registerItemModels();
    }

    public void run() {
        withCurrentProvider(() -> {
            registerStatesAndModels();
            // Reapply specialized runtime definitions after ordinary Registrate item callbacks.
            GTModels.registerRuntimeTintedItemModels();
            registerLegacyItemModelDefinitions();
            emitResources(consumer);
        });
        // Keep the queue on failure, allowing a caller to fix a resource/sink and retry.
        clearGenerated();
    }

    private void registerLegacyItemModelDefinitions() {
        var itemModels = itemModels();
        var activeFileHelper = RuntimeExistingFileHelper.INSTANCE.activeHelper();
        for (var item : BuiltInRegistries.ITEM) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            if (!GTCEu.MOD_ID.equals(itemId.getNamespace()) || itemModels.hasItemDefinition(itemId) ||
                    GTDynamicResourcePack.hasResource(RuntimeModelResources.itemDefinitionPath(itemId))) continue;

            Identifier modelId = itemId.withPrefix("item/");
            var generatedItemModel = itemModels.generatedModels.get(modelId);
            if (generatedItemModel != null) {
                itemModels.bindItem(itemId, generatedItemModel);
                continue;
            }
            if (activeFileHelper.exists(modelId, MODEL)) {
                itemModels.bindItemDefinition(itemId, RuntimeModelResources.itemDefinition(modelId));
            }
        }
    }

    public <T extends ModelBuilder<T>> void processModelProvider(ModelProvider<T> provider) {
        for (T model : provider.generatedModels.values()) {
            consumer.accept(RuntimeModelResources.modelPath(model.getLocation()), model.toJson());
        }
    }
}
