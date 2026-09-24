package com.gregtechceu.gtceu.client.model.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.model.item.FacadeBlockStateModel;
import com.gregtechceu.gtceu.client.model.ctm.CTMWeightedVariants;
import com.gregtechceu.gtceu.client.model.machine.multipart.KeyValuePartCondition;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Target unbaked machine model. Model references are resolved and baked once
 * for each model reload; state selection happens later on the meshing worker
 * from the block's immutable model-data snapshot.
 */
public record UnbakedMachineBlockStateModel(
        MachineDefinition definition,
        Map<String, BlockStateModel.Unbaked> variants,
        List<MultipartUnbaked> multipart,
        List<DynamicRender<?, ?>> dynamicRenders,
        Set<String> replaceableTextures,
        Map<String, Identifier> textureOverrides,
        Quadrant x, Quadrant y, Quadrant z) implements CustomUnbakedBlockStateModel {

    private static final Identifier PIPE_OUTPUT_OVERLAY = GTCEu.id("block/overlay/machine/overlay_pipe");
    private static final Identifier FLUID_OUTPUT_OVERLAY = GTCEu.id("block/overlay/machine/overlay_fluid_output");
    private static final Identifier ITEM_OUTPUT_OVERLAY = GTCEu.id("block/overlay/machine/overlay_item_output");
    private static final Identifier COVER_BACK_PLATE = GTCEu.id("block/cover/cover_back_plate");

    private static final Codec<JsonElement> RAW_JSON = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            value -> new Dynamic<>(JsonOps.INSTANCE, value));

    private static final Codec<MultipartUnbaked> MULTIPART_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RAW_JSON.optionalFieldOf("when", new JsonObject()).forGetter(MultipartUnbaked::condition),
            BlockStateModel.Unbaked.CODEC.fieldOf("apply").forGetter(MultipartUnbaked::model))
            .apply(instance, MultipartUnbaked::new));

    public static final MapCodec<UnbakedMachineBlockStateModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GTRegistries.MACHINES.codec().fieldOf("machine").forGetter(UnbakedMachineBlockStateModel::definition),
            Codec.unboundedMap(Codec.STRING, BlockStateModel.Unbaked.CODEC)
                    .optionalFieldOf("variants", Map.of()).forGetter(UnbakedMachineBlockStateModel::variants),
            MULTIPART_CODEC.listOf().optionalFieldOf("multipart", List.of())
                    .forGetter(UnbakedMachineBlockStateModel::multipart),
            DynamicRender.CODEC.listOf().optionalFieldOf("dynamic_renders", List.of())
                    .forGetter(UnbakedMachineBlockStateModel::dynamicRenders),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("replaceable_textures", Set.of())
                    .forGetter(UnbakedMachineBlockStateModel::replaceableTextures),
            Codec.unboundedMap(Codec.STRING, Identifier.CODEC)
                    .optionalFieldOf("texture_overrides", Map.of())
                    .forGetter(UnbakedMachineBlockStateModel::textureOverrides),
            Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(UnbakedMachineBlockStateModel::x),
            Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(UnbakedMachineBlockStateModel::y),
            Quadrant.CODEC.optionalFieldOf("z", Quadrant.R0).forGetter(UnbakedMachineBlockStateModel::z))
            .apply(instance, UnbakedMachineBlockStateModel::new));

    public UnbakedMachineBlockStateModel {
        variants = Map.copyOf(variants);
        multipart = List.copyOf(multipart);
        dynamicRenders = List.copyOf(dynamicRenders);
        replaceableTextures = Set.copyOf(replaceableTextures);
        textureOverrides = Map.copyOf(textureOverrides);
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        this.variants.values().forEach(model -> model.resolveDependencies(resolver));
        this.multipart.forEach(part -> part.model().resolveDependencies(resolver));
        this.textureOverrides.values().forEach(resolver::markDependency);
        resolver.markDependency(PIPE_OUTPUT_OVERLAY);
        resolver.markDependency(FLUID_OUTPUT_OVERLAY);
        resolver.markDependency(ITEM_OUTPUT_OVERLAY);
        resolver.markDependency(COVER_BACK_PLATE);
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
        StateDefinition<MachineDefinition, MachineRenderState> stateDefinition = this.definition.getStateDefinition();
        ModelDebugName debugName = () -> "machine " + this.definition.getId();
        TextureAtlasSprite pipeOverlay = baker.materials().get(new Material(PIPE_OUTPUT_OVERLAY), debugName).sprite();
        TextureAtlasSprite fluidOutputOverlay = baker.materials().get(new Material(FLUID_OUTPUT_OVERLAY), debugName).sprite();
        TextureAtlasSprite itemOutputOverlay = baker.materials().get(new Material(ITEM_OUTPUT_OVERLAY), debugName).sprite();
        TextureAtlasSprite coverBackPlate = baker.materials().get(new Material(COVER_BACK_PLATE), debugName).sprite();
        Map<String, TextureAtlasSprite> bakedTextureOverrides = new HashMap<>();
        this.textureOverrides.forEach((key, sprite) -> bakedTextureOverrides.put(key,
                baker.materials().get(new Material(sprite), debugName).sprite()));
        Map<MachineRenderState, BlockStateModel> bakedVariants = new IdentityHashMap<>();

        for (Map.Entry<String, BlockStateModel.Unbaked> variant : this.variants.entrySet()) {
            Predicate<MachineRenderState> predicate = variantPredicate(stateDefinition, variant.getKey());
            for (MachineRenderState state : stateDefinition.getPossibleStates()) {
                if (predicate.test(state)) {
                    if (bakedVariants.containsKey(state)) {
                        throw new IllegalStateException("Overlapping machine model variant for " + state);
                    }
                    bakedVariants.put(state, CTMWeightedVariants.bake(variant.getValue(), baker));
                }
            }
        }

        List<MachineBlockStateModel.MultipartPart> bakedMultipart = new ArrayList<>(this.multipart.size());
        for (MultipartUnbaked part : this.multipart) {
            Predicate<MachineRenderState> predicate = conditionPredicate(stateDefinition, part.condition());
            bakedMultipart.add(new MachineBlockStateModel.MultipartPart(predicate,
                    CTMWeightedVariants.bake(part.model(), baker)));
        }

        return new MachineBlockStateModel(this.definition, bakedVariants, bakedMultipart, this.dynamicRenders,
                this.replaceableTextures, bakedTextureOverrides, pipeOverlay, fluidOutputOverlay, itemOutputOverlay,
                new FacadeBlockStateModel(coverBackPlate), Quadrant.fromXYZAngles(this.x, this.y, this.z));
    }

    private static Predicate<MachineRenderState> variantPredicate(
            StateDefinition<MachineDefinition, MachineRenderState> definition, String serialized) {
        Map<Property<?>, Comparable<?>> properties = new HashMap<>();
        for (String part : serialized.split(",")) {
            if (part.isBlank()) continue;
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Invalid machine model predicate: " + serialized);
            }
            Property<?> property = definition.getProperty(pair[0].trim());
            Comparable<?> value = property == null ? null : propertyValue(property, pair[1].trim());
            if (property == null || value == null) {
                throw new IllegalArgumentException("Unknown machine model property/value in: " + serialized);
            }
            properties.put(property, value);
        }
        return state -> state.getDefinition() == definition.getOwner() &&
                properties.entrySet().stream().allMatch(entry -> state.getValue(entry.getKey()).equals(entry.getValue()));
    }

    private static Predicate<MachineRenderState> conditionPredicate(
            StateDefinition<MachineDefinition, MachineRenderState> definition, JsonElement json) {
        if (json == null || json.isJsonNull()) return state -> true;
        if (!json.isJsonObject()) {
            throw new IllegalArgumentException("Machine multipart condition must be an object: " + json);
        }
        JsonObject object = json.getAsJsonObject();
        if (object.size() == 0) return state -> true;

        if (object.size() == 1 && (object.has("AND") || object.has("OR"))) {
            String operator = object.has("AND") ? "AND" : "OR";
            JsonArray terms = GsonHelper.convertToJsonArray(object.get(operator), operator);
            List<Predicate<MachineRenderState>> predicates = new ArrayList<>(terms.size());
            for (JsonElement term : terms) predicates.add(conditionPredicate(definition, term));
            if (predicates.isEmpty()) {
                throw new IllegalArgumentException("Empty machine multipart " + operator + " condition");
            }
            return operator.equals("AND") ?
                    state -> predicates.stream().allMatch(predicate -> predicate.test(state)) :
                    state -> predicates.stream().anyMatch(predicate -> predicate.test(state));
        }

        List<Predicate<MachineRenderState>> predicates = new ArrayList<>(object.size());
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            predicates.add(new KeyValuePartCondition(entry.getKey(), entry.getValue().getAsString())
                    .getPredicate(definition));
        }
        return state -> predicates.stream().allMatch(predicate -> predicate.test(state));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @Nullable Comparable<?> propertyValue(Property<?> property, String value) {
        return (Comparable<?>) ((Property) property).getValue(value).orElse(null);
    }

    public record MultipartUnbaked(JsonElement condition, BlockStateModel.Unbaked model) {}
}
