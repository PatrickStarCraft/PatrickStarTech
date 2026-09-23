package com.gregtechceu.gtceu.data.tags;

import com.gregtechceu.gtceu.common.block.StoneTypes;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;

public class BlockTagLoader {

    public static void init(RegistrateTagsProvider.Impl<Block> provider) {
        tag(provider, CustomTags.CONCRETE_BLOCK)
                .add(Blocks.CONCRETE.white(), Blocks.CONCRETE.orange(), Blocks.CONCRETE.magenta(), Blocks.CONCRETE.lightBlue(),
                        Blocks.CONCRETE.yellow(), Blocks.CONCRETE.lime(), Blocks.CONCRETE.pink(), Blocks.CONCRETE.gray(),
                        Blocks.CONCRETE.lightGray(), Blocks.CONCRETE.cyan(), Blocks.CONCRETE.purple(), Blocks.CONCRETE.blue(),
                        Blocks.CONCRETE.brown(), Blocks.CONCRETE.green(), Blocks.CONCRETE.red(), Blocks.CONCRETE.black());
        tag(provider, CustomTags.CONCRETE_POWDER_BLOCK)
                .add(Blocks.CONCRETE_POWDER.white(), Blocks.CONCRETE_POWDER.orange(), Blocks.CONCRETE_POWDER.magenta(),
                        Blocks.CONCRETE_POWDER.lightBlue(), Blocks.CONCRETE_POWDER.yellow(), Blocks.CONCRETE_POWDER.lime(),
                        Blocks.CONCRETE_POWDER.pink(), Blocks.CONCRETE_POWDER.gray(), Blocks.CONCRETE_POWDER.lightGray(),
                        Blocks.CONCRETE_POWDER.cyan(), Blocks.CONCRETE_POWDER.purple(), Blocks.CONCRETE_POWDER.blue(),
                        Blocks.CONCRETE_POWDER.brown(), Blocks.CONCRETE_POWDER.green(), Blocks.CONCRETE_POWDER.red(),
                        Blocks.CONCRETE_POWDER.black());

        var speedConcretes = tag(provider, CustomTags.VERY_FAST_WALKABLE_BLOCKS);
        speedConcretes.add(GTBlocks.LIGHT_CONCRETE.get(), GTBlocks.DARK_CONCRETE.get());

        GTBlocks.STONE_BLOCKS.column(StoneTypes.CONCRETE_LIGHT)
                .forEach((type, block) -> speedConcretes.add(block.get()));
        GTBlocks.STONE_BLOCKS.column(StoneTypes.CONCRETE_DARK)
                .forEach((type, block) -> speedConcretes.add(block.get()));

        var studs = tag(provider, CustomTags.FAST_WALKABLE_BLOCKS);
        GTBlocks.STUDS.forEach((color, block) -> studs.add(block.get()));

        tag(provider, CustomTags.ENDSTONE_ORE_REPLACEABLES).add(Blocks.END_STONE);

        tag(provider, CustomTags.TALL_PLANTS)
                .add(Blocks.SUGAR_CANE, Blocks.CACTUS)
                .add(Blocks.TALL_GRASS, Blocks.LARGE_FERN)
                .add(Blocks.BAMBOO, Blocks.BAMBOO_SAPLING)
                .add(Blocks.CHORUS_FLOWER, Blocks.CHORUS_PLANT)
                .add(Blocks.VINE,
                        Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT,
                        Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT)
                .add(Blocks.PITCHER_CROP, Blocks.SUNFLOWER, Blocks.LILAC, Blocks.PEONY, Blocks.ROSE_BUSH)
                .addTag(BlockTags.CAVE_VINES)
                .addOptionalTag(Identifier.fromNamespaceAndPath("forge", "cacti"))
                .addOptionalTag(Identifier.fromNamespaceAndPath("forge", "crops/cactus"))
                .addOptionalTag(Identifier.fromNamespaceAndPath("forge", "crops/sugar_cane"))
                .addOptionalTag(Identifier.fromNamespaceAndPath("forge", "reeds"));

        tag(provider, BlockTags.REPLACEABLE)
                .add(GTMaterials.Oil.getFluid().defaultFluidState().createLegacyBlock().getBlock())
                .add(GTMaterials.OilLight.getFluid().defaultFluidState().createLegacyBlock().getBlock())
                .add(GTMaterials.OilHeavy.getFluid().defaultFluidState().createLegacyBlock().getBlock())
                .add(GTMaterials.RawOil.getFluid().defaultFluidState().createLegacyBlock().getBlock())
                .add(GTMaterials.NaturalGas.getFluid().defaultFluidState().createLegacyBlock().getBlock());

        tag(provider, BlockTags.MINEABLE_WITH_AXE)
                .add(GTMachines.WOODEN_DRUM.getBlock())
                .add(GTMachines.WOODEN_CRATE.getBlock());

        // always add the wrench/pickaxe tag as a valid tag to mineable/wrench etc.
        tag(provider, CustomTags.MINEABLE_WITH_WRENCH)
                .addTag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .add(Blocks.PISTON, Blocks.PISTON_HEAD, Blocks.STICKY_PISTON, Blocks.OBSERVER, Blocks.REDSTONE_LAMP,
                        Blocks.REDSTONE_BLOCK, Blocks.IRON_DOOR, Blocks.IRON_TRAPDOOR,
                        Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
                        Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER,
                        Blocks.LIGHTNING_ROD.weathering().unaffected(), Blocks.DAYLIGHT_DETECTOR, Blocks.BELL);
        tag(provider, CustomTags.MINEABLE_WITH_WIRE_CUTTER)
                .addTag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WIRE_CUTTER);

        tag(provider, CustomTags.MINEABLE_WITH_SHEARS)
                .addTag(BlockTags.LEAVES)
                .addTag(BlockTags.WOOL)
                .add(Blocks.COBWEB, Blocks.DEAD_BUSH, Blocks.FERN, Blocks.GLOW_LICHEN, Blocks.HANGING_ROOTS,
                        Blocks.LARGE_FERN, Blocks.NETHER_SPROUTS, Blocks.SEAGRASS, Blocks.SHORT_GRASS, Blocks.SMALL_DRIPLEAF,
                        Blocks.TALL_GRASS, Blocks.TALL_SEAGRASS, Blocks.TRIPWIRE, Blocks.TWISTING_VINES, Blocks.VINE,
                        Blocks.WEEPING_VINES);

        tag(provider, CustomTags.CLEANROOM_FLOORS)
                .addOptionalTag(Identifier.fromNamespaceAndPath("elevatorid", "elevators"))
                .addOptional(Identifier.fromNamespaceAndPath("enderio", "travel_anchor"))
                .addOptional(Identifier.fromNamespaceAndPath("rftoolsutility", "matter_transmitter"))
                .addOptional(Identifier.fromNamespaceAndPath("rftoolsutility", "matter_receiver"))
                .addOptional(Identifier.fromNamespaceAndPath("rftoolsutility", "dialing_device"))
                .addOptional(Identifier.fromNamespaceAndPath("travelanchors", "travel_anchor"));

        tag(provider, CustomTags.CHARCOAL_PILE_IGNITER_WALLS)
                .addTag(BlockTags.DIRT) // any dirt blocks
                .remove(Blocks.MOSS_BLOCK, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS) // except moss and mud
                .add(Blocks.DIRT_PATH) // path blocks
                .addTag(Tags.Blocks.SANDS).addTag(BlockTags.SAND) // any sand blocks
                .addTag(BlockTags.TERRACOTTA); // any terracotta

        tag(provider, CustomTags.CLEANROOM_DOORS).add(Blocks.IRON_DOOR).addTag(BlockTags.WOODEN_DOORS);
    }

    private static RegistryTagAppender<Block> tag(RegistrateTagsProvider.Impl<Block> provider, TagKey<Block> tag) {
        return RegistryTagAppender.create(provider, tag, block -> block.builtInRegistryHolder().key());
    }
}
