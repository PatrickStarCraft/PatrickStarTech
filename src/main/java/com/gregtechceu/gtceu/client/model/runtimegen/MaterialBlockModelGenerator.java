package com.gregtechceu.gtceu.client.model.runtimegen;

import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.client.color.MaterialLayerTintSource;
import com.gregtechceu.gtceu.data.model.builder.RuntimeModelResources;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import com.google.gson.JsonArray;

import java.util.HashSet;
import java.util.Set;

public class MaterialBlockModelGenerator {

    private static final Set<MaterialBlockModelGenerator> MODELS = new HashSet<>();

    public static void reinitModels() {
        for (MaterialBlockModelGenerator model : MODELS) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(model.block);
            Identifier modelId = model.type.getBlockModelPath(model.iconSet, true);

            RuntimeModelResources.emitBlockState(blockId, RuntimeModelResources.simpleBlockState(modelId),
                    GTDynamicResourcePack::addResource);
            if (model.block instanceof MaterialBlock materialBlock) {
                RuntimeModelResources.emitItem(BuiltInRegistries.ITEM.getKey(model.block.asItem()), modelId,
                        materialTints(), GTDynamicResourcePack::addResource);
            } else {
                RuntimeModelResources.emitItem(BuiltInRegistries.ITEM.getKey(model.block.asItem()), modelId,
                        GTDynamicResourcePack::addResource);
            }
        }
    }

    private static JsonArray materialTints() {
        return RuntimeModelResources.dynamicLayerTints(MaterialLayerTintSource.ID, 10);
    }

    private final Block block;
    private final MaterialIconType type;
    private final MaterialIconSet iconSet;

    protected MaterialBlockModelGenerator(Block block, MaterialIconType type, MaterialIconSet iconSet) {
        this.block = block;
        this.type = type;
        this.iconSet = iconSet;
    }

    public static void add(Block block, MaterialIconType type, MaterialIconSet iconSet) {
        MODELS.add(new MaterialBlockModelGenerator(block, type, iconSet));
    }
}
