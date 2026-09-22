package com.gregtechceu.gtceu.utils.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.common.registry.GTRegistration;
import com.gregtechceu.gtceu.data.model.builder.ModelBuilder;
import com.gregtechceu.gtceu.data.model.builder.ModelProvider;
import com.gregtechceu.gtceu.data.model.builder.RuntimeModelResources;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;

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

    // Runtime callers enqueue models directly, rather than replaying build-time Registrate callbacks.
    @Override
    protected void registerStatesAndModels() {}

    public void run() {
        withCurrentProvider(() -> emitResources(consumer));
        // Keep the queue on failure, allowing a caller to fix a resource/sink and retry.
        clearGenerated();
    }

    public <T extends ModelBuilder<T>> void processModelProvider(ModelProvider<T> provider) {
        for (T model : provider.generatedModels.values()) {
            consumer.accept(RuntimeModelResources.modelPath(model.getLocation()), model.toJson());
        }
    }
}
