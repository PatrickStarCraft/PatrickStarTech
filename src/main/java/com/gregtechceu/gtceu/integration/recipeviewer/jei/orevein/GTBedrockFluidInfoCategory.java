package com.gregtechceu.gtceu.integration.recipeviewer.jei.orevein;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.OreVeinRecipeWidget;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

import brachy.modularui.integration.jei.recipe.ModularUIRecipeCategory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class GTBedrockFluidInfoCategory extends ModularUIRecipeCategory<BedrockFluidDefinition> {

    public final static IRecipeType<BedrockFluidDefinition> RECIPE_TYPE = IRecipeType.create(
            GTCEu.id("bedrock_fluid_diagram"), BedrockFluidDefinition.class);

    private final IDrawable icon;

    public GTBedrockFluidInfoCategory(IJeiHelpers helpers) {
        super(OreVeinRecipeWidget::new, v -> ClientProxy.CLIENT_FLUID_VEINS.inverse().get(v));
        this.icon = helpers.getGuiHelper()
                .createDrawableItemStack(GTMaterials.Oil.getFluid().getBucket().asItem().getDefaultInstance());
    }

    public static void registerRecipes(IRecipeRegistration registry) {
        registry.addRecipes(RECIPE_TYPE, ClientProxy.CLIENT_FLUID_VEINS.values().stream()
                .toList());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(GTItems.PROSPECTOR_HV.asStack(), RECIPE_TYPE);
        registration.addRecipeCatalyst(GTItems.PROSPECTOR_LuV.asStack(), RECIPE_TYPE);
    }

    @NotNull
    @Override
    public IRecipeType<BedrockFluidDefinition> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public int getHeight() {
        return 250;
    }

    @Override
    public int getWidth() {
        return 180;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BedrockFluidDefinition fluid,
                          IFocusGroup focuses) {
        super.setRecipe(builder, fluid, focuses);
        Arrays.stream(OreVeinRecipeWidget.getDimensionMarkers(fluid.dimensionFilter))
                .forEach(v -> builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                        .addIngredient(VanillaTypes.ITEM_STACK, v.getIcon()));

        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addIngredient(NeoForgeTypes.FLUID_STACK,
                new FluidStack(fluid.getStoredFluid().get(), 1000));
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.jei.bedrock_fluid_diagram");
    }

    @NotNull
    @Override
    public IDrawable getIcon() {
        return icon;
    }
}
