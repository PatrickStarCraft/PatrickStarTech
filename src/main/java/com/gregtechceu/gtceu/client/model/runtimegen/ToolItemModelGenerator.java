package com.gregtechceu.gtceu.client.model.runtimegen;

import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.data.model.builder.RuntimeModelResources;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

public class ToolItemModelGenerator {

    private static final Set<ToolItemModelGenerator> MODELS = new HashSet<>();

    public static void reinitModels() {
        for (ToolItemModelGenerator model : MODELS) {
            RuntimeModelResources.emitItem(BuiltInRegistries.ITEM.getKey(model.item), model.toolType.modelLocation,
                    GTDynamicResourcePack::addResource);
        }
    }

    private final Item item;
    private final GTToolType toolType;

    protected ToolItemModelGenerator(Item item, GTToolType toolType) {
        this.item = item;
        this.toolType = toolType;
    }

    public static void add(Item item, GTToolType toolType) {
        MODELS.add(new ToolItemModelGenerator(item, toolType));
    }
}
