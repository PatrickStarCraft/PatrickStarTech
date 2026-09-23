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
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

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

    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(itemTag(ChemicalHelper.getTagOrThrow(TagPrefix.plate, GTMaterials.Iron))));
        ingredients.add(Ingredient.of(Blocks.STONE));
        return ingredients;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    public ItemStack getResultItem(RegistryAccess registryManager) {
        return createPreviewStack();
    }

    private static ItemStack createPreviewStack() {
        ItemStack result = GTItems.COVER_FACADE.asStack(6);
        FacadeItemBehaviour.setFacadeState(result, Blocks.STONE.defaultBlockState());
        return result;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(this.getIngredients());
    }

    @Override
    public java.util.List<RecipeDisplay> display() {
        return java.util.List.of(new ShapelessCraftingRecipeDisplay(
                this.getIngredients().stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(createPreviewStack())),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    private static HolderSet<net.minecraft.world.item.Item> itemTag(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        return BuiltInRegistries.ITEM.get(tag)
                .<HolderSet<net.minecraft.world.item.Item>>map(holders -> holders)
                .orElseThrow(() -> new IllegalStateException("Missing item tag " + tag.location()));
    }
}
