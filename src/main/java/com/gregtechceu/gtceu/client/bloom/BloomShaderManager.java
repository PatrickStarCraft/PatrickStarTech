package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.config.GTEarlyConfig;

import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.UniformValue;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.serialization.JsonOps;
import dev.toma.configuration.config.validate.ValidationResult;
import dev.toma.configuration.config.validate.Validator;
import dev.toma.configuration.config.value.IConfigValueReadable;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.nio.file.Path;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.StrictJsonParser;
import org.joml.Vector2f;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
@UtilityClass
public class BloomShaderManager {

    public static @UnknownNullability PostChain BLOOM_CHAIN = null;
    public static @UnknownNullability RenderTarget BLOOM_TARGET = null;
    public static @UnknownNullability RenderTarget BLOOM_OUTPUT_TARGET = null;

    private static @Nullable ProjectionMatrixBuffer bloomProjectionBuffer;
    private static @Nullable BloomType loadedBloomType;
    private static @Nullable BloomSettings loadedBloomSettings;
    private static @Nullable BloomSettings failedBloomSettings;
    private static int availabilityTicks;

    private static final Identifier BLOOM_SOURCE_TARGET_ID = GTCEu.id("bloom_source");
    private static final Identifier BLOOM_OUTPUT_TARGET_ID = GTCEu.id("bloom_output");

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event) {
        // Add a validator & update listener for the bloom type config option
        // this path must match the config option's path exactly.
        ConfigHolder.INTERNAL_INSTANCE.getConfigValue("client.bloom.type", BloomType.class)
                .ifPresentOrElse(option -> option.addValidator(new BloomTypeConfigValidator()),
                        () -> GTCEu.LOGGER.warn(
                                "Could not initialize bloom type config update listener! The shaders will not update automatically when the config option is changed."));
    }

    /// @return whether the post effect was loaded successfully
    @ApiStatus.Internal
    public static boolean initPostShaders() {
        deinitPostShaders();

        // forcefully update availability on (re-)load
        bloomAvailable = updateBloomShaderAvailability();

        if (!isBloomAvailable()) return false;

        Identifier id = null;

        switch (ConfigHolder.INSTANCE.client.bloom.type) {
            case UNITY -> id = GTCEu.id("shaders/post/bloom_unity.json");
            case UNREAL -> id = GTCEu.id("shaders/post/bloom_unreal.json");
            case DISABLED -> {
                return true;
            }
            // skip adding a default branch in favor of the if statement below
        }
        if (id == null) {
            GTCEu.LOGGER.error("Invalid bloom style {}", ConfigHolder.INSTANCE.client.bloom.type);
            ConfigHolder.INSTANCE.client.bloom.type = BloomType.DISABLED;
            return false;
        }

        TextureTarget bloomTarget = null;
        TextureTarget outputTarget = null;
        LoadedBloomChain loaded = null;
        try {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
            bloomTarget = new TextureTarget("gtceu_bloom_source", mainTarget.width, mainTarget.height,
                    true, GpuFormat.RGBA8_UNORM);
            outputTarget = new TextureTarget("gtceu_bloom_output", mainTarget.width, mainTarget.height,
                    false, GpuFormat.RGBA8_UNORM);

            BloomType type = ConfigHolder.INSTANCE.client.bloom.type;
            loaded = loadConfiguredChain(mc, id, type, getBloomSettings());
            BLOOM_CHAIN = loaded.chain();
            BLOOM_TARGET = bloomTarget;
            BLOOM_OUTPUT_TARGET = outputTarget;
            bloomProjectionBuffer = loaded.projectionBuffer();
            loadedBloomType = type;
            loadedBloomSettings = getBloomSettings();
            failedBloomSettings = null;
            return true;
        } catch (Exception e) {
            GTCEu.LOGGER.error("Failed to {} shader {}:",
                    e instanceof JsonSyntaxException || e instanceof JsonParseException ? "parse" : "load", id, e);
            if (loaded != null) {
                loaded.chain().close();
                loaded.projectionBuffer().close();
            }
            if (bloomTarget != null) bloomTarget.destroyBuffers();
            if (outputTarget != null) outputTarget.destroyBuffers();
            BLOOM_CHAIN = null;
            BLOOM_TARGET = null;
            BLOOM_OUTPUT_TARGET = null;
            bloomProjectionBuffer = null;
            loadedBloomType = null;
            loadedBloomSettings = null;
            return false;
        }
    }

    private static void deinitPostShaders() {
        if (BLOOM_CHAIN != null) BLOOM_CHAIN.close();
        if (bloomProjectionBuffer != null) bloomProjectionBuffer.close();
        if (BLOOM_TARGET != null) BLOOM_TARGET.destroyBuffers();
        if (BLOOM_OUTPUT_TARGET != null) BLOOM_OUTPUT_TARGET.destroyBuffers();

        BLOOM_CHAIN = null;
        BLOOM_TARGET = null;
        BLOOM_OUTPUT_TARGET = null;
        bloomProjectionBuffer = null;
        loadedBloomType = null;
        loadedBloomSettings = null;
        failedBloomSettings = null;
    }

    private static LoadedBloomChain loadConfiguredChain(Minecraft minecraft, Identifier resourceId,
                                                         BloomType type, BloomSettings settings) throws Exception {
        Resource resource = minecraft.getResourceManager().getResource(resourceId)
                .orElseThrow(() -> new FileNotFoundException("Missing bloom post-chain " + resourceId));
        PostChainConfig config;
        try (Reader reader = resource.openAsReader()) {
            config = PostChainConfig.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(reader))
                    .getOrThrow(JsonSyntaxException::new);
        }
        config = applyBloomSettings(config, settings);

        Projection projection = new Projection();
        projection.setupOrtho(0.1F, 1000.0F, 1.0F, 1.0F, false);
        ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("gtceu-bloom-" + type.name().toLowerCase(Locale.ROOT));
        Identifier chainId = GTCEu.id("bloom/" + type.name().toLowerCase(Locale.ROOT));
        try {
            PostChain chain = PostChain.load(config, minecraft.getTextureManager(),
                    Set.of(PostChain.MAIN_TARGET_ID, BLOOM_SOURCE_TARGET_ID, BLOOM_OUTPUT_TARGET_ID),
                    chainId, projection, projectionBuffer);
            return new LoadedBloomChain(chain, projectionBuffer);
        } catch (Exception | Error failure) {
            projectionBuffer.close();
            throw failure;
        }
    }

    private static PostChainConfig applyBloomSettings(PostChainConfig config, BloomSettings settings) {
        List<PostChainConfig.Pass> passes = new ArrayList<>(config.passes().size());
        for (int i = 0; i < config.passes().size(); i++) {
            PostChainConfig.Pass pass = config.passes().get(i);
            Map<String, List<UniformValue>> uniforms = new HashMap<>(pass.uniforms());
            Identifier shader = pass.fragmentShaderId();

            if (shader.equals(GTCEu.id("program/filter_bloom_color"))) {
                uniforms.put("BloomDepth", List.of(
                        new UniformValue.FloatUniform(0.05F),
                        new UniformValue.FloatUniform(settings.depthFar())));
            } else if (shader.equals(GTCEu.id("program/blur"))) {
                float x = (i & 1) == 0 ? 0.0F : settings.step();
                float y = (i & 1) == 0 ? settings.step() : 0.0F;
                List<UniformValue> oldValues = uniforms.get("BlurSettings");
                float radius = oldValues != null && oldValues.size() > 1 && oldValues.get(1) instanceof UniformValue.FloatUniform value
                        ? value.value() : 5.0F;
                uniforms.put("BlurSettings", List.of(
                        new UniformValue.Vec2Uniform(new Vector2f(x, y)),
                        new UniformValue.FloatUniform(radius)));
            } else if (shader.equals(GTCEu.id("program/unity_composite"))) {
                uniforms.put("BloomSettings", List.of(
                        new UniformValue.FloatUniform(settings.strength()),
                        new UniformValue.FloatUniform(settings.baseBrightness()),
                        new UniformValue.FloatUniform(settings.maxBrightness()),
                        new UniformValue.FloatUniform(settings.minBrightness())));
            } else if (shader.equals(GTCEu.id("program/unreal_composite"))) {
                List<UniformValue> oldValues = uniforms.get("BloomSettings");
                float radius = oldValues != null && !oldValues.isEmpty() && oldValues.getFirst() instanceof UniformValue.FloatUniform value
                        ? value.value() : 1.0F;
                uniforms.put("BloomSettings", List.of(
                        new UniformValue.FloatUniform(radius),
                        new UniformValue.FloatUniform(settings.strength()),
                        new UniformValue.FloatUniform(settings.baseBrightness()),
                        new UniformValue.FloatUniform(settings.maxBrightness()),
                        new UniformValue.FloatUniform(settings.minBrightness())));
            }

            passes.add(new PostChainConfig.Pass(pass.vertexShaderId(), shader, pass.inputs(), pass.outputTarget(), uniforms));
        }
        return new PostChainConfig(config.internalTargets(), passes);
    }

    private static BloomSettings getBloomSettings() {
        var config = ConfigHolder.INSTANCE.client.bloom;
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        return new BloomSettings(config.strength, config.baseBrightness, config.minBrightness, config.maxBrightness,
                config.step, renderDistance * 16.0F * 4.0F);
    }

    public static void refreshPostChainSettings() {
        BloomType type = loadedBloomType;
        if (BLOOM_CHAIN == null || type == null || BLOOM_TARGET == null || BLOOM_OUTPUT_TARGET == null) return;
        BloomSettings settings = getBloomSettings();
        if (settings.equals(loadedBloomSettings) || settings.equals(failedBloomSettings)) return;

        Identifier resourceId = switch (type) {
            case UNITY -> GTCEu.id("shaders/post/bloom_unity.json");
            case UNREAL -> GTCEu.id("shaders/post/bloom_unreal.json");
            case DISABLED -> null;
        };
        if (resourceId == null) return;

        try {
            LoadedBloomChain replacement = loadConfiguredChain(Minecraft.getInstance(), resourceId, type, settings);
            PostChain oldChain = BLOOM_CHAIN;
            ProjectionMatrixBuffer oldProjectionBuffer = bloomProjectionBuffer;
            BLOOM_CHAIN = replacement.chain();
            bloomProjectionBuffer = replacement.projectionBuffer();
            loadedBloomSettings = settings;
            failedBloomSettings = null;
            oldChain.close();
            if (oldProjectionBuffer != null) oldProjectionBuffer.close();
        } catch (Exception e) {
            failedBloomSettings = settings;
            GTCEu.LOGGER.error("Failed to update bloom post-chain settings:", e);
        }
    }

    public static void resizeTargets(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (BLOOM_TARGET != null && (BLOOM_TARGET.width != width || BLOOM_TARGET.height != height)) {
            BLOOM_TARGET.resize(width, height);
        }
        if (BLOOM_OUTPUT_TARGET != null && (BLOOM_OUTPUT_TARGET.width != width || BLOOM_OUTPUT_TARGET.height != height)) {
            BLOOM_OUTPUT_TARGET.resize(width, height);
        }
    }

    private record BloomSettings(float strength, float baseBrightness, float minBrightness, float maxBrightness,
                                 float step, float depthFar) {}

    private record LoadedBloomChain(PostChain chain, ProjectionMatrixBuffer projectionBuffer) {}

    public static boolean isBloomActive() {
        return BLOOM_CHAIN != null && BLOOM_TARGET != null &&
                ConfigHolder.INSTANCE.client.bloom.type != BloomType.DISABLED && isBloomAvailable();
    }

    @Getter
    private static boolean bloomAvailable = updateBloomShaderAvailability();

    @ApiStatus.Internal
    public static void updateShaderAvailability(ClientTickEvent.Pre event) {

        // only update bloom availability once a second so every frame isn't bogged down with mod loaded checks
        if (++availabilityTicks % 20 != 0) return;

        bloomAvailable = updateBloomShaderAvailability();
    }

    private static boolean updateBloomShaderAvailability() {
        return !GTEarlyConfig.OPTIFINE_PRESENT &&
                !(GTCEu.Mods.isIrisOculusLoaded() && IrisCallWrapper.isShaderActive());
    }

    private static class IrisCallWrapper {

        private static boolean isShaderActive() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }

    private static final class BloomTypeConfigValidator implements Validator<BloomType> {

        @Override
        public ValidationResult validate(BloomType newType, IConfigValueReadable<BloomType> configField) {
            if (!BloomShaderManager.initPostShaders()) {
                // failed to load post shaders

                Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath();
                Path logFile = gameDir.resolve(Path.of("logs", "latest.log"));
                Component latestLogClickable = Component.literal(gameDir.relativize(logFile).toString())
                        .withStyle((style) -> style.withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenFile(logFile)));

                return ValidationResult.warning(Component.translatable(
                        "config.gtceu.option.bloomType.load_error", latestLogClickable));
            } else {
                // post shader loaded successfully
                return ValidationResult.success();
            }
        }
    }
}
