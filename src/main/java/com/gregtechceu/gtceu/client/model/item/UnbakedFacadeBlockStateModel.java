package com.gregtechceu.gtceu.client.model.item;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import com.mojang.serialization.MapCodec;

/** Codec target for the dynamically supplied cover-facade model part. */
public record UnbakedFacadeBlockStateModel() implements CustomUnbakedBlockStateModel {

    public static final MapCodec<UnbakedFacadeBlockStateModel> CODEC = MapCodec.unit(UnbakedFacadeBlockStateModel::new);

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(GTCEu.id("block/cover/cover_back_plate"));
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
        ModelDebugName debugName = () -> "GT cover back plate";
        TextureAtlasSprite backPlate = baker.materials()
                .get(new Material(GTCEu.id("block/cover/cover_back_plate")), debugName)
                .sprite();
        return new FacadeBlockStateModel(backPlate);
    }
}
