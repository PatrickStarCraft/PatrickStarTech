package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.item.behavior.FacadeItemBehaviour;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.codec.StreamCodec;

import org.jspecify.annotations.NullMarked;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.google.gson.JsonObject;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@NullMarked
@ParametersAreNonnullByDefault
public class FacadeCoverRecipe implements CraftingRecipe {

    public static final FacadeCoverRecipe INSTANCE = new FacadeCoverRecipe();
    /** The recipe is stateless, so both codecs ignore their input and yield {@link #INSTANCE}. */
    public static final RecipeSerializer<FacadeCoverRecipe> SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    public static Identifier ID = GTCEu.id("crafting/facade_cover");

    @Override
    public boolean matches(CraftingInput container, Level level) {
        int platesCount = 0;
        boolean foundBlockItem = false;
        for (int i = 0; i < container.size(); i++) {
            var item = container.getItem(i);
            if (item.isEmpty()) continue;
            if (FacadeItemBehaviour.isValidFacade(item)) {
                if (foundBlockItem) {
                    return false;
                }
                foundBlockItem = true;
            } else if (item.is(ChemicalHelper.getTagOrThrow(TagPrefix.plate, GTMaterials.Iron))) {
                if (platesCount > 1) {
                    return false;
                }
                platesCount++;
            } else {
                return false;
            }
        }
        return foundBlockItem && platesCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput container) {
        ItemStack itemStack = GTItems.COVER_FACADE.asStack();
        BlockState facadeState = null;

        for (int i = 0; i < container.size(); i++) {
            var item = container.getItem(i);
            if (item.isEmpty()) continue;
            if (FacadeItemBehaviour.isValidFacade(item)) {
                facadeState = FacadeItemBehaviour.getFacadeState(item);
                break;
            }
        }
        if (facadeState != null) {
            FacadeItemBehaviour.setFacadeState(itemStack, facadeState);
            itemStack.setCount(6);
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public PlacementInfo placementInfo() {
        var ironPlateTag = ChemicalHelper.getTagOrThrow(TagPrefix.plate, GTMaterials.Iron);
        HolderSet<Item> ironPlates = BuiltInRegistries.ITEM.get(ironPlateTag)
                .<HolderSet<Item>>map(tag -> tag).orElseGet(HolderSet::empty);
        return PlacementInfo.create(List.of(Ingredient.of(ironPlates), Ingredient.of(Blocks.STONE)));
    }

    public Identifier getId() {
        return ID;
    }

    @Override
    public RecipeSerializer<FacadeCoverRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }
}
