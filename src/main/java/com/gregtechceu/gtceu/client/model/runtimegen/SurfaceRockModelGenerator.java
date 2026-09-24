package com.gregtechceu.gtceu.client.model.runtimegen;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.block.SurfaceRockBlock;
import com.gregtechceu.gtceu.data.model.builder.RuntimeModelResources;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;

import org.jspecify.annotations.NullMarked;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonObject;

import javax.annotation.ParametersAreNonnullByDefault;

@NullMarked
@ParametersAreNonnullByDefault
public class SurfaceRockModelGenerator {

    private static final Set<SurfaceRockModelGenerator> MODELS = new HashSet<>();

    public static void reinitModels() {
        for (SurfaceRockModelGenerator model : MODELS) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(model.block);
            Identifier modelId = blockId.withPrefix("block/");

            JsonObject modelJson = new JsonObject();
            modelJson.addProperty("parent", GTCEu.id("block/surface_rock").toString());
            RuntimeModelResources.emitModel(modelId, modelJson, GTDynamicResourcePack::addResource);

            JsonObject variants = new JsonObject();
            variants.add("facing=down", variant(modelId, -1, -1));
            variants.add("facing=up", variant(modelId, 180, -1));
            variants.add("facing=north", variant(modelId, -1, 90));
            variants.add("facing=south", variant(modelId, -1, 270));
            variants.add("facing=west", variant(modelId, 270, -1));
            variants.add("facing=east", variant(modelId, 90, -1));
            JsonObject blockState = new JsonObject();
            blockState.add("variants", variants);
            RuntimeModelResources.emitBlockState(blockId, blockState, GTDynamicResourcePack::addResource);

            int materialColor = -1;
            if (model.block instanceof SurfaceRockBlock surfaceRock) {
                materialColor = surfaceRock.getMaterial().getMaterialRGB();
            }
            RuntimeModelResources.emitItem(blockId, RuntimeModelResources.itemDefinition(modelId,
                    RuntimeModelResources.constantTints(-1, materialColor)),
                    GTDynamicResourcePack::addResource);
        }
    }

    private static JsonObject variant(Identifier modelId, int xRotation, int yRotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modelId.toString());
        if (xRotation >= 0) {
            variant.addProperty("x", xRotation);
        }
        if (yRotation >= 0) {
            variant.addProperty("y", yRotation);
        }
        return variant;
    }

    private final Block block;

    protected SurfaceRockModelGenerator(Block block) {
        this.block = block;
    }

    public static void add(Block block) {
        MODELS.add(new SurfaceRockModelGenerator(block));
    }
}
