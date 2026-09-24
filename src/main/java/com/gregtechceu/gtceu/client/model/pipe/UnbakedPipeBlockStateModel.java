package com.gregtechceu.gtceu.client.model.pipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.model.ctm.CTMWeightedVariants;
import com.gregtechceu.gtceu.client.model.item.FacadeBlockStateModel;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record UnbakedPipeBlockStateModel(
        Map<String, BlockStateModel.Unbaked> parts,
        Map<String, BlockStateModel.Unbaked> restrictors) implements CustomUnbakedBlockStateModel {

    private static final Set<String> CENTER_KEYS = Set.of("center", "core", "null", "none");
    private static final Identifier COVER_BACK_PLATE = GTCEu.id("block/cover/cover_back_plate");

    public static final MapCodec<UnbakedPipeBlockStateModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, BlockStateModel.Unbaked.CODEC)
                    .optionalFieldOf("parts", Map.of()).forGetter(UnbakedPipeBlockStateModel::parts),
            Codec.unboundedMap(Codec.STRING, BlockStateModel.Unbaked.CODEC)
                    .optionalFieldOf("restrictors", Map.of()).forGetter(UnbakedPipeBlockStateModel::restrictors))
            .apply(instance, UnbakedPipeBlockStateModel::new));

    public UnbakedPipeBlockStateModel {
        parts = Map.copyOf(parts);
        restrictors = Map.copyOf(restrictors);
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        this.parts.values().forEach(model -> model.resolveDependencies(resolver));
        this.restrictors.values().forEach(model -> model.resolveDependencies(resolver));
        resolver.markDependency(COVER_BACK_PLATE);
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
        ModelDebugName debugName = () -> "GT pipe cover back plate";
        TextureAtlasSprite coverBackPlate = baker.materials().get(new Material(COVER_BACK_PLATE), debugName).sprite();
        BlockStateModel center = null;
        EnumMap<Direction, BlockStateModel> bakedParts = new EnumMap<>(Direction.class);
        EnumMap<Direction, BlockStateModel> bakedRestrictors = new EnumMap<>(Direction.class);

        for (Map.Entry<String, BlockStateModel.Unbaked> part : this.parts.entrySet()) {
            String key = part.getKey().toLowerCase(Locale.ROOT);
            BlockStateModel baked = CTMWeightedVariants.bake(part.getValue(), baker);
            Direction direction = Direction.byName(key);
            if (direction != null) {
                bakedParts.put(direction, baked);
            } else if (CENTER_KEYS.contains(key)) {
                if (center != null) throw new IllegalArgumentException("Duplicate center part in pipe model");
                center = baked;
            } else {
                throw new IllegalArgumentException("Invalid pipe model part specifier: " + part.getKey());
            }
        }

        for (Map.Entry<String, BlockStateModel.Unbaked> restrictor : this.restrictors.entrySet()) {
            Direction direction = Direction.byName(restrictor.getKey());
            if (direction == null) {
                throw new IllegalArgumentException("Invalid pipe restrictor direction: " + restrictor.getKey());
            }
            bakedRestrictors.put(direction, CTMWeightedVariants.bake(restrictor.getValue(), baker));
        }
        return new PipeBlockStateModel(center, bakedParts, bakedRestrictors,
                new FacadeBlockStateModel(coverBackPlate));
    }
}
