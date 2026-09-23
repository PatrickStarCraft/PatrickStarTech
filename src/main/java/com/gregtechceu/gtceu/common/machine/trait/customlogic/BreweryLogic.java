package com.gregtechceu.gtceu.common.machine.trait.customlogic;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.fluid.potion.PotionFluidHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.gregtechceu.gtceu.api.GTValues.*;

// TODO: Make these static recipes
@SuppressWarnings("deprecation")
public enum BreweryLogic implements GTRecipeType.ICustomRecipeLogic {

    INSTANCE;

    @Override
    public @Nullable GTRecipe createCustomRecipe(IRecipeCapabilityHolder holder) {
        PotionBrewing potionBrewing = getPotionBrewing(holder);
        if (potionBrewing == null) return null;

        var handlerLists = holder.getCapabilitiesForIO(IO.IN);
        if (handlerLists.isEmpty()) return null;
        List<RecipeHandlerList> distinct = new ArrayList<>();
        List<IRecipeHandler<?>> notDistinctItems = new ArrayList<>();
        List<IRecipeHandler<?>> notDistinctFluids = new ArrayList<>();

        for (var handlerList : handlerLists) {
            if (handlerList.isDistinct()) {
                distinct.add(handlerList);
            } else {
                notDistinctItems.addAll(handlerList.getCapability(ItemRecipeCapability.CAP));
                notDistinctFluids.addAll(handlerList.getCapability(FluidRecipeCapability.CAP));
            }
        }

        if (distinct.isEmpty() && notDistinctItems.isEmpty() && notDistinctFluids.isEmpty()) return null;

        List<ItemStack> itemStacks = new ArrayList<>();
        List<FluidStack> fluidStacks = new ArrayList<>();

        for (var handlerList : distinct) {
            itemStacks.clear();
            fluidStacks.clear();
            if (!collect(handlerList, itemStacks, fluidStacks)) continue;
            GTRecipe recipe = findRecipe(potionBrewing, itemStacks, fluidStacks);
            if (recipe != null) return recipe;
        }

        if (notDistinctItems.isEmpty() && notDistinctFluids.isEmpty()) return null;

        List<ItemStack> sharedItems = new ArrayList<>();
        List<FluidStack> sharedFluids = new ArrayList<>();
        collect(notDistinctItems, notDistinctFluids, sharedItems, sharedFluids);
        GTRecipe recipe = findRecipe(potionBrewing, sharedItems, sharedFluids);
        if (recipe != null) return recipe;

        for (var handlerList : distinct) {
            itemStacks.clear();
            fluidStacks.clear();
            collect(handlerList, itemStacks, fluidStacks);

            recipe = findRecipe(potionBrewing, sharedItems, fluidStacks);
            if (recipe != null) return recipe;

            recipe = findRecipe(potionBrewing, itemStacks, sharedFluids);
            if (recipe != null) return recipe;
        }

        return null;
    }

    private static @Nullable PotionBrewing getPotionBrewing(IRecipeCapabilityHolder holder) {
        if (holder instanceof IRecipeLogicMachine machine && machine.self().getLevel() instanceof ServerLevel level) {
            return level.potionBrewing();
        }
        return null;
    }

    private static @Nullable GTRecipe findRecipe(PotionBrewing potionBrewing, List<ItemStack> items,
                                                List<FluidStack> fluids) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            for (FluidStack fluid : fluids) {
                if (fluid.isEmpty()) continue;
                ItemStack potionInput = potionInput(fluid);
                if (!potionBrewing.hasMix(potionInput, item)) continue;

                ItemStack potionOutput = potionBrewing.mix(item.copyWithCount(1), potionInput);
                FluidStack outputFluid = PotionFluidHelper.getFluidFromPotionItem(potionOutput,
                        PotionFluidHelper.MB_PER_RECIPE);
                if (outputFluid.isEmpty()) continue;

                FluidStack inputFluid = fluid.copy();
                inputFluid.setAmount(PotionFluidHelper.MB_PER_RECIPE);
                return buildBreweryRecipe("potion_dynamic", item, inputFluid, outputFluid);
            }
        }
        return null;
    }

    private static ItemStack potionInput(FluidStack fluid) {
        ItemStack input = PotionFluidHelper.fillBottle(new ItemStack(Items.GLASS_BOTTLE), fluid);
        if (fluid.getFluid() == Fluids.WATER && input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion().isEmpty()) {
            input.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        }
        return input;
    }

    private static GTRecipe buildBreweryRecipe(String id, ItemStack ingredient, FluidStack fromFluid,
                                               FluidStack toFluid) {
        return GTRecipeTypes.BREWING_RECIPES.recipeBuilder(id)
                .inputItems(ingredient.copyWithCount(1))
                .inputFluids(fromFluid)
                .outputFluids(toFluid)
                .duration(400)
                .EUt(VHA[MV])
                .buildRawRecipe();
    }

    private static boolean collect(RecipeHandlerList rhl, List<ItemStack> itemStacks, List<FluidStack> fluidStacks) {
        return collect(rhl.getCapability(ItemRecipeCapability.CAP),
                rhl.getCapability(FluidRecipeCapability.CAP),
                itemStacks, fluidStacks);
    }

    private static boolean collect(List<IRecipeHandler<?>> itemHandlers, List<IRecipeHandler<?>> fluidHandlers,
                                   List<ItemStack> itemStacks, List<FluidStack> fluidStacks) {
        for (var handler : itemHandlers) {
            for (var content : handler.getContents()) {
                if (content instanceof ItemStack stack && !stack.isEmpty()) {
                    itemStacks.add(stack);
                }
            }
        }

        for (var handler : fluidHandlers) {
            for (var content : handler.getContents()) {
                if (content instanceof FluidStack stack && !stack.isEmpty()) {
                    fluidStacks.add(stack);
                }
            }
        }

        return !(itemStacks.isEmpty() || fluidStacks.isEmpty());
    }

    @Override
    public void buildRepresentativeRecipes() {
        PotionBrewing potionBrewing = getRepresentativePotionBrewing();
        int index = 0;
        for (Potion fromPotion : BuiltInRegistries.POTION) {
            ItemStack potionInput = new ItemStack(Items.POTION);
            potionInput.set(DataComponents.POTION_CONTENTS,
                    new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(fromPotion)));
            FluidStack fromFluid = PotionFluidHelper.getFluidFromPotion(fromPotion,
                    PotionFluidHelper.MB_PER_RECIPE);

            for (var item : BuiltInRegistries.ITEM) {
                ItemStack ingredient = new ItemStack(item);
                if (!potionBrewing.hasMix(potionInput, ingredient)) continue;
                if (hasConcreteBrewingRecipe(potionBrewing, potionInput, ingredient)) continue;

                ItemStack potionOutput = potionBrewing.mix(ingredient.copy(), potionInput.copy());
                FluidStack toFluid = PotionFluidHelper.getFluidFromPotionItem(potionOutput,
                        PotionFluidHelper.MB_PER_RECIPE);
                if (toFluid.isEmpty()) continue;

                Potion outputPotion = PotionFluidHelper.getPotionFromItemStack(potionOutput);
                String name = outputPotion == null ?
                        toFluid.getFluid().builtInRegistryHolder().key().identifier().getPath() :
                        potionOutput.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                                .getName("").getString();
                GTRecipe recipe = buildBreweryRecipe("potion_vanilla_" + name + "_" + index++, ingredient,
                        fromFluid, toFluid);
                recipe.setId(recipe.getId().withPrefix("/"));
                GTRecipeTypes.BREWING_RECIPES.addToMainCategory(recipe);
            }
        }

        for (var brewingRecipe : potionBrewing.getRecipes()) {
            if (!(brewingRecipe instanceof BrewingRecipe impl)) {
                continue;
            }

            FluidIngredient fromFluid = PotionFluidHelper.getPotionFluidIngredientFrom(impl.getInput(),
                    PotionFluidHelper.MB_PER_RECIPE);
            FluidStack toFluid = PotionFluidHelper.getFluidFromPotionItem(impl.getOutput(),
                    PotionFluidHelper.MB_PER_RECIPE);

            String name = toFluid.getFluid().builtInRegistryHolder().key().identifier().getPath();
            Potion output = PotionFluidHelper.getPotionFromItemStack(impl.getOutput());
            if (output != null) {
                name = impl.getOutput().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                        .getName("").getString();
            }

            GTRecipe recipe = GTRecipeTypes.BREWING_RECIPES.recipeBuilder("potion_forge_" + name + "_" + index++)
                    .inputItems(impl.getIngredient())
                    .inputFluids(fromFluid)
                    .outputFluids(toFluid)
                    .duration(400)
                    .EUt(VHA[MV])
                    .buildRawRecipe();
            // for EMI to detect it's a synthetic recipe (not ever in JSON)
            recipe.setId(recipe.getId().withPrefix("/"));
            GTRecipeTypes.BREWING_RECIPES.addToMainCategory(recipe);
        }
    }

    private static boolean hasConcreteBrewingRecipe(PotionBrewing potionBrewing, ItemStack potionInput,
                                                    ItemStack ingredient) {
        for (var recipe : potionBrewing.getRecipes()) {
            if (recipe instanceof BrewingRecipe && !recipe.getOutput(potionInput, ingredient).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static PotionBrewing getRepresentativePotionBrewing() {
        var server = GTCEu.getMinecraftServer();
        if (server != null) return server.potionBrewing();
        return PotionBrewing.bootstrap(FeatureFlags.DEFAULT_FLAGS,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }
}
