package com.gregtechceu.gtceu.client.util;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.model.ctm.CTMBakedModel;
import com.gregtechceu.gtceu.client.model.ctm.CTMModelPartSource;
import com.gregtechceu.gtceu.client.model.ManuallyConnectedTextureModel;
import com.gregtechceu.gtceu.integration.modernfix.GTModernFixIntegration;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("deprecation")
@UtilityClass
@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public class ModelEventHelper {

    @ApiStatus.Internal
    public record EventListenerHolder<T>(T listener, boolean removeOnReload) {}

    @ApiStatus.Internal
    public static final List<EventListenerHolder<?>> EVENT_LISTENERS = new ArrayList<>();
    @ApiStatus.Internal
    public static final Map<Identifier, TextureAtlasSprite> CTM_SPRITE_CACHE = new ConcurrentHashMap<>();

    private static final Multimap<Identifier, Material> SCRAPED_TEXTURES = HashMultimap.create();
    private static final Map<BlockStateModel, BlockStateModel> WRAPPED_MODELS = new IdentityHashMap<>();

    @ApiStatus.Internal
    public static void markTextureUsedForModel(Identifier modelLocation, Material material) {
        SCRAPED_TEXTURES.put(modelLocation, material);
    }

    public static void registerAtlasStitchedEventListener(boolean removeOnReload,
                                                          AssetEventListener.AtlasStitched listener) {
        EVENT_LISTENERS.add(new EventListenerHolder<>(listener, removeOnReload));
    }

    public static void registerAtlasStitchedEventListener(boolean removeOnReload, final Identifier atlasLocation,
                                                          final AssetEventListener.AtlasStitched listener) {
        registerAtlasStitchedEventListener(removeOnReload, event -> {
            if (event.getAtlas().location().equals(atlasLocation)) {
                listener.accept(event);
            }
        });
    }

    public static void registerBakeEventListener(boolean removeOnReload,
                                                 AssetEventListener.BlockStateModelReplacement listener) {
        EVENT_LISTENERS.add(new EventListenerHolder<>(listener, removeOnReload));
    }

    public static void registerStandaloneModelsEventListener(boolean removeOnReload,
                                                             AssetEventListener.RegisterStandalone listener) {
        EVENT_LISTENERS.add(new EventListenerHolder<>(listener, removeOnReload));
    }

    @Deprecated
    public static void registerAddModelsEventListener(boolean removeOnReload,
                                                      AssetEventListener.RegisterStandalone listener) {
        registerStandaloneModelsEventListener(removeOnReload, listener);
    }

    private static final AtomicInteger reloadCounter = new AtomicInteger(0);

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void registerReloadListener(AddClientReloadListenersEvent event) {
        event.addListener(GTCEu.id("client_model_event_helper"), (ResourceManagerReloadListener) resourceManager -> {
                    if (reloadCounter.addAndGet(1) > 1) {
                        EVENT_LISTENERS.removeIf(EventListenerHolder::removeOnReload);
                    }

                    CTM_SPRITE_CACHE.clear();
                    WRAPPED_MODELS.clear();
                    SCRAPED_TEXTURES.clear();
                    TextureMetadataHelper.invalidateCaches();
                });
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAtlasStitched(TextureAtlasStitchedEvent event) {
        for (var listener : EVENT_LISTENERS) {
            if (!(listener.listener instanceof AssetEventListener<?> assetEventListener)) continue;

            Class<?> eventClass = assetEventListener.eventClass();
            if (eventClass != null && eventClass.isInstance(event)) {
                ((AssetEventListener<TextureAtlasStitchedEvent>) listener.listener).accept(event);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // don't process baked model replacement here if ModernFix is loaded & dynamic resources is enabled
        if (GTCEu.Mods.isModernFixLoaded() && GTModernFixIntegration.isDynamicResourcesEnabled()) return;

        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        for (var entry : models.entrySet()) {
            BlockStateModel model = entry.getValue();

            // process all model replacers
            for (var listener : EVENT_LISTENERS) {
                if (!(listener.listener instanceof AssetEventListener.BlockStateModelReplacement modelReplacement)) continue;
                model = modelReplacement.modifyBlockStateModel(entry.getKey(), model);
            }
            entry.setValue(model);
        }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterStandalone(ModelEvent.RegisterStandalone event) {
        for (var listener : EVENT_LISTENERS) {
            if (!(listener.listener instanceof AssetEventListener<?> assetEventListener)) continue;

            Class<?> eventClass = assetEventListener.eventClass();
            if (eventClass != null && eventClass.isInstance(event)) {
                ((AssetEventListener<ModelEvent.RegisterStandalone>) listener.listener).accept(event);
            }
        }
    }

    // INTERNAL ASSET RELOAD LISTENER REGISTRATION

    @ApiStatus.Internal
    public static void initInternalAssetReloadListeners() {
        registerAtlasStitchedEventListener(false, TextureAtlas.LOCATION_BLOCKS, event -> {
            TextureAtlas atlas = event.getAtlas();
            // Cache all textures' CTM metadata
            // TODO lazy
            for (Identifier location : atlas.getTextures().keySet()) {
                var sec = TextureMetadataHelper.getMetadataFromRelativeLocation(location);
                sec.ifPresent(section -> {
                    if (section.connectionTexture() != null) {
                        TextureAtlasSprite ctmSprite = atlas.getSprite(section.connectionTexture());
                        CTM_SPRITE_CACHE.put(location, ctmSprite);
                    }
                });
            }

        });

        // register CTM model wrapper
        ModelEventHelper.registerBakeEventListener(false, ModelEventHelper::wrapConnectedTextureModel);
    }

    private static BlockStateModel wrapConnectedTextureModel(BlockState state, BlockStateModel model) {
        return WRAPPED_MODELS.computeIfAbsent(model, candidate -> {
            if (candidate instanceof ManuallyConnectedTextureModel || candidate instanceof CTMBakedModel<?>) {
                return candidate;
            }
            return hasConnectedTexture(candidate) ? new CTMBakedModel<>(candidate) : candidate;
        });
    }

    private static boolean hasConnectedTexture(BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        if (model instanceof CTMModelPartSource source) {
            source.collectConnectedTextureCandidates(RandomSource.create(0L), parts);
        } else {
            model.collectParts(RandomSource.create(0L), parts);
        }
        for (BlockStateModelPart part : parts) {
            if (hasConnectedTexture(part.getQuads(null))) return true;
            for (Direction direction : Direction.values()) {
                if (hasConnectedTexture(part.getQuads(direction))) return true;
            }
        }
        return false;
    }

    private static boolean hasConnectedTexture(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            Identifier sprite = quad.materialInfo().sprite().contents().name();
            if (CTM_SPRITE_CACHE.containsKey(sprite)) return true;
        }
        return false;
    }
}
