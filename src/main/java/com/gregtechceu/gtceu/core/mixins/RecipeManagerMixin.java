package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.MapIngredientPool;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeManagerHandler;
import com.gregtechceu.gtceu.common.item.armor.PowerlessJetpack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.HashMap;

/**
 * Only fires if KubeJS is not interacting with GT recipes.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Inject(method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "TAIL"))
    private void gtceu$cloneVanillaRecipes(RecipeMap map, ResourceManager resourceManager,
                                           ProfilerFiller profiler, CallbackInfo ci) {
        // The client's initial recipe reload can finish before data-pack defaults are bound
        // to every item. Proxy conversion assembles result stacks and requires those defaults.
        if (BuiltInRegistries.ITEM.stream().anyMatch(item -> !item.builtInRegistryHolder().areComponentsBound())) {
            return;
        }

        Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes = new HashMap<>();
        for (var holder : map.values()) {
            recipes.computeIfAbsent(holder.value().getType(), type -> new HashMap<>())
                    .put(holder.id().identifier(), holder.value());
        }
        PowerlessJetpack.FUELS.clear();
        for (RecipeType<?> recipeType : BuiltInRegistries.RECIPE_TYPE) {
            if (!(recipeType instanceof GTRecipeType gtRecipeType)) {
                continue;
            }
            gtRecipeType.beginStagingRecipes();
            gtRecipeType.getProxyRecipes().forEach((type, list) -> {
                var recipesByID = recipes.get(type);
                if (recipesByID == null) {
                    return;
                }
                RecipeManagerHandler.addProxyRecipesToLookup(recipesByID, gtRecipeType, type, list);
            });
            var recipesByID = recipes.get(gtRecipeType);
            if (recipesByID == null) {
                gtRecipeType.getAdditionHandler().completeStaging();
                continue;
            }
            RecipeManagerHandler.addRecipesToLookup(recipesByID, gtRecipeType);
            gtRecipeType.getAdditionHandler().completeStaging();
        }
        MapIngredientPool.clear();
    }
}
