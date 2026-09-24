package com.gregtechceu.gtceu.client;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cosmetics.event.RegisterGTCapesEvent;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.client.color.item.GTMaterialPartTintSource;
import com.gregtechceu.gtceu.client.color.FluidCellTintSource;
import com.gregtechceu.gtceu.client.color.MaterialLayerTintSource;
import com.gregtechceu.gtceu.client.color.MachineItemTintSource;
import com.gregtechceu.gtceu.client.model.pipe.PipeModel;
import com.gregtechceu.gtceu.client.model.runtimegen.*;
import com.gregtechceu.gtceu.client.particle.GTParticleManager;
import com.gregtechceu.gtceu.client.particle.HazardParticle;
import com.gregtechceu.gtceu.client.particle.MufflerParticle;
import com.gregtechceu.gtceu.client.renderer.entity.GTBoatRenderer;
import com.gregtechceu.gtceu.client.renderer.entity.GTExplosiveRenderer;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTComponentItemDecorator;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTLampItemOverlayRenderer;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTTankItemFluidPreview;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTToolBarRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.MachineBlockEntityRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.impl.*;
import com.gregtechceu.gtceu.client.renderer.machine.impl.BoilerMultiPartRender;
import com.gregtechceu.gtceu.client.util.ModelEventHelper;
import com.gregtechceu.gtceu.common.CommonEventListener;
import com.gregtechceu.gtceu.common.CommonProxy;
import com.gregtechceu.gtceu.common.data.*;
import com.gregtechceu.gtceu.common.data.models.GTModels;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.entity.GTBoat;
import com.gregtechceu.gtceu.common.item.DrumMachineItem;
import com.gregtechceu.gtceu.common.item.LampBlockItem;
import com.gregtechceu.gtceu.common.item.armor.GTArmorItem;
import com.gregtechceu.gtceu.common.item.QuantumTankMachineItem;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTGuiTheme;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.model.builder.PipeModelBuilder;
import com.gregtechceu.gtceu.data.pack.event.RegisterDynamicResourcesEvent;
import com.gregtechceu.gtceu.integration.embeddium.GTEmbeddiumCompat;
import com.gregtechceu.gtceu.integration.kjs.GregTechKubeJSPlugin;
import com.gregtechceu.gtceu.integration.modernfix.GTModernFixIntegration;
import com.gregtechceu.gtceu.integration.map.ClientCacheManager;
import com.gregtechceu.gtceu.integration.map.cache.client.GTClientCache;
import com.gregtechceu.gtceu.integration.map.ftbchunks.FTBChunksPlugin;
import com.gregtechceu.gtceu.integration.map.layer.Layers;
import com.gregtechceu.gtceu.integration.map.layer.builtin.FluidRenderLayer;
import com.gregtechceu.gtceu.integration.map.layer.builtin.OreRenderLayer;
import com.gregtechceu.gtceu.utils.data.RuntimeBlockstateProvider;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMapping;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAt;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class ClientProxy extends CommonProxy {

    @Getter
    private static final DeltaTracker.Timer timer60Fps = new DeltaTracker.Timer(60f, 0, mspt -> mspt);

    public static final BiMap<Identifier, GTOreDefinition> CLIENT_ORE_VEINS = HashBiMap.create();
    public static final BiMap<Identifier, BedrockFluidDefinition> CLIENT_FLUID_VEINS = HashBiMap.create();
    public static final BiMap<Identifier, BedrockOreDefinition> CLIENT_BEDROCK_ORE_VEINS = HashBiMap.create();

    public ClientProxy() {
        super();
        init();
    }

    public static void init() {
        if (!GTCEu.isDataGen()) {

            ClientCacheManager.registerClientCache(GTClientCache.instance, "gtceu");
            Layers.registerLayer(OreRenderLayer::new, "ore_veins");
            Layers.registerLayer(FluidRenderLayer::new, "bedrock_fluids");
            CommonEventListener.registerCapes(new RegisterGTCapesEvent());

            if (GTCEu.Mods.isSodiumEmbeddiumLoaded()) {
                GTEmbeddiumCompat.init();
            }
        }
        initializeDynamicRenders();
        ModelEventHelper.initInternalAssetReloadListeners();

        NeoForge.EVENT_BUS.register(GTParticleManager.INSTANCE);
        NeoForge.EVENT_BUS.addListener(ClientProxy::onClientStarted);
        GTGuiTextures.init();
        ModLoadingContext.get().getActiveContainer().getEventBus().addListener(GTGuiTheme::onReloadThemes);
    }

    @SubscribeEvent
    public void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GTEntityTypes.DYNAMITE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(GTEntityTypes.POWDERBARREL.get(), GTExplosiveRenderer::new);
        event.registerEntityRenderer(GTEntityTypes.INDUSTRIAL_TNT.get(), GTExplosiveRenderer::new);

        event.registerBlockEntityRenderer(GTBlockEntities.GT_SIGN.get(), StandingSignRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.GT_HANGING_SIGN.get(), HangingSignRenderer::new);

        event.registerEntityRenderer(GTEntityTypes.BOAT.get(), c -> new GTBoatRenderer(c, false));
        event.registerEntityRenderer(GTEntityTypes.CHEST_BOAT.get(), c -> new GTBoatRenderer(c, true));

        Set<BlockEntityType<?>> registeredMachineTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var definition : GTRegistries.MACHINES) {
            BlockEntityType<? extends MetaMachine> type = definition.getBlockEntityType();
            if (registeredMachineTypes.add(type)) {
                registerMachineRenderer(event, type);
            }
        }
    }

    @SubscribeEvent
    public void onRegisterEntityLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (var type : GTBoat.BoatType.values()) {
            event.registerLayerDefinition(GTBoatRenderer.getBoatModelName(type), BoatModel::createBoatModel);
            event.registerLayerDefinition(GTBoatRenderer.getChestBoatModelName(type),
                    BoatModel::createChestBoatModel);
        }
    }

    @SubscribeEvent
    public void onRegisterDebugEntries(RegisterDebugEntriesEvent event) {
        GTParticleManager.INSTANCE.registerDebugEntries(event);

        Identifier machineDebugEntry = GTCEu.id("machine_debug_overlay");
        event.register(machineDebugEntry, (displayer, level, clientChunk, serverChunk) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.showOnlyReducedInfo()) return;

            Entity cameraEntity = mc.getCameraEntity();
            if (cameraEntity == null || mc.level == null) return;

            // Match vanilla's targeted-block debug entry and append GT's machine details to its group.
            if (!(cameraEntity.pick(20.0, 0.0F, false) instanceof BlockHitResult hit)) return;
            BlockEntity blockEntity = mc.level.getBlockEntity(hit.getBlockPos());
            if (!(blockEntity instanceof MetaMachine machine)) return;

            List<String> lines = new ArrayList<>();
            lines.add("");
            machine.addDebugOverlayText(lines::add);
            displayer.addToGroup(DebugEntryLookingAt.BLOCK_GROUP, lines);
        });
        event.includeInProfile(machineDebugEntry, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.ALWAYS_ON);
    }

    @SubscribeEvent
    public void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof IComponentItem) {
                event.register(item, GTComponentItemDecorator.INSTANCE);
            }
            if (item instanceof IGTTool) {
                event.register(item, GTToolBarRenderer.INSTANCE);
            }
            if (item instanceof LampBlockItem) {
                event.register(item, GTLampItemOverlayRenderer.INSTANCE);
            }
            if (item instanceof DrumMachineItem) {
                event.register(item, GTTankItemFluidPreview.DRUM);
            }
            if (item instanceof QuantumTankMachineItem) {
                event.register(item, GTTankItemFluidPreview.QUANTUM_TANK);
            }
        }
    }

    @SubscribeEvent
    public void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(FluidCellTintSource.ID, FluidCellTintSource.MAP_CODEC);
        event.register(GTMaterialPartTintSource.ID, GTMaterialPartTintSource.MAP_CODEC);
        event.register(MaterialLayerTintSource.ID, MaterialLayerTintSource.MAP_CODEC);
        event.register(MachineItemTintSource.ID, MachineItemTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    public void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ArmorComponentItem || item instanceof GTArmorItem) {
                event.registerItem(GTArmorClientExtensions.INSTANCE, item);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onModernFixModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (GTCEu.Mods.isModernFixLoaded()) {
            GTModernFixIntegration.onModifyBakingResult(event);
        }
    }

    @SubscribeEvent
    public void registerKeyBindings(RegisterKeyMappingsEvent event) {
        SyncedKeyMapping.onRegisterKeyBinds(event);
    }

    @SubscribeEvent
    public void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GTCEu.id("hud"), new HudGuiOverlay());
    }

    @SubscribeEvent
    public void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GTParticleTypes.HAZARD_PARTICLE.get(), HazardParticle.Provider::new);
        event.registerSpriteSet(GTParticleTypes.MUFFLER_PARTICLE.get(), MufflerParticle.Provider::new);
    }

    @SubscribeEvent
    private static void onClientStarted(ClientStartedEvent event) {
        MachineOwner.init();
        if (ConfigHolder.INSTANCE.compat.minimap.toggle.ftbChunksIntegration &&
                GTCEu.isModLoaded(GTValues.MODID_FTB_CHUNKS)) {
            FTBChunksPlugin.addEventListeners();
        }
    }

    private static void initializeDynamicRenders() {
        DynamicRenderManager.register(GTCEu.id("quantum_tank_fluid"), QuantumTankFluidRender.TYPE);
        DynamicRenderManager.register(GTCEu.id("quantum_chest_item"), QuantumChestItemRender.TYPE);

        DynamicRenderManager.register(GTCEu.id("fusion_ring"), FusionRingRender.TYPE);
        DynamicRenderManager.register(GTCEu.id("boiler_multi_parts"), BoilerMultiPartRender.TYPE);
        DynamicRenderManager.register(GTCEu.id("assembly_line"), AssemblyLineRender.TYPE);

        DynamicRenderManager.register(GTCEu.id("fluid_area"), FluidAreaRender.TYPE);
        DynamicRenderManager.register(GTCEu.id("growing_plant"), GrowingPlantRender.TYPE);

        DynamicRenderManager.register(GTCEu.id("central_monitor"), CentralMonitorRender.TYPE);
    }

    private static <T extends MetaMachine> void registerMachineRenderer(EntityRenderersEvent.RegisterRenderers event,
                                                                        BlockEntityType<T> type) {
        event.registerBlockEntityRenderer(type, MachineBlockEntityRenderer::new);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void preRegisterDynamicAssets(RegisterDynamicResourcesEvent event) {
        PipeModel.DYNAMIC_MODELS.clear();
    }

    @SubscribeEvent
    public void registerDynamicAssets(RegisterDynamicResourcesEvent event) {
        // regenerate all pipe models in case their textures changed
        // cables may do this, others too if something's removed
        for (var block : GTMaterialBlocks.CABLE_BLOCKS.values()) {
            if (block == null) continue;
            block.get().createPipeModel(RuntimeBlockstateProvider.INSTANCE).dynamicModel();
        }
        for (var block : GTMaterialBlocks.FLUID_PIPE_BLOCKS.values()) {
            if (block == null) continue;
            block.get().createPipeModel(RuntimeBlockstateProvider.INSTANCE).dynamicModel();
        }
        for (var block : GTMaterialBlocks.ITEM_PIPE_BLOCKS.values()) {
            if (block == null) continue;
            block.get().createPipeModel(RuntimeBlockstateProvider.INSTANCE).dynamicModel();
        }

        MaterialBlockModelGenerator.reinitModels();
        TagPrefixItemModelGenerator.reinitModels();
        OreBlockModelGenerator.reinitModels();
        ToolItemModelGenerator.reinitModels();
        ArmorItemModelGenerator.reinitModels();
        SurfaceRockModelGenerator.reinitModels();
        GTModels.registerMaterialFluidModels();
        GTModels.registerRuntimeTintedItemModels();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void postRegisterDynamicAssets(RegisterDynamicResourcesEvent event) {
        // do this last so addons can easily add new variants to the registered model set
        PipeModel.initDynamicModels();

        if (GTCEu.Mods.isKubeJSLoaded()) {
            GregTechKubeJSPlugin.generateMachineBlockModels();
        }
        RuntimeBlockstateProvider.INSTANCE.run();
        PipeModelBuilder.clearRestrictorModelCache();
    }
}
