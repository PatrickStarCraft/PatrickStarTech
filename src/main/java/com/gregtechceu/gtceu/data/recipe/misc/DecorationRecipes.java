package com.gregtechceu.gtceu.data.recipe.misc;

import com.gregtechceu.gtceu.common.data.GTRecipeCategories;
import com.gregtechceu.gtceu.data.recipe.GeneratedRecipe;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.block;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.plate;
import static com.gregtechceu.gtceu.common.data.GTBlocks.LARGE_METAL_SHEETS;
import static com.gregtechceu.gtceu.common.data.GTBlocks.METAL_SHEETS;
import static com.gregtechceu.gtceu.common.data.GTBlocks.STUDS;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.CHEMICAL_BATH_RECIPES;

public class DecorationRecipes {

    private DecorationRecipes() {}

    public static void init(Consumer<GeneratedRecipe> provider) {
        assemblerRecipes(provider);
        dyeRecipes(provider);
        copperOxidationRecipes(provider);
    }

    private static void assemblerRecipes(Consumer<GeneratedRecipe> provider) {
        ASSEMBLER_RECIPES.recipeBuilder("metal_sheet_white")
                .inputItems(block, Concrete, 5)
                .inputItems(plate, Iron, 2)
                .circuitMeta(8)
                .outputItems(METAL_SHEETS.get(DyeColor.WHITE), 32)
                .EUt(4).duration(20)
                .addMaterialInfo(true).save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("large_metal_sheet_white")
                .inputItems(block, Concrete, 5)
                .inputItems(plate, Iron, 4)
                .circuitMeta(9)
                .outputItems(LARGE_METAL_SHEETS.get(DyeColor.WHITE), 32)
                .EUt(4).duration(20)
                .addMaterialInfo(true).save(provider);

        ASSEMBLER_RECIPES.recipeBuilder("studs_black")
                .inputItems(block, Concrete, 3)
                .inputItems(plate, Rubber, 3)
                .circuitMeta(8)
                .outputItems(STUDS.get(DyeColor.BLACK), 32)
                .EUt(4).duration(20)
                .addMaterialInfo(true).save(provider);
    }

    private static void dyeRecipes(Consumer<GeneratedRecipe> provider) {
        for (DyeColor color : DyeColor.values()) {
            CHEMICAL_BATH_RECIPES.recipeBuilder("metal_sheet_%s".formatted(color.getName()))
                    .inputItems(METAL_SHEETS.get(DyeColor.WHITE).asStack())
                    .inputFluids(DYE_MATERIALS.get(color).getFluid(9))
                    .outputItems(METAL_SHEETS.get(color))
                    .EUt(2).duration(10)
                    .category(GTRecipeCategories.CHEM_DYES)
                    .save(provider);

            CHEMICAL_BATH_RECIPES.recipeBuilder("large_metal_sheet_%s".formatted(color.getName()))
                    .inputItems(LARGE_METAL_SHEETS.get(DyeColor.WHITE).asStack())
                    .inputFluids(DYE_MATERIALS.get(color).getFluid(9))
                    .outputItems(LARGE_METAL_SHEETS.get(color))
                    .EUt(2).duration(10)
                    .category(GTRecipeCategories.CHEM_DYES)
                    .save(provider);

            CHEMICAL_BATH_RECIPES.recipeBuilder("studs_%s".formatted(color.getName()))
                    .inputItems(STUDS.get(DyeColor.BLACK).asStack())
                    .inputFluids(DYE_MATERIALS.get(color).getFluid(9))
                    .outputItems(STUDS.get(color))
                    .EUt(2).duration(10)
                    .category(GTRecipeCategories.CHEM_DYES)
                    .save(provider);
        }
    }

    private static void copperOxidationRecipes(Consumer<GeneratedRecipe> provider) {
        registerOxidationChain(provider, "copper_block", Items.COPPER_BLOCK.weathering().unaffected(), Items.COPPER_BLOCK.weathering().exposed(),
                Items.COPPER_BLOCK.weathering().weathered(), Items.COPPER_BLOCK.weathering().oxidized());
        registerOxidationChain(provider, "cut_copper", Items.CUT_COPPER.weathering().unaffected(), Items.CUT_COPPER.weathering().exposed(),
                Items.CUT_COPPER.weathering().weathered(), Items.CUT_COPPER.weathering().oxidized());
        registerOxidationChain(provider, "cut_copper_stairs", Items.CUT_COPPER_STAIRS.weathering().unaffected(), Items.CUT_COPPER_STAIRS.weathering().exposed(),
                Items.CUT_COPPER_STAIRS.weathering().weathered(), Items.CUT_COPPER_STAIRS.weathering().oxidized());
        registerOxidationChain(provider, "cut_copper_slab", Items.CUT_COPPER_SLAB.weathering().unaffected(), Items.CUT_COPPER_SLAB.weathering().exposed(),
                Items.CUT_COPPER_SLAB.weathering().weathered(), Items.CUT_COPPER_SLAB.weathering().oxidized());

        // Waxing recipes
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_copper_block")
                .inputItems(Items.COPPER_BLOCK.weathering().unaffected())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.COPPER_BLOCK.waxed().unaffected())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_exposed_copper")
                .inputItems(Items.COPPER_BLOCK.weathering().exposed())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.COPPER_BLOCK.waxed().exposed())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_weathered_copper")
                .inputItems(Items.COPPER_BLOCK.weathering().weathered())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.COPPER_BLOCK.waxed().weathered())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_oxidized_copper")
                .inputItems(Items.COPPER_BLOCK.weathering().oxidized())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.COPPER_BLOCK.waxed().oxidized())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_cut_copper")
                .inputItems(Items.CUT_COPPER.weathering().unaffected())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER.waxed().unaffected())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_exposed_cut_copper")
                .inputItems(Items.CUT_COPPER.weathering().exposed())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER.waxed().exposed())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_weathered_cut_copper")
                .inputItems(Items.CUT_COPPER.weathering().weathered())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER.waxed().weathered())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_oxidized_cut_copper")
                .inputItems(Items.CUT_COPPER.weathering().oxidized())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER.waxed().oxidized())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_cut_copper_stairs")
                .inputItems(Items.CUT_COPPER_STAIRS.weathering().unaffected())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_STAIRS.waxed().unaffected())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_exposed_cut_copper_stairs")
                .inputItems(Items.CUT_COPPER_STAIRS.weathering().exposed())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_STAIRS.waxed().exposed())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_weathered_cut_copper_stairs")
                .inputItems(Items.CUT_COPPER_STAIRS.weathering().weathered())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_STAIRS.waxed().weathered())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_oxidized_cut_copper_stairs")
                .inputItems(Items.CUT_COPPER_STAIRS.weathering().oxidized())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_STAIRS.waxed().oxidized())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_cut_copper_slab")
                .inputItems(Items.CUT_COPPER_SLAB.weathering().unaffected())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_SLAB.waxed().unaffected())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_exposed_cut_copper_slab")
                .inputItems(Items.CUT_COPPER_SLAB.weathering().exposed())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_SLAB.waxed().exposed())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_weathered_cut_copper_slab")
                .inputItems(Items.CUT_COPPER_SLAB.weathering().weathered())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_SLAB.waxed().weathered())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
        CHEMICAL_BATH_RECIPES.recipeBuilder("waxing_oxidized_cut_copper_slab")
                .inputItems(Items.CUT_COPPER_SLAB.weathering().oxidized())
                .inputFluids(Wax, L / 2)
                .outputItems(Items.CUT_COPPER_SLAB.waxed().oxidized())
                .EUt(VA[ULV]).duration(10)
                .save(provider);
    }

    private static final String[] OXIDATION_STAGES = { "raw", "exposed", "weathered", "oxidized" };

    private static void registerOxidationChain(Consumer<GeneratedRecipe> provider, String name, Item... items) {
        for (int i = 0; i < items.length - 1; i++) {
            CHEMICAL_BATH_RECIPES
                    .recipeBuilder(
                            "%s_to_%s_%s_oxidation".formatted(OXIDATION_STAGES[i], OXIDATION_STAGES[i + 1], name))
                    .inputItems(items[i])
                    .inputFluids(Oxygen, 100)
                    .outputItems(items[i + 1])
                    .EUt(VA[ULV]).duration(10)
                    .save(provider);
        }
    }
}
