package com.gregtechceu.gtceu.common.data.models;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.*;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorage;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.client.color.MaterialLayerTintSource;
import com.gregtechceu.gtceu.common.block.*;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.core.MixinHelpers;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.gregtechceu.gtceu.data.model.builder.ConfiguredModel;
import com.gregtechceu.gtceu.data.model.builder.ItemModelProvider;
import com.gregtechceu.gtceu.data.model.builder.ModelFile;
import com.gregtechceu.gtceu.data.model.builder.RuntimeModelResources;
import com.gregtechceu.gtceu.utils.data.RuntimeBlockstateProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class GTModels {

    public static final Identifier BLANK_TEXTURE = GTCEu.id("block/void");

    public static final String ACTIVE_SUFFIX = "_active";

    // region BLOCK MODELS

    public static NonNullBiConsumer<DataGenContext<Block, ? extends Block>, GTBlockstateProvider> createModelBlockState(Identifier modelLocation) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(), prov.models().getExistingFile(modelLocation));
        };
    }

    public static void createCrossBlockState(DataGenContext<Block, ? extends Block> ctx,
                                             GTBlockstateProvider prov) {
        prov.simpleBlock(ctx.getEntry(), prov.models().cross(ctx.getName(), prov.blockTexture(ctx.getEntry())));
    }

    /**
     * Creates an item definition with a modern range-dispatch model. The predicate id must identify a
     * registered {@code RangeSelectItemModelProperty}; item-model property registration is supplied by the consumer.
     */
    public static <
            T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, ItemModelProvider> overrideModel(Identifier predicate,
                                                                                                                  int modelNumber) {
        if (modelNumber <= 0) return NonNullBiConsumer.noop();
        return (ctx, prov) -> {
            String modelRoot = "item/%s/".formatted(ctx.getName());
            ModelFile fallbackModel = prov.generated(ctx.getId(), prov.modLoc(modelRoot + "1"));
            Identifier fallback = fallbackModel.getLocation();

            JsonArray entries = new JsonArray();
            for (int i = 0; i < modelNumber; i++) {
                String modelPath = modelRoot + i;
                prov.generated(modelPath, prov.modLoc("item/%s/%d".formatted(ctx.getName(), i + 1)));

                JsonObject entry = new JsonObject();
                entry.addProperty("threshold", i / 100f);
                entry.add("model", itemModelReference(prov.modLoc(modelPath)));
                entries.add(entry);
            }

            JsonObject rangeDispatch = new JsonObject();
            rangeDispatch.addProperty("type", "minecraft:range_dispatch");
            rangeDispatch.addProperty("property", predicate.toString());
            rangeDispatch.addProperty("scale", 1.0f);
            rangeDispatch.add("entries", entries);
            rangeDispatch.add("fallback", itemModelReference(fallback));

            JsonObject definition = new JsonObject();
            definition.add("model", rangeDispatch);
            prov.bindItemDefinition(ctx.getId(), definition);
        };
    }

    public static void createTextureModel(DataGenContext<Item, ? extends Item> ctx, ItemModelProvider prov,
                                          Identifier texture) {
        prov.generated(ctx.getId(), texture);
    }

    /** Bind the modern NeoForge dynamic fluid-container item model for the registered container. */
    public static void createFluidContainerItemDefinition(DataGenContext<Item, ? extends Item> ctx,
                                                          ItemModelProvider prov, boolean hasCover) {
        prov.bindItemDefinition(ctx.getId(), fluidContainerItemDefinition(ctx.getName(), hasCover));
    }

    private static JsonObject fluidContainerItemDefinition(String itemPath, boolean hasCover) {
        JsonObject textures = new JsonObject();
        textures.addProperty("base", GTCEu.id("item/" + itemPath + "/base").toString());
        textures.addProperty("fluid", GTCEu.id("item/" + itemPath + "/overlay").toString());
        if (hasCover) {
            textures.addProperty("cover", GTCEu.id("item/" + itemPath + "/base").toString());
        }

        JsonObject model = new JsonObject();
        model.addProperty("type", "neoforge:fluid_container");
        model.add("textures", textures);
        model.addProperty("fluid", "minecraft:empty");

        JsonObject definition = new JsonObject();
        definition.add("model", model);
        return definition;
    }

    /** Generate the rotor's texture model and bind its 26.2 material tint source in the item definition. */
    public static void createMaterialPartItemModel(DataGenContext<Item, ? extends Item> ctx,
                                                   ItemModelProvider prov, Identifier texture) {
        prov.generated(ctx.getId(), texture);
        prov.bindItemDefinition(ctx.getId(), materialPartItemDefinition(ctx.getId().withPrefix("item/")));
    }

    private static JsonObject materialPartItemDefinition(Identifier modelId) {
        JsonArray tints = new JsonArray();
        JsonObject materialPartTint = new JsonObject();
        materialPartTint.addProperty("type", "gtceu:material_part");
        tints.add(materialPartTint);
        return RuntimeModelResources.itemDefinition(modelId, tints);
    }

    /** Keep the generated block-item model as the base so its display transforms stay active. */
    public static void createLampItemDefinition(DataGenContext<Item, ? extends Item> ctx,
                                                ItemModelProvider prov) {
        JsonObject specialModel = new JsonObject();
        specialModel.addProperty("type", "minecraft:special");
        specialModel.addProperty("base", ctx.getId().withPrefix("item/").toString());

        JsonObject renderer = new JsonObject();
        renderer.addProperty("type", GTCEu.id("lamp").toString());
        specialModel.add("model", renderer);

        JsonObject definition = new JsonObject();
        definition.add("model", specialModel);
        prov.bindItemDefinition(ctx.getId(), definition);
    }

    /** Add item definitions and their generated model to the live dynamic resource pack. */
    public static void registerRuntimeTintedItemModels() {
        ItemModelProvider itemModels = RuntimeBlockstateProvider.INSTANCE.itemModels();
        for (Item item : List.of(GTItems.FLUID_CELL.get(), GTItems.FLUID_CELL_UNIVERSAL.get(),
                GTItems.FLUID_CELL_GLASS_VIAL.get(), GTItems.FLUID_CELL_LARGE_STEEL.get(),
                GTItems.FLUID_CELL_LARGE_ALUMINIUM.get(), GTItems.FLUID_CELL_LARGE_STAINLESS_STEEL.get(),
                GTItems.FLUID_CELL_LARGE_TITANIUM.get(), GTItems.FLUID_CELL_LARGE_TUNGSTEN_STEEL.get())) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            boolean hasCover = item == GTItems.FLUID_CELL_GLASS_VIAL.get();
            itemModels.bindItemDefinition(itemId,
                    fluidContainerItemDefinition(itemId.getPath(), hasCover));
        }

        Item rotor = GTItems.TURBINE_ROTOR.get();
        Identifier rotorId = BuiltInRegistries.ITEM.getKey(rotor);
        itemModels.generated(rotorId, GTCEu.id("item/tools/turbine"));
        itemModels.bindItemDefinition(rotorId, materialPartItemDefinition(rotorId.withPrefix("item/")));

        // ToolItemModelGenerator registers the base item definitions first. Re-emit them here with
        // stack-aware model tints so material and crowbar colors remain dynamic in the 26.2 item pipeline.
        for (var cell : GTMaterialItems.TOOL_ITEMS.cellSet()) {
            var toolEntry = cell.getValue();
            if (toolEntry == null) continue;

            Item tool = toolEntry.get();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(tool);
            RuntimeModelResources.emitItem(itemId, cell.getColumnKey().modelLocation,
                    RuntimeModelResources.dynamicLayerTints(MaterialLayerTintSource.ID, 3),
                    GTDynamicResourcePack::addResource);
        }

        registerMaterialPipeItemTints(itemModels, GTMaterialBlocks.CABLE_BLOCKS.values());
        registerMaterialPipeItemTints(itemModels, GTMaterialBlocks.FLUID_PIPE_BLOCKS.values());
        registerMaterialPipeItemTints(itemModels, GTMaterialBlocks.ITEM_PIPE_BLOCKS.values());
    }

    private static void registerMaterialPipeItemTints(ItemModelProvider itemModels,
                                                       Iterable<? extends com.tterrag.registrate.util.entry.BlockEntry<?>> entries) {
        for (var entry : entries) {
            if (entry == null) continue;
            Identifier itemId = BuiltInRegistries.ITEM.getKey(entry.get().asItem());
            itemModels.bindItemDefinition(itemId, RuntimeModelResources.itemDefinition(
                    itemId.withPrefix("item/"), RuntimeModelResources.dynamicLayerTints(MaterialLayerTintSource.ID, 10)));
        }
    }

    public static void rubberTreeSaplingModel(DataGenContext<Item, BlockItem> context,
                                              ItemModelProvider provider) {
        provider.generated(context.getId(), provider.modLoc("block/" + context.getName()));
    }

    private static JsonObject itemModelReference(Identifier modelId) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelId.toString());
        return model;
    }

    public static final Identifier CUBE_ALL_EMISSIVE = GTCEu.id("block/cube/emissive/all");

    public static NonNullBiConsumer<DataGenContext<Block, LampBlock>, GTBlockstateProvider> lampModel(DyeColor color,
                                                                                                              boolean border) {
        return (ctx, prov) -> {
            final String textureBase = "block/lamps/" + color.getSerializedName() + (border ? "" : "_borderless");

            var onModel = prov.models().withExistingParent(ctx.getName() + "_on", CUBE_ALL_EMISSIVE)
                    .texture("all", prov.modLoc(textureBase + "_on"));
            var offModel = prov.models().cubeAll(ctx.getName() + "_off", prov.modLoc(textureBase + "_off"));

            var bloomModel = prov.models().withExistingParent(ctx.getName() + "_bloom",
                    border ? GTCEu.id("block/cube_2_layer/all") : CUBE_ALL_EMISSIVE);
            if (border) {
                bloomModel.texture("bot_all", textureBase + "_on");
                bloomModel.texture("top_all", textureBase + "_bloom");
            } else {
                bloomModel.texture("all", textureBase + "_bloom");
            }

            prov.getVariantBuilder(ctx.getEntry())
                    // spotless:off
                    .partialState()
                        .with(LampBlock.INVERTED, false).with(LampBlock.POWERED, false)
                        .modelForState().modelFile(offModel)
                        .addModel()
                    .partialState()
                        .with(LampBlock.INVERTED, true).with(LampBlock.POWERED, true)
                        .modelForState().modelFile(offModel)
                        .addModel()
                    .partialState()
                        .with(LampBlock.INVERTED, false).with(LampBlock.POWERED, true)
                        .with(LampBlock.BLOOM, false)
                        .modelForState().modelFile(onModel)
                        .addModel()
                    .partialState()
                        .with(LampBlock.INVERTED, true).with(LampBlock.POWERED, false)
                        .with(LampBlock.BLOOM, false)
                        .modelForState().modelFile(onModel)
                        .addModel()
                    .partialState()
                        .with(LampBlock.INVERTED, false).with(LampBlock.POWERED, true)
                        .with(LampBlock.BLOOM, true)
                        .modelForState().modelFile(bloomModel)
                        .addModel()
                    .partialState()
                        .with(LampBlock.INVERTED, true).with(LampBlock.POWERED, false)
                        .with(LampBlock.BLOOM, true)
                        .modelForState().modelFile(bloomModel)
                        .addModel();
                    // spotless:on
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, Block>, GTBlockstateProvider> randomRotatedModel(Identifier texturePath) {
        return (ctx, prov) -> {
            Block block = ctx.getEntry();
            ModelFile cubeAll = prov.models().cubeAll(ctx.getName(), texturePath);
            ModelFile cubeMirroredAll = prov.models().singleTexture(ctx.getName() + "_mirrored",
                    prov.mcLoc("block/cube_mirrored_all"), "all", texturePath);
            ConfiguredModel[] models = ConfiguredModel.builder()
                    .modelFile(cubeAll)
                    .rotationY(0)
                    .nextModel()
                    .modelFile(cubeAll)
                    .rotationY(180)
                    .nextModel()
                    .modelFile(cubeMirroredAll)
                    .rotationY(0)
                    .nextModel()
                    .modelFile(cubeMirroredAll)
                    .rotationY(180)
                    .build();
            prov.simpleBlock(block, models);
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, Block>, GTBlockstateProvider> createSidedCasingModel(Identifier texture) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(), prov.models().cubeBottomTop(ctx.getName(),
                    texture.withSuffix("/side"),
                    texture.withSuffix("/bottom"),
                    texture.withSuffix("/top")));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, ? extends Block>, GTBlockstateProvider> cubeAllModel(Identifier texture) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), texture));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, Block>, GTBlockstateProvider> createMachineCasingModel(String tierName) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(),
                    prov.models()
                            .withExistingParent("%s_machine_casing".formatted(tierName),
                                    GTCEu.id("block/cube/tinted/bottom_top"))
                            .texture("bottom", GTCEu.id("block/casings/voltage/%s/bottom".formatted(tierName)))
                            .texture("top", GTCEu.id("block/casings/voltage/%s/top".formatted(tierName)))
                            .texture("side", GTCEu.id("block/casings/voltage/%s/side".formatted(tierName))));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, Block>, GTBlockstateProvider> createHermeticCasingModel(String tierName) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(), prov.models()
                    .withExistingParent("%s_hermetic_casing".formatted(tierName), GTCEu.id("block/hermetic_casing"))
                    .texture("bot_bottom", GTCEu.id("block/casings/voltage/%s/bottom".formatted(tierName)))
                    .texture("bot_top", GTCEu.id("block/casings/voltage/%s/top".formatted(tierName)))
                    .texture("bot_side", GTCEu.id("block/casings/voltage/%s/side".formatted(tierName))));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, Block>, GTBlockstateProvider> createSteamCasingModel(String material) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(), prov.models().cubeBottomTop(ctx.getName(),
                    GTCEu.id("block/casings/steam/%s/side".formatted(material)),
                    GTCEu.id("block/casings/steam/%s/bottom".formatted(material)),
                    GTCEu.id("block/casings/steam/%s/top".formatted(material))));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, CoilBlock>, GTBlockstateProvider> createCoilModel(ICoilType coilType) {
        return (ctx, prov) -> {
            String name = ctx.getName();
            ActiveBlock block = ctx.getEntry();
            ModelFile inactive = prov.models().cubeAll(name, coilType.getTexture());
            ModelFile active = prov.models().withExistingParent(name + "_active", GTCEu.id("block/cube_2_layer/all"))
                    .texture("bot_all", coilType.getTexture())
                    .texture("top_all", coilType.getTexture().withSuffix("_bloom"));
            prov.getVariantBuilder(block)
                    .partialState().with(GTBlockStateProperties.ACTIVE, false).modelForState().modelFile(inactive)
                    .addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true).modelForState().modelFile(active)
                    .addModel();
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, BatteryBlock>, GTBlockstateProvider> createBatteryBlockModel(IBatteryData batteryData) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(), prov.models().cubeBottomTop(ctx.getName(),
                    GTCEu.id("block/casings/battery/" + batteryData.getBatteryName() + "/side"),
                    GTCEu.id("block/casings/battery/" + batteryData.getBatteryName() + "/top"),
                    GTCEu.id("block/casings/battery/" + batteryData.getBatteryName() + "/top")));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, FusionCasingBlock>, GTBlockstateProvider> createFusionCasingModel(IFusionCasingType casingType) {
        return (ctx, prov) -> {
            String name = ctx.getName();
            ActiveBlock block = ctx.getEntry();
            ModelFile inactive = prov.models().cubeAll(name, casingType.getTexture());
            ModelFile active = prov.models().withExistingParent(name + "_active", GTCEu.id("block/cube_2_layer/all"))
                    .texture("bot_all", casingType.getTexture())
                    .texture("top_all", casingType.getTexture().withSuffix("_bloom"));
            prov.getVariantBuilder(block)
                    .partialState().with(GTBlockStateProperties.ACTIVE, false).modelForState().modelFile(inactive)
                    .addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true).modelForState().modelFile(active)
                    .addModel();
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, Block>, GTBlockstateProvider> createCleanroomFilterModel(IFilterType type) {
        return (ctx, prov) -> {
            prov.simpleBlock(ctx.getEntry(), prov.models()
                    .cubeAll(ctx.getName(), GTCEu.id("block/casings/cleanroom/" + type.getSerializedName())));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, ActiveBlock>, GTBlockstateProvider> createActiveModel(Identifier modelPath) {
        return (ctx, prov) -> {
            ActiveBlock block = ctx.getEntry();
            ModelFile inactive = prov.models().getExistingFile(modelPath);
            ModelFile active = prov.models().getExistingFile(modelPath.withSuffix("_active"));
            prov.getVariantBuilder(block)
                    .partialState().with(GTBlockStateProperties.ACTIVE, false).modelForState().modelFile(inactive)
                    .addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true).modelForState().modelFile(active)
                    .addModel();
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, ActiveBlock>, GTBlockstateProvider> createFireboxModel(BoilerFireboxType type) {
        return (ctx, prov) -> {
            String name = ctx.getName();
            ActiveBlock block = ctx.getEntry();
            ModelFile inactive = prov.models().cubeBottomTop(name, type.side(), type.bottom(), type.top());
            ModelFile active = prov.models().withExistingParent(name + "_active", GTCEu.id("block/fire_box_active"))
                    .texture("side", type.side())
                    .texture("bottom", type.bottom())
                    .texture("top", type.top());
            prov.getVariantBuilder(block)
                    .partialState().with(GTBlockStateProperties.ACTIVE, false)
                    .modelForState().modelFile(inactive).addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true)
                    .modelForState().modelFile(active).addModel();
        };
    }

    public static void createPipeBlockModel(DataGenContext<Block, ? extends PipeBlock<?, ?, ?>> ctx,
                                            GTBlockstateProvider prov) {
        // the pipe model generator handles adding its models to the provider by itself
        ctx.getEntry().createPipeModel(prov).initModels();
    }

    // endregion

    // region RUNTIME GEN

    /**
     * register fluid models for materials
     */
    public static void registerMaterialFluidModels() {
        for (var material : GTRegistries.MATERIALS) {
            var fluidProperty = material.getProperty(PropertyKey.FLUID);
            if (fluidProperty == null) continue;
            MaterialIconSet iconSet = material.getMaterialIconSet();

            for (FluidStorageKey key : FluidStorageKey.allKeys()) {
                FluidStorage storage = fluidProperty.getStorage();
                // fluid block models.
                FluidStorage.FluidEntry fluidEntry = storage.getEntry(key);
                if (fluidEntry != null && fluidEntry.getBuilder() != null) {
                    if (fluidEntry.getBuilder().still() == null) {
                        Identifier foundTexture = key.getIconType().getBlockTexturePath(iconSet, false);
                        fluidEntry.getBuilder().still(foundTexture);
                    }
                    if (fluidEntry.getBuilder().flowing() == null) {
                        fluidEntry.getBuilder().flowing(fluidEntry.getBuilder().still());
                    }
                    MixinHelpers.addFluidTexture(material, fluidEntry);
                }

                // bucket models.
                Fluid fluid = storage.get(key);
                if (fluid instanceof GTFluid gtFluid) {
                    // read the base bucket model JSON
                    JsonObject original;
                    try (BufferedReader reader = Minecraft.getInstance().getResourceManager()
                            .openAsReader(GTCEu.id("models/item/bucket/bucket.json"))) {
                        original = GsonHelper.parse(reader);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    JsonObject newJson = original.deepCopy();
                    newJson.addProperty("fluid", BuiltInRegistries.FLUID.getKey(gtFluid).toString());
                    if (gtFluid.getFluidType().isLighterThanAir()) {
                        newJson.addProperty("flip_gas", true);
                    }
                    if (gtFluid.getFluidType().getLightLevel() > 0) {
                        newJson.addProperty("apply_fluid_luminosity", true);
                    }

                    Identifier bucketId = BuiltInRegistries.ITEM.getKey(gtFluid.getBucket());
                    JsonObject itemDefinition = new JsonObject();
                    itemDefinition.add("model", newJson);
                    RuntimeModelResources.emitItem(bucketId, itemDefinition, GTDynamicResourcePack::addResource);
                }
            }
        }
    }

    // endregion
}
