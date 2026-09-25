package com.gregtechceu.gtceu.core;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.ItemMaterialData;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.OreProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.MaterialToolTier;
import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorage;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.forge.GTClientFluidTypeExtensions;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.mixins.BlockBehaviourAccessor;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.LimitCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.Tags;

import com.tterrag.registrate.util.entry.BlockEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class MixinHelpers {

    public static <T> void generateGTDynamicTags(Map<Identifier, List<TagLoader.EntryWithSource>> tagMap,
                                                 Registry<T> registry) {
        if (registry == BuiltInRegistries.ITEM) {
            ItemMaterialData.MATERIAL_ENTRY_ITEM_MAP.forEach((entry, itemLikes) -> {
                if (itemLikes.isEmpty()) return;
                var material = entry.material();
                var entries = itemLikes.stream().map(MixinHelpers::makeItemEntry).collect(toArrayList());

                var prefixTagKeys = entry.tagPrefix().getAllItemTags(material);
                for (TagKey<Item> prefixTag : prefixTagKeys) {
                    tagMap.computeIfAbsent(prefixTag.location(), path -> new ArrayList<>()).addAll(entries);
                }
                for (TagKey<Item> materialTag : material.getItemTags()) {
                    tagMap.computeIfAbsent(materialTag.location(), path -> new ArrayList<>()).addAll(entries);
                }

                if (material.hasProperty(PropertyKey.TOOL) && isToolRepairMaterial(entry)) {
                    tagMap.computeIfAbsent(MaterialToolTier.getRepairItemsTag(material).location(),
                            path -> new ArrayList<>()).addAll(entries);
                }

                if (entry.tagPrefix() == TagPrefix.crushed && material.hasProperty(PropertyKey.ORE)) {
                    OreProperty ore = material.getPropertyOrThrow(PropertyKey.ORE);
                    if (!ore.hasWashedInFluid()) return;
                    Material washedIn = ore.getWashedIn().first();
                    Identifier generalTag = CustomTags.CHEM_BATH_WASHABLE.location();
                    Identifier specificTag = generalTag.withSuffix("/" + washedIn.getName());

                    tagMap.computeIfAbsent(generalTag, path -> new ArrayList<>()).addAll(entries);
                    tagMap.computeIfAbsent(specificTag, path -> new ArrayList<>()).addAll(entries);
                }
            });

            GTMaterialItems.TOOL_ITEMS.rowMap().forEach((material, map) -> {
                var repairTag = MaterialToolTier.getRepairItemsTag(material).location();
                map.values().forEach(item -> {
                    if (item == null) return;
                    Item registeredItem = item.get();
                    var entry = makeItemEntry(registeredItem);
                    tagMap.computeIfAbsent(repairTag, path -> new ArrayList<>()).add(entry);
                    if (registeredItem instanceof IGTTool tool) {
                        for (TagKey<Item> tag : tool.getToolType().itemTags) {
                            tagMap.computeIfAbsent(tag.location(), path -> new ArrayList<>()).add(entry);
                        }
                    }
                });
            });

            GTMaterialItems.ARMOR_ITEMS.rowMap().forEach((material, map) -> {
                map.forEach((type, item) -> {
                    if (item != null) {
                        var entry = new TagLoader.EntryWithSource(TagEntry.element(item.getId()),
                                GTValues.CUSTOM_TAG_SOURCE);
                        tagMap.computeIfAbsent(ItemTags.TRIMMABLE_ARMOR.location(), $ -> new ArrayList<>())
                                .add(entry);
                        var armorTag = switch (type) {
                            case HELMET -> net.minecraft.tags.ItemTags.HEAD_ARMOR.location();
                            case CHESTPLATE -> net.minecraft.tags.ItemTags.CHEST_ARMOR.location();
                            case LEGGINGS -> net.minecraft.tags.ItemTags.LEG_ARMOR.location();
                            case BOOTS -> net.minecraft.tags.ItemTags.FOOT_ARMOR.location();
                            case BODY -> null;
                        };
                        if (armorTag != null) {
                            tagMap.computeIfAbsent(armorTag, $ -> new ArrayList<>()).add(entry);
                        }
                    }
                });
            });

            if (!GTCEu.Mods.isAE2Loaded()) {
                return;
            }
            // If AE2 is loaded, add the Fluid P2P attunement tag to all the buckets
            var p2pFluidAttunements = Identifier.fromNamespaceAndPath(GTValues.MODID_APPENG,
                    "p2p_attunements/fluid_p2p_tunnel");
            for (Material material : GTRegistries.MATERIALS) {
                FluidProperty property = material.getProperty(PropertyKey.FLUID);
                if (property == null) {
                    continue;
                }
                for (FluidStorageKey key : FluidStorageKey.allKeys()) {
                    Fluid fluid = property.get(key);
                    if (fluid == null || fluid.getBucket() == Items.AIR) {
                        continue;
                    }
                    var entry = makeItemEntry(fluid.getBucket());
                    tagMap.computeIfAbsent(p2pFluidAttunements, path -> new ArrayList<>()).add(entry);
                }
            }
        } else if (registry == BuiltInRegistries.BLOCK) {
            for (int toolLevel = 0; toolLevel < CustomTags.INCORRECT_FOR_GT_TOOL_TIERS.length; toolLevel++) {
                Identifier incorrectTag = CustomTags.INCORRECT_FOR_GT_TOOL_TIERS[toolLevel].location();
                for (int requiredTier = toolLevel + 1; requiredTier < CustomTags.TOOL_TIERS.length; requiredTier++) {
                    tagMap.computeIfAbsent(incorrectTag, path -> new ArrayList<>())
                            .add(makeOptionalTagEntry(CustomTags.TOOL_TIERS[requiredTier]));
                }
            }

            ItemMaterialData.MATERIAL_ENTRY_BLOCK_MAP.forEach((entry, blocks) -> {
                if (blocks.isEmpty()) return;
                var material = entry.material();

                var entries = blocks.stream().map(MixinHelpers::makeBlockEntry).collect(toArrayList());
                var materialTags = entry.tagPrefix().getAllBlockTags(material);
                for (TagKey<Block> materialTag : materialTags) {
                    tagMap.computeIfAbsent(materialTag.location(), path -> new ArrayList<>()).addAll(entries);
                }
                // Add tool tags
                if (!entry.isIgnored() && !entry.tagPrefix().miningToolTag().isEmpty()) {
                    tagMap.computeIfAbsent(CustomTags.TOOL_TIERS[material.getBlockHarvestLevel()].location(),
                            path -> new ArrayList<>()).addAll(entries);
                    if (material.hasProperty(PropertyKey.WOOD)) {
                        // Wood blocks with this tag always allow a Wrench, but only allow an Axe if the config is
                        // not set. Pickaxe is never allowed (special case)
                        if (entry.tagPrefix().miningToolTag()
                                .contains(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)) {
                            tagMap.computeIfAbsent(CustomTags.MINEABLE_WITH_WRENCH.location(),
                                    path -> new ArrayList<>()).addAll(entries);
                            if (!ConfigHolder.INSTANCE.machines.requireGTToolsForBlocks) {
                                tagMap.computeIfAbsent(BlockTags.MINEABLE_WITH_AXE.location(),
                                        path -> new ArrayList<>())
                                        .addAll(entries);
                            }
                        } else {
                            // Other wood stuff should still get the Axe tag
                            tagMap.computeIfAbsent(BlockTags.MINEABLE_WITH_AXE.location(), path -> new ArrayList<>())
                                    .addAll(entries);
                        }
                    } else {
                        for (var tag : entry.tagPrefix().miningToolTag()) {
                            tagMap.computeIfAbsent(tag.location(), path -> new ArrayList<>()).addAll(entries);
                        }
                    }
                }

                if (entry.tagPrefix() == TagPrefix.oreEndstone) {
                    // Make endstone-based ores dragon-immune
                    tagMap.computeIfAbsent(BlockTags.DRAGON_IMMUNE.location(), $ -> new ArrayList<>()).addAll(entries);
                }

                if (entry.tagPrefix() == TagPrefix.frameGt) {
                    tagMap.computeIfAbsent(CustomTags.SLOW_WALKABLE_BLOCKS.location(), path -> new ArrayList<>())
                            .addAll(entries);
                }
            });

            GTRegistries.MACHINES.forEach(machine -> {
                tagMap.computeIfAbsent(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH.location(),
                        path -> new ArrayList<>()).add(makeBlockEntry(machine.getBlock()));
            });

            // if config is NOT enabled, add the "configurable" mineability tags to the pickaxe tag
            if (!ConfigHolder.INSTANCE.machines.requireGTToolsForBlocks) {
                var tagList = tagMap.computeIfAbsent(BlockTags.MINEABLE_WITH_PICKAXE.location(),
                        path -> new ArrayList<>());

                tagList.add(makeTagEntry(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH));
                tagList.add(makeTagEntry(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WIRE_CUTTER));
            }
        } else if (registry == BuiltInRegistries.FLUID) {
            for (Material material : GTRegistries.MATERIALS) {
                FluidProperty property = material.getProperty(PropertyKey.FLUID);
                if (property == null) {
                    continue;
                }
                for (FluidStorageKey key : FluidStorageKey.allKeys()) {
                    Fluid fluid = property.get(key);
                    if (fluid == null) {
                        continue;
                    }
                    ItemMaterialData.FLUID_MATERIAL.put(fluid, material);

                    TagLoader.EntryWithSource entry = makeFluidEntry(fluid);

                    Identifier fluidIdTag = fluid.builtInRegistryHolder().key().identifier();
                    // NeoForge still consumes optional legacy fluid tag aliases in the "forge" namespace.
                    fluidIdTag = Identifier.fromNamespaceAndPath("forge", fluidIdTag.getPath());
                    tagMap.computeIfAbsent(fluidIdTag, path -> new ArrayList<>()).add(entry);
                    FluidState state;

                    if (fluid instanceof GTFluid gtFluid) {
                        state = gtFluid.getState();
                    } else {
                        state = key.getDefaultFluidState();
                    }
                    tagMap.computeIfAbsent(state.getTagKey().location(), path -> new ArrayList<>()).add(entry);

                    if (key.getExtraTag() != null) {
                        tagMap.computeIfAbsent(key.getExtraTag().location(), path -> new ArrayList<>()).add(entry);
                    }
                }
            }
        }
    }

    private static <T> Collector<T, ?, ArrayList<T>> toArrayList() {
        return Collectors.toCollection(ArrayList::new);
    }

    public static TagLoader.EntryWithSource makeItemEntry(Supplier<? extends Item> item) {
        return makeItemEntry(item.get());
    }

    public static TagLoader.EntryWithSource makeItemEntry(ItemLike item) {
        return makeElementEntry(item.asItem().builtInRegistryHolder().key().identifier());
    }

    public static TagLoader.EntryWithSource makeBlockEntry(Supplier<? extends Block> block) {
        return makeBlockEntry(block.get());
    }

    public static TagLoader.EntryWithSource makeBlockEntry(Block block) {
        return makeElementEntry(block.builtInRegistryHolder().key().identifier());
    }

    public static TagLoader.EntryWithSource makeFluidEntry(Fluid fluid) {
        return makeElementEntry(fluid.builtInRegistryHolder().key().identifier());
    }

    public static TagLoader.EntryWithSource makeElementEntry(Identifier id) {
        return new TagLoader.EntryWithSource(TagEntry.element(id), GTValues.CUSTOM_TAG_SOURCE);
    }

    public static TagLoader.EntryWithSource makeTagEntry(TagKey<?> tag) {
        return new TagLoader.EntryWithSource(TagEntry.tag(tag.location()), GTValues.CUSTOM_TAG_SOURCE);
    }

    private static TagLoader.EntryWithSource makeOptionalTagEntry(TagKey<?> tag) {
        return new TagLoader.EntryWithSource(TagEntry.optionalTag(tag.location()), GTValues.CUSTOM_TAG_SOURCE);
    }

    private static boolean isToolRepairMaterial(MaterialEntry entry) {
        Material material = entry.material();
        TagPrefix prefix = entry.tagPrefix();
        if (material.hasProperty(PropertyKey.WOOD)) {
            return prefix == TagPrefix.planks;
        }
        if (prefix == TagPrefix.plate) {
            return true;
        }
        if (material.hasProperty(PropertyKey.INGOT)) {
            return prefix == TagPrefix.ingot;
        }
        return material.hasProperty(PropertyKey.GEM) && prefix == TagPrefix.gem;
    }

    public static void generateGTDynamicLoot(Map<Identifier, LootTable> lootTables,
                                            HolderLookup.Provider registries) {
        GTBlockLoot blockLoot = new GTBlockLoot(registries);
        GTMaterialBlocks.MATERIAL_BLOCKS.rowMap().forEach((prefix, map) -> {
            if (TagPrefix.ORES.containsKey(prefix)) {
                final TagPrefix.OreType type = TagPrefix.ORES.get(prefix);
                map.forEach((material, blockEntry) -> {
                    Identifier lootTableId = blockEntry.getId().withPrefix("blocks/");
                    Block block = blockEntry.get();

                    // Registry reloads build loot tables before item default components are bound.
                    // LootItem only needs the item, so avoid creating an ItemStack here.
                    Item dropItem = ChemicalHelper.getItem(TagPrefix.rawOre, material);
                    if (dropItem == Items.AIR) dropItem = ChemicalHelper.getItem(TagPrefix.gem, material);
                    if (dropItem == Items.AIR) dropItem = ChemicalHelper.getItem(TagPrefix.dust, material);
                    int oreMultiplier = type.isDoubleDrops() ? 2 : 1;

                    LootTable.Builder builder = blockLoot.createSilkTouchDispatchTable(block,
                            blockLoot.applyExplosionDecay(block,
                                    LootItem.lootTableItem(dropItem)
                                            .apply(SetItemCountFunction
                                                    .setCount(ConstantValue.exactly(oreMultiplier)))));
                    // disable fortune for balance reasons. (for now, until we can think of a better solution.)
                    // .apply(ApplyBonusCount.addOreBonusCount(Enchantments.FORTUNE))));

                    LootPool.Builder pool = LootPool.lootPool();
                    boolean isEmpty = true;
                    for (MaterialStack secondaryMaterial : prefix.secondaryMaterials()) {
                        if (secondaryMaterial.material().hasProperty(PropertyKey.DUST)) {
                            pool.add(LootItem.lootTableItem(getGemItem(secondaryMaterial))
                                    .when(blockLoot.doesNotHaveSilkTouch())
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(0, 1)))
                                    .apply(LimitCount.limitCount(IntRange.range(0, 2)))
                                    .apply(ApplyExplosionDecay.explosionDecay()));
                            isEmpty = false;
                        }
                    }
                    if (!isEmpty) {
                        builder.withPool(pool);
                    }
                    lootTables.put(lootTableId, builder.setParamSet(LootContextParamSets.BLOCK).build());
                    setBlockLootTable(block, lootTableId);
                });
            } else {
                MixinHelpers.addMaterialBlockLootTables(lootTables, blockLoot, prefix, map);
            }
        });
        GTMaterialBlocks.CABLE_BLOCKS.rowMap().forEach((prefix, map) -> {
            MixinHelpers.addMaterialBlockLootTables(lootTables, blockLoot, prefix, map);
        });
        GTMaterialBlocks.FLUID_PIPE_BLOCKS.rowMap().forEach((prefix, map) -> {
            MixinHelpers.addMaterialBlockLootTables(lootTables, blockLoot, prefix, map);
        });
        GTMaterialBlocks.ITEM_PIPE_BLOCKS.rowMap().forEach((prefix, map) -> {
            MixinHelpers.addMaterialBlockLootTables(lootTables, blockLoot, prefix, map);
        });
        GTMaterialBlocks.SURFACE_ROCK_BLOCKS.forEach((material, blockEntry) -> {
            Identifier lootTableId = blockEntry.getId().withPrefix("blocks/");
            LootTable.Builder builder = blockLoot
                    .createSingleItemTable(ChemicalHelper.getItem(TagPrefix.dustTiny, material),
                            UniformGenerator.between(3, 5))
                    .apply(ApplyBonusCount.addUniformBonusCount(
                            registries.lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(Enchantments.FORTUNE)));
            lootTables.put(lootTableId, builder.setParamSet(LootContextParamSets.BLOCK).build());
            setBlockLootTable(blockEntry.get(), lootTableId);
        });
        GTRegistries.MACHINES.forEach(machine -> {
            Block block = machine.getBlock();
            Identifier id = machine.getId();
            Identifier lootTableId = BuiltInRegistries.BLOCK.getKey(block).withPrefix("blocks/");
            setBlockLootTable(block, lootTableId);
            lootTables.put(lootTableId,
                    blockLoot.createSingleItemTable(block).setParamSet(LootContextParamSets.BLOCK).build());
        });
    }

    private static void setBlockLootTable(Block block, Identifier id) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        ((BlockBehaviourAccessor) block).setDrops(Optional.of(key));
    }

    public static void addMaterialBlockLootTables(Map<Identifier, LootTable> lootTables, GTBlockLoot blockLoot,
                                                  TagPrefix prefix,
                                                  Map<Material, ? extends BlockEntry<? extends Block>> map) {
        map.forEach((material, blockEntry) -> {
            Identifier lootTableId = blockEntry.getId().withPrefix("blocks/");
            setBlockLootTable(blockEntry.get(), lootTableId);
            lootTables.put(lootTableId,
                    blockLoot.createSingleItemTable(blockEntry.get()).setParamSet(LootContextParamSets.BLOCK).build());
        });
    }

    private static Item getGemItem(MaterialStack materialStack) {
        if (materialStack.isEmpty()) return Items.AIR;

        Material material = materialStack.material();
        if (material.hasProperty(PropertyKey.GEM) && !TagPrefix.gem.isIgnored(material) &&
                materialStack.amount() == TagPrefix.gem.getMaterialAmount(material)) {
            return ChemicalHelper.getItem(TagPrefix.gem, material);
        }

        long amount = materialStack.amount();
        if (!material.hasProperty(PropertyKey.DUST) || amount <= 0) return Items.AIR;
        if (amount % GTValues.M == 0 || amount >= GTValues.M * 16) {
            return ChemicalHelper.getItem(TagPrefix.dust, material);
        }
        if ((amount * 4) % GTValues.M == 0 || amount >= GTValues.M * 8) {
            return ChemicalHelper.getItem(TagPrefix.dustSmall, material);
        }
        if ((amount * 9) >= GTValues.M) {
            return ChemicalHelper.getItem(TagPrefix.dustTiny, material);
        }
        return Items.AIR;
    }

    public static final class GTBlockLoot extends VanillaBlockLoot {

        public GTBlockLoot(HolderLookup.Provider registries) {
            super(registries);
        }

        public LootItemCondition.Builder doesNotHaveSilkTouch() {
            return super.doesNotHaveSilkTouch();
        }
    }

    public static void addFluidTexture(Material material, FluidStorage.FluidEntry value) {
        if (value != null) {
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(value.getFluid().get());
            if (extensions instanceof GTClientFluidTypeExtensions gtExtensions && value.getBuilder() != null) {
                gtExtensions.setFlowingTexture(value.getBuilder().flowing());
                gtExtensions.setStillTexture(value.getBuilder().still());
            }
        }
    }
}
