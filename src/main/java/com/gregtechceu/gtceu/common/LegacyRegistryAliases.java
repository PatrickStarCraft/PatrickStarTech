package com.gregtechceu.gtceu.common;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Registers identifier aliases for IDs accepted by the former missing-mapping remapper. */
public final class LegacyRegistryAliases {

    private static final String GREGICENG = "gregiceng";

    private LegacyRegistryAliases() {}

    public static void register() {
        Identifier legacyCoil = GTCEu.id("tungstensteel_coil_block");
        alias(BuiltInRegistries.BLOCK, legacyCoil, key(BuiltInRegistries.BLOCK, GTBlocks.COIL_RTMALLOY.get()));
        alias(BuiltInRegistries.ITEM, legacyCoil,
                key(BuiltInRegistries.ITEM, GTBlocks.COIL_RTMALLOY.get().asItem()));

        var steamMiner = GTMachines.STEAM_MINER.first();
        Identifier legacySteamMiner = GTCEu.id("steam_miner");
        alias(BuiltInRegistries.BLOCK, legacySteamMiner, key(BuiltInRegistries.BLOCK, steamMiner.getBlock()));
        alias(BuiltInRegistries.ITEM, legacySteamMiner, key(BuiltInRegistries.ITEM, steamMiner.getItem()));
        alias(BuiltInRegistries.BLOCK_ENTITY_TYPE, legacySteamMiner,
                key(BuiltInRegistries.BLOCK_ENTITY_TYPE, steamMiner.getBlockEntityType()));

        alias(BuiltInRegistries.ITEM, GTCEu.id("tungstensteel_fluid_cell"),
                key(BuiltInRegistries.ITEM, GTItems.FLUID_CELL_LARGE_TUNGSTEN_STEEL.get().asItem()));
        alias(BuiltInRegistries.ITEM, GTCEu.id("avanced_nanomuscle_chestplate"),
                key(BuiltInRegistries.ITEM, GTItems.NANO_CHESTPLATE_ADVANCED.get()));

        registerLegacyWirecutterAliases();
        registerLegacyMaterialAliases();

    }

    private static void registerLegacyWirecutterAliases() {
        for (String typeName : List.of("lv_wirecutter", "hv_wirecutter", "iv_wirecutter")) {
            GTToolType type = GTToolType.getTypes().get(typeName);
            if (type == null) continue;

            for (Material material : GTRegistries.MATERIALS) {
                if (!GTCEu.MOD_ID.equals(material.getID().getNamespace())) continue;
                var tool = GTMaterialItems.TOOL_ITEMS.get(material, type);
                if (tool == null || !tool.isBound()) continue;

                String legacyPath = typeName.substring(0, 2) + "_" + material.getName() + "_wirecutter";
                alias(BuiltInRegistries.ITEM, GTCEu.id(legacyPath),
                        key(BuiltInRegistries.ITEM, tool.asItem()));
            }
        }
    }

    private static void registerLegacyMaterialAliases() {
        if (GTMaterialBlocks.MATERIAL_BLOCKS == null || GTMaterialItems.MATERIAL_ITEMS == null) return;

        for (TagPrefix prefix : TagPrefix.values()) {
            String prefixName = FormattingUtil.toLowerCaseUnderscore(prefix.name);
            for (Material material : GTRegistries.MATERIALS) {
                if (!GTCEu.MOD_ID.equals(material.getID().getNamespace())) continue;

                String legacyPath = prefix.invertedName
                        ? prefixName + "_" + material.getName()
                        : material.getName() + "_" + prefixName;
                Identifier legacyId = GTCEu.id(legacyPath);
                BlockEntry<? extends Block> block = GTMaterialBlocks.MATERIAL_BLOCKS.get(prefix, material);
                if (block != null && block.isBound()) {
                    alias(BuiltInRegistries.BLOCK, legacyId, key(BuiltInRegistries.BLOCK, block.get()));
                    alias(BuiltInRegistries.ITEM, legacyId, key(BuiltInRegistries.ITEM, block.asItem()));
                    continue;
                }

                ItemEntry<? extends Item> item = GTMaterialItems.MATERIAL_ITEMS.get(prefix, material);
                if (item != null && item.isBound()) {
                    alias(BuiltInRegistries.ITEM, legacyId, key(BuiltInRegistries.ITEM, item.get()));
                }
            }
        }
    }

    private static void registerLegacyGregicengAliases() {
        aliasGregicengBuffers("input_buffer", "dual_input_hatch", GTMachines.DUAL_IMPORT_HATCH);
        aliasGregicengBuffers("output_buffer", "dual_output_hatch", GTMachines.DUAL_EXPORT_HATCH);
    }

    private static void aliasGregicengMachine(String legacyPath, MachineDefinition target) {
        Identifier legacyId = Identifier.fromNamespaceAndPath(GREGICENG, legacyPath);
        alias(BuiltInRegistries.BLOCK, legacyId, key(BuiltInRegistries.BLOCK, target.getBlock()));
        alias(BuiltInRegistries.BLOCK_ENTITY_TYPE, legacyId,
                key(BuiltInRegistries.BLOCK_ENTITY_TYPE, target.getBlockEntityType()));
        alias(BuiltInRegistries.ITEM, legacyId, key(BuiltInRegistries.ITEM, target.getItem()));
    }

    private static void aliasGregicengBuffers(String legacyPrefix, String targetPrefix,
                                              MachineDefinition[] targets) {
        for (MachineDefinition target : targets) {
            Identifier targetBlock = key(BuiltInRegistries.BLOCK, target.getBlock());
            if (targetBlock == null || !GTCEu.MOD_ID.equals(targetBlock.getNamespace()) ||
                    !targetBlock.getPath().contains(targetPrefix)) continue;

            Identifier legacyBlock = Identifier.fromNamespaceAndPath(GREGICENG,
                    targetBlock.getPath().replace(targetPrefix, legacyPrefix));
            alias(BuiltInRegistries.BLOCK, legacyBlock, targetBlock);

            Identifier targetItem = key(BuiltInRegistries.ITEM, target.getItem());
            if (targetItem != null) {
                Identifier legacyItem = Identifier.fromNamespaceAndPath(GREGICENG,
                        targetItem.getPath().replace(targetPrefix, legacyPrefix));
                alias(BuiltInRegistries.ITEM, legacyItem, targetItem);
            }

            Identifier targetBlockEntity = key(BuiltInRegistries.BLOCK_ENTITY_TYPE, target.getBlockEntityType());
            if (targetBlockEntity != null) {
                Identifier legacyBlockEntity = Identifier.fromNamespaceAndPath(GREGICENG,
                        targetBlockEntity.getPath().replace(targetPrefix, legacyPrefix));
                alias(BuiltInRegistries.BLOCK_ENTITY_TYPE, legacyBlockEntity, targetBlockEntity);
            }
        }
    }

    private static <T> void alias(Registry<T> registry, Identifier legacyId, @Nullable Identifier targetId) {
        if (targetId == null || registry.containsKey(legacyId) || !registry.containsKey(targetId)) return;

        Identifier resolvedId = registry.resolve(legacyId);
        if (resolvedId.equals(legacyId)) {
            registry.addAlias(legacyId, targetId);
        } else if (!resolvedId.equals(targetId)) {
            GTCEu.LOGGER.warn("Keeping existing registry alias {} -> {} instead of {}", legacyId, resolvedId, targetId);
        }
    }

    private static <T> @Nullable Identifier key(Registry<T> registry, @Nullable T value) {
        return value == null ? null : registry.getKey(value);
    }
}
