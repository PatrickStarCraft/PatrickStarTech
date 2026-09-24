package com.gregtechceu.gtceu.client.model.machine;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.client.model.CoverBlockStateModel;
import com.gregtechceu.gtceu.client.model.GTModelProperties;
import com.gregtechceu.gtceu.client.model.TextureOverrideModel;
import com.gregtechceu.gtceu.client.model.ctm.CTMBakedModel;
import com.gregtechceu.gtceu.client.model.ctm.CTMModelPartSource;
import com.gregtechceu.gtceu.client.model.item.FacadeBlockStateModel;
import com.gregtechceu.gtceu.client.model.quad.StaticFaceBakery;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Static machine geometry for the target block-state model pipeline.
 *
 * <p>The model state comes from the meshing snapshot. This class deliberately
 * does not look up a live MetaMachine on chunk worker threads.</p>
 */
public final class MachineBlockStateModel implements DynamicBlockStateModel, MachineRenderModelOwner,
        CTMModelPartSource {

    private final MachineDefinition definition;
    private final Map<MachineRenderState, BlockStateModel> variants;
    private final List<MultipartPart> multipart;
    private final List<DynamicRender<?, ?>> dynamicRenders;
    private final Set<String> replaceableTextures;
    private final Map<String, TextureAtlasSprite> textureOverrides;
    private final TextureAtlasSprite pipeOverlaySprite;
    private final TextureAtlasSprite fluidOutputOverlaySprite;
    private final TextureAtlasSprite itemOutputOverlaySprite;
    private final FacadeBlockStateModel facadeModel;
    private final CoverBlockStateModel coverModel = new CoverBlockStateModel();

    public MachineBlockStateModel(MachineDefinition definition,
                                  Map<MachineRenderState, BlockStateModel> variants,
                                  List<MultipartPart> multipart,
                                  List<DynamicRender<?, ?>> dynamicRenders,
                                  Set<String> replaceableTextures,
                                  Map<String, TextureAtlasSprite> textureOverrides,
                                  TextureAtlasSprite pipeOverlaySprite,
                                  TextureAtlasSprite fluidOutputOverlaySprite,
                                  TextureAtlasSprite itemOutputOverlaySprite,
                                  FacadeBlockStateModel facadeModel) {
        this.definition = definition;
        IdentityHashMap<MachineRenderState, BlockStateModel> identityVariants = new IdentityHashMap<>();
        identityVariants.putAll(variants);
        this.variants = Collections.unmodifiableMap(identityVariants);
        this.multipart = List.copyOf(multipart);
        this.dynamicRenders = List.copyOf(dynamicRenders);
        this.replaceableTextures = Set.copyOf(replaceableTextures);
        this.textureOverrides = Map.copyOf(textureOverrides);
        this.pipeOverlaySprite = pipeOverlaySprite;
        this.fluidOutputOverlaySprite = fluidOutputOverlaySprite;
        this.itemOutputOverlaySprite = itemOutputOverlaySprite;
        this.facadeModel = facadeModel;
        this.dynamicRenders.forEach(render -> render.setParent(this));
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        MachineRenderState fallback = this.definition.defaultRenderState();
        for (MultipartPart part : this.multipart) {
            if (part.condition().test(fallback)) {
                return part.model().particleMaterial();
            }
        }
        BlockStateModel variant = this.getVariant(fallback);
        if (variant != null) {
            return variant.particleMaterial();
        }
        if (!this.multipart.isEmpty()) {
            return this.multipart.getFirst().model().particleMaterial();
        }
        throw new IllegalStateException("Machine model for " + this.definition + " has no baked geometry");
    }

    @Override
    @Deprecated
    public int materialFlags() {
        MachineRenderState fallback = this.definition.defaultRenderState();
        int flags = 0;
        for (MultipartPart part : this.multipart) {
            if (part.condition().test(fallback)) {
                flags |= part.model().materialFlags();
            }
        }
        BlockStateModel variant = this.getVariant(fallback);
        if (variant != null) {
            flags |= variant.materialFlags();
        }
        return flags;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                             RandomSource random, List<BlockStateModelPart> output) {
        MachineRenderState machineState = getMachineState(level, pos);
        long seed = random.nextLong();

        for (MultipartPart part : this.multipart) {
            if (part.condition().test(machineState)) {
                part.model().collectParts(level, pos, state, RandomSource.create(seed), output);
            }
        }

        BlockStateModel variant = this.getVariant(machineState);
        if (variant != null) {
            variant.collectParts(level, pos, state, RandomSource.create(seed), output);
        }

        for (ControllerPartSelection selection : this.collectControllerPartSelections(
                level, pos, state, RandomSource.create(seed))) {
            output.addAll(selection.parts());
        }

        MachineOutputOverlayPart outputPart = this.createOutputOverlayPart(getOutputState(level, pos));
        if (outputPart != null) output.add(outputPart);
        this.facadeModel.collectParts(level, pos, state, RandomSource.create(seed), output);
        this.coverModel.collectParts(level, pos, output);
    }

    @Override
    public void collectConnectedTextureCandidates(RandomSource random, List<BlockStateModelPart> output) {
        long seed = random.nextLong();
        Set<BlockStateModel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (MultipartPart part : this.multipart) {
            if (seen.add(part.model())) collectTextureCandidates(part.model(), seed, output);
        }
        for (BlockStateModel variant : this.variants.values()) {
            if (seen.add(variant)) collectTextureCandidates(variant, seed, output);
        }
    }

    private static void collectTextureCandidates(BlockStateModel model, long seed,
                                                 List<BlockStateModelPart> output) {
        RandomSource random = RandomSource.create(seed);
        if (model instanceof CTMModelPartSource source) {
            source.collectConnectedTextureCandidates(random, output);
        } else {
            model.collectParts(random, output);
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        MachineRenderState machineState = getMachineState(level, pos);
        long seed = random.nextLong();
        List<PartGeometryKey> multipartKeys = new ArrayList<>();

        for (int i = 0; i < this.multipart.size(); i++) {
            MultipartPart part = this.multipart.get(i);
            if (part.condition().test(machineState)) {
                Object key = part.model().createGeometryKey(level, pos, state, RandomSource.create(seed));
                multipartKeys.add(new PartGeometryKey(i, key));
            }
        }

        BlockStateModel variant = this.getVariant(machineState);
        Object variantKey = variant == null ? null :
                variant.createGeometryKey(level, pos, state, RandomSource.create(seed));
        List<ControllerPartKey> controllerPartKeys = this.collectControllerPartSelections(
                        level, pos, state, RandomSource.create(seed)).stream()
                .map(selection -> new ControllerPartKey(selection.descriptorIndex(), selection.geometryKey()))
                .toList();
        MachineOutputRenderState outputState = getOutputState(level, pos);
        Object facadeKey = this.facadeModel.createGeometryKey(level, pos, state, RandomSource.create(seed));
        Object coverKey = this.coverModel.geometryKey(level, pos);
        return new GeometryKey(this.definition, machineState, outputState, variantKey, List.copyOf(multipartKeys),
                controllerPartKeys, facadeKey, coverKey);
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(
            BlockAndTintGetter level, BlockPos pos, BlockState state) {
        MachineRenderState machineState = getMachineState(level, pos);
        for (MultipartPart part : this.multipart) {
            if (part.condition().test(machineState)) {
                return part.model().particleMaterial(level, pos, state);
            }
        }
        BlockStateModel variant = this.getVariant(machineState);
        if (variant != null) {
            return variant.particleMaterial(level, pos, state);
        }
        if (!this.multipart.isEmpty()) {
            return this.multipart.getFirst().model().particleMaterial(level, pos, state);
        }
        throw new IllegalStateException("Machine model for " + this.definition + " has no baked geometry");
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        MachineRenderState machineState = getMachineState(level, pos);
        int flags = 0;
        for (MultipartPart part : this.multipart) {
            if (part.condition().test(machineState)) {
                flags |= part.model().materialFlags(level, pos, state);
            }
        }
        BlockStateModel variant = this.getVariant(machineState);
        if (variant != null) {
            flags |= variant.materialFlags(level, pos, state);
        }
        for (ControllerPartSelection selection : this.collectControllerPartSelections(
                level, pos, state, RandomSource.create(state.getSeed(pos)))) {
            for (BlockStateModelPart part : selection.parts()) flags |= part.materialFlags();
        }
        MachineOutputOverlayPart outputPart = this.createOutputOverlayPart(getOutputState(level, pos));
        if (outputPart != null) flags |= outputPart.materialFlags();
        flags |= this.facadeModel.materialFlags(level, pos, state);
        flags |= this.coverModel.materialFlags(level, pos);
        return flags;
    }

    private MachineOutputOverlayPart createOutputOverlayPart(MachineOutputRenderState outputState) {
        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        if (outputState.itemOutputDirection() != null) {
            addOutputQuad(quads, outputState.itemOutputDirection(), this.pipeOverlaySprite);
            if (outputState.autoOutputItems()) {
                addOutputQuad(quads, outputState.itemOutputDirection(), this.itemOutputOverlaySprite);
            }
        }
        if (outputState.fluidOutputDirection() != null) {
            addOutputQuad(quads, outputState.fluidOutputDirection(), this.pipeOverlaySprite);
            if (outputState.autoOutputFluids()) {
                addOutputQuad(quads, outputState.fluidOutputDirection(), this.fluidOutputOverlaySprite);
            }
        }
        if (quads.isEmpty()) return null;

        EnumMap<Direction, List<BakedQuad>> immutable = new EnumMap<>(Direction.class);
        quads.forEach((direction, faces) -> immutable.put(direction, List.copyOf(faces)));
        return new MachineOutputOverlayPart(Map.copyOf(immutable), new Material.Baked(this.pipeOverlaySprite, false));
    }

    private static void addOutputQuad(EnumMap<Direction, List<BakedQuad>> quads, Direction direction,
                                      TextureAtlasSprite sprite) {
        quads.computeIfAbsent(direction, $ -> new ArrayList<>()).add(
                StaticFaceBakery.bakeFace(StaticFaceBakery.OUTPUT_OVERLAY, direction, sprite));
    }

    private static MachineOutputRenderState getOutputState(BlockAndTintGetter level, BlockPos pos) {
        ModelData data = level.getModelData(pos);
        MachineOutputRenderState outputState = data.get(GTModelProperties.MACHINE_OUTPUT_RENDER_STATE);
        return outputState == null ? MachineOutputRenderState.EMPTY : outputState;
    }

    private List<ControllerPartSelection> collectControllerPartSelections(BlockAndTintGetter level, BlockPos pos,
                                                                          BlockState state, RandomSource random) {
        ControllerPartRenderState controllerState = level.getModelData(pos)
                .get(GTModelProperties.FORMED_PART_RENDER_STATE);
        if (controllerState == null) return List.of();

        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        MachineBlockStateModel controllerModel = find(modelSet.get(controllerState.controllerBlockState()));
        if (controllerModel == null) return List.of();

        long seed = random.nextLong();
        List<ControllerPartSelection> result = new ArrayList<>();
        List<DynamicRender<?, ?>> descriptors = controllerModel.dynamicRenders;
        for (int index = 0; index < descriptors.size(); index++) {
            DynamicRender<?, ?> descriptor = descriptors.get(index);
            if (!(descriptor instanceof ControllerPartModel partModel)) continue;

            ControllerPartModel.PartModelResult collected = partModel.collectPartModel(controllerState, modelSet,
                    level, pos, state, RandomSource.create(seed));
            if (collected.parts().isEmpty()) continue;

            List<BlockStateModelPart> parts = this.applyControllerTextureOverrides(collected.parts(), controllerModel);
            result.add(new ControllerPartSelection(index, parts,
                    new ControllerPartGeometryKey(controllerState, collected.geometryKey())));
        }
        return List.copyOf(result);
    }

    public List<DynamicRender<?, ?>> getDynamicRenders() {
        return this.dynamicRenders;
    }

    public static @Nullable MachineBlockStateModel find(BlockStateModel model) {
        if (model instanceof MachineBlockStateModel machineModel) return machineModel;
        if (model instanceof CTMBakedModel<?> ctmModel && ctmModel.getParent() instanceof MachineBlockStateModel machineModel) {
            return machineModel;
        }
        return null;
    }

    private List<BlockStateModelPart> applyControllerTextureOverrides(List<BlockStateModelPart> parts,
                                                                       MachineBlockStateModel controllerModel) {
        Map<String, TextureAtlasSprite> overrides = this.remappedTextureOverridesFrom(controllerModel);
        if (overrides.isEmpty()) return parts;

        List<BlockStateModelPart> result = new ArrayList<>(parts.size());
        for (BlockStateModelPart part : parts) {
            EnumMap<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                faces.put(direction, List.copyOf(TextureOverrideModel.retextureQuads(part.getQuads(direction), overrides)));
            }
            List<BakedQuad> unculled = List.copyOf(TextureOverrideModel.retextureQuads(part.getQuads(null), overrides));
            result.add(new RetexturedControllerPart(part, unculled, Map.copyOf(faces)));
        }
        return List.copyOf(result);
    }

    private MachineRenderState getMachineState(BlockAndTintGetter level, BlockPos pos) {
        ModelData data = level.getModelData(pos);
        MachineRenderState state = data.get(GTModelProperties.MACHINE_RENDER_STATE);
        return state != null && state.getDefinition() == this.definition ?
                state : this.definition.defaultRenderState();
    }

    private @Nullable BlockStateModel getVariant(MachineRenderState state) {
        BlockStateModel exact = this.variants.get(state);
        return exact != null ? exact : this.variants.get(this.definition.defaultRenderState());
    }

    public MachineDefinition definition() {
        return this.definition;
    }

    @Override
    public MachineDefinition getDefinition() {
        return this.definition;
    }

    public List<DynamicRender<?, ?>> dynamicRenders() {
        return this.dynamicRenders;
    }

    public Set<String> replaceableTextures() {
        return this.replaceableTextures;
    }

    public Map<String, TextureAtlasSprite> textureOverrides() {
        return this.textureOverrides;
    }

    /** Maps a controller's override keys to the texture slots this model exposes for formed multiblock parts. */
    public Map<String, TextureAtlasSprite> remappedTextureOverridesFrom(MachineBlockStateModel controller) {
        if (controller.textureOverrides.isEmpty()) return Map.of();
        Map<String, TextureAtlasSprite> remapped = new HashMap<>();
        controller.textureOverrides.forEach((key, sprite) -> this.remapReplaceableTextures(key)
                .forEach(mappedKey -> remapped.putIfAbsent(mappedKey, sprite)));
        return Map.copyOf(remapped);
    }

    public List<BakedQuad> retextureFormedPartQuads(List<BakedQuad> quads, MachineBlockStateModel controller) {
        return TextureOverrideModel.retextureQuads(quads, this.remappedTextureOverridesFrom(controller));
    }

    public List<String> remapReplaceableTextures(String key) {
        if (this.replaceableTextures.contains(key)) return List.of(key);
        return switch (key) {
            case "side", "top", "bottom" -> List.of("all");
            case "all" -> List.of("side", "top", "bottom");
            default -> List.of();
        };
    }

    public record MultipartPart(Predicate<MachineRenderState> condition, BlockStateModel model) {}

    private record PartGeometryKey(int selectorIndex, Object geometryKey) {}

    private record ControllerPartSelection(int descriptorIndex, List<BlockStateModelPart> parts, Object geometryKey) {}

    private record ControllerPartGeometryKey(ControllerPartRenderState controller, Object modelGeometryKey) {}

    private record ControllerPartKey(int descriptorIndex, Object geometryKey) {}

    private record GeometryKey(MachineDefinition definition, MachineRenderState machineState,
                               MachineOutputRenderState outputState, Object variantKey,
                               List<PartGeometryKey> multipartKeys, List<ControllerPartKey> controllerPartKeys,
                               Object facadeKey, Object coverKey) {}

    private record RetexturedControllerPart(BlockStateModelPart original, List<BakedQuad> unculledQuads,
                                            Map<Direction, List<BakedQuad>> faces) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? this.unculledQuads : this.faces.getOrDefault(direction, List.of());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.original.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.original.particleMaterial();
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            int flags = 0;
            for (List<BakedQuad> quads : this.faces.values()) {
                for (BakedQuad quad : quads) flags |= quad.materialInfo().flags();
            }
            for (BakedQuad quad : this.unculledQuads) flags |= quad.materialInfo().flags();
            return flags;
        }
    }

    private record MachineOutputOverlayPart(Map<Direction, List<BakedQuad>> quads,
                                            Material.Baked particleMaterial) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? List.of() : this.quads.getOrDefault(direction, List.of());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.particleMaterial;
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            int flags = 0;
            for (List<BakedQuad> faces : this.quads.values()) {
                for (BakedQuad quad : faces) flags |= quad.materialInfo().flags();
            }
            return flags;
        }
    }
}
