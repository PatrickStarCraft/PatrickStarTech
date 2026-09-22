package com.gregtechceu.gtceu.api.registry.registrate.provider;

import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.client.util.ExtendedBlockModelRotation;
import com.gregtechceu.gtceu.data.model.builder.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;

import com.google.gson.JsonElement;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.GeneratorType;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * GT model output, shared by datagen and the runtime resource pack. Custom loader payloads retain
 * {@code gtceu:z}; ordinary blockstates use 26.2's native {@code z} rotation.
 * Registrate callbacks must use {@link #BLOCKSTATE} and {@link #ITEM_MODEL}, not its vanilla generators.
 */
public class GTBlockstateProvider implements RegistrateProvider {

    public static final String Z_ROT_PROPERTY_NAME = "gtceu:z";
    public static final ModelFileHelper.ResourceType TEXTURE = ModelFileHelper.ResourceType.TEXTURE;
    public static final ModelFileHelper.ResourceType MODEL = ModelFileHelper.ResourceType.MODEL;
    public static final ProviderType<GTBlockstateProvider> TYPE = ProviderType.registerClientProvider(
            "gt_model_generation", () -> GTBlockstateProvider::new);
    public static final GeneratorType<GTBlockstateProvider> BLOCKSTATE = TYPE.createGenerator("gt_blockstate");
    public static final GeneratorType<ItemModelProvider> ITEM_MODEL = TYPE.createGenerator("gt_item_model");

    private static final ThreadLocal<GTBlockstateProvider> CURRENT_PROVIDER = new ThreadLocal<>();
    private final AbstractRegistrate<?> parent;
    private final PackOutput output;
    private final BlockModelProvider blockModels;
    private final ItemModelProvider itemModels;
    protected final Map<Block, Supplier<? extends JsonElement>> registeredBlocks = new LinkedHashMap<>();
    private final Map<Block, VariantBlockStateBuilder> variants = new LinkedHashMap<>();
    private final Map<Block, MultiPartBlockStateBuilder> multipart = new LinkedHashMap<>();

    public GTBlockstateProvider(ProviderType.Context<GTBlockstateProvider> context) {
        this(context.parent(), context.output(), fileHelper(context.event().getResourceManager(PackType.CLIENT_RESOURCES)));
    }

    public GTBlockstateProvider(AbstractRegistrate<?> parent, PackOutput output, ModelFileHelper helper) {
        this.parent = parent;
        this.output = output;
        this.blockModels = new BlockModelProvider(parent.getModid(), helper);
        this.itemModels = new ItemModelProvider(parent.getModid(), helper);
    }

    private static ModelFileHelper fileHelper(ResourceManager resources) {
        return new ModelFileHelper() {
            private final Set<Identifier> generated = new HashSet<>();

            private Identifier path(Identifier id, ResourceType type) {
                return type == ResourceType.MODEL ? id.withPrefix("models/").withSuffix(".json") :
                        id.withPrefix("textures/").withSuffix(".png");
            }

            @Override
            public boolean exists(Identifier id, ResourceType type) {
                Identifier path = path(id, type);
                return generated.contains(path) || resources.getResource(path).isPresent();
            }

            @Override
            public void trackGenerated(Identifier id, ResourceType type) {
                generated.add(path(id, type));
            }
        };
    }

    public static GTBlockstateProvider getCurrentProvider() {
        return CURRENT_PROVIDER.get();
    }

    protected final void withCurrentProvider(Runnable action) {
        GTBlockstateProvider previous = CURRENT_PROVIDER.get();
        CURRENT_PROVIDER.set(this);
        try {
            action.run();
        } finally {
            if (previous == null) CURRENT_PROVIDER.remove();
            else CURRENT_PROVIDER.set(previous);
        }
    }

    protected void registerStatesAndModels() {
        parent.genData(BLOCKSTATE, this);
        parent.genData(ITEM_MODEL, itemModels);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var saves = new ArrayList<CompletableFuture<?>>();
        withCurrentProvider(() -> {
            clearGenerated();
            registerStatesAndModels();
            emitResources((id, json) -> saves.add(DataProvider.saveStable(cache, json,
                    output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(id.getNamespace()).resolve(id.getPath()))));
        });
        return CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new));
    }

    /** Emits complete resource paths under assets/&lt;namespace&gt;, including modern item definitions. */
    protected final void emitResources(BiConsumer<Identifier, JsonElement> sink) {
        blockModels.generatedModels.values().forEach(model ->
                sink.accept(RuntimeModelResources.modelPath(model.getLocation()), model.toJson()));
        itemModels.generatedModels.values().forEach(model ->
                sink.accept(RuntimeModelResources.modelPath(model.getLocation()), model.toJson()));
        itemModels.emitItemDefinitions(sink);
        registeredBlocks.forEach((block, state) -> sink.accept(
                RuntimeModelResources.blockStatePath(BuiltInRegistries.BLOCK.getKey(block)), state.get()));
    }

    protected final void clearGenerated() {
        blockModels.generatedModels.clear();
        itemModels.clear();
        registeredBlocks.clear();
        variants.clear();
        multipart.clear();
    }

    public BlockModelProvider models() { return blockModels; }
    public ItemModelProvider itemModels() { return itemModels; }
    public ModelFileHelper getExistingFileHelper() { return blockModels.getExistingFileHelper(); }
    public Identifier modLoc(String path) { return Identifier.fromNamespaceAndPath(parent.getModid(), path); }
    public Identifier mcLoc(String path) { return Identifier.withDefaultNamespace(path); }
    public Identifier blockTexture(Block block) { return BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/"); }

    public VariantBlockStateBuilder getVariantBuilder(Block block) {
        if (multipart.containsKey(block)) throw new IllegalStateException("Block already has multipart output: " + block);
        return variants.computeIfAbsent(block, key -> {
            var builder = new VariantBlockStateBuilder(key);
            registeredBlocks.put(key, builder::toJson);
            return builder;
        });
    }

    public MultiPartBlockStateBuilder getMultipartBuilder(Block block) {
        if (variants.containsKey(block)) throw new IllegalStateException("Block already has variant output: " + block);
        return multipart.computeIfAbsent(block, key -> {
            var builder = new MultiPartBlockStateBuilder(key);
            registeredBlocks.put(key, builder::toJson);
            return builder;
        });
    }

    public void simpleBlock(Block block, ModelFile model) {
        simpleBlock(block, ConfiguredModel.builder().modelFile(model).build());
    }

    public void simpleBlock(Block block, ConfiguredModel... models) {
        getVariantBuilder(block).partialState().setModels(models);
    }

    public void simpleBlockWithFacing(Block block, ModelFile model, MachineDefinition definition) {
        RotationState rotation = definition.getRotationState();
        if (rotation == RotationState.NONE) {
            simpleBlock(block, model);
            return;
        }
        var builder = getVariantBuilder(block);
        for (var front : rotation.property.getPossibleValues()) {
            if (definition.isAllowExtendedFacing()) {
                for (var up : GTBlockStateProperties.UPWARDS_FACING.getPossibleValues()) {
                    builder.partialState().with(rotation.property, front).with(GTBlockStateProperties.UPWARDS_FACING, up)
                            .setModels(oriented(model, ExtendedBlockModelRotation.getExtended(front, up)));
                }
            } else {
                builder.partialState().with(rotation.property, front)
                        .setModels(oriented(model, ExtendedBlockModelRotation.get(front)));
            }
        }
    }

    private static ConfiguredModel[] oriented(ModelFile model, ExtendedBlockModelRotation rotation) {
        return ConfiguredModel.builder().modelFile(model).rotationX(rotation.getAngleX())
                .rotationY(rotation.getAngleY()).rotationZ(rotation.getAngleZ()).build();
    }

    @Override
    public String getName() { return "GT models and blockstates: " + parent.getModid(); }
}
