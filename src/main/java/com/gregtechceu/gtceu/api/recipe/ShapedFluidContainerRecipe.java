package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidContainerIngredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

// TODO shapeless fluid container recipes
public class ShapedFluidContainerRecipe extends NormalCraftingRecipe {

    public static final MapCodec<ShapedFluidContainerRecipe> MAP_CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result))
                    .apply(instance, ShapedFluidContainerRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedFluidContainerRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());
    public static final RecipeSerializer<ShapedFluidContainerRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public ShapedFluidContainerRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                      ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo);
        this.pattern = pattern;
        this.result = result;
    }

    public int getWidth() {
        return this.pattern.width();
    }

    public int getHeight() {
        return this.pattern.height();
    }

    public List<Optional<Ingredient>> getIngredients() {
        return this.pattern.ingredients();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.result.create();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(this.pattern.ingredients());
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapedCraftingRecipeDisplay(getWidth(), getHeight(),
                this.pattern.ingredients().stream()
                        .map(value -> value.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE))
                        .toList(),
                new SlotDisplay.ItemStackSlotDisplay(this.result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<ShapedFluidContainerRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput input) {
        // figure out all the fluid container ingredients' remainders.
        int replacedSlot = -1;
        ItemStack replacement = ItemStack.EMPTY;
        OUTER_LOOP:
        for (int x = 0; x <= input.width() - this.getWidth(); ++x) {
            for (int y = 0; y <= input.height() - this.getHeight(); ++y) {
                var stack = this.findFluidReplacement(input, x, y, false);
                if (stack.firstInt() != -1) {
                    replacedSlot = stack.firstInt();
                    replacement = stack.second();
                    break OUTER_LOOP;
                }

                stack = this.findFluidReplacement(input, x, y, true);
                if (stack.firstInt() != -1) {
                    replacedSlot = stack.firstInt();
                    replacement = stack.second();
                    break OUTER_LOOP;
                }
            }
        }

        NonNullList<ItemStack> items = CraftingRecipe.defaultCraftingReminder(input);
        if (replacedSlot != -1) {
            // the fluid container is replaced by its emptied counterpart instead of its crafting remainder
            items.set(replacedSlot, replacement);
        }
        return items;
    }

    /**
     * Checks if the region of a crafting inventory is match for the recipe.
     */
    private IntObjectPair<ItemStack> findFluidReplacement(CraftingInput inv, int width, int height,
                                                          boolean mirrored) {
        List<Optional<Ingredient>> ingredients = this.getIngredients();
        for (int x = 0; x < inv.width(); ++x) {
            for (int y = 0; y < inv.height(); ++y) {
                int offsetX = x - width;
                int offsetY = y - height;
                Optional<Ingredient> ingredient = Optional.empty();
                if (offsetX >= 0 && offsetY >= 0 && offsetX < this.getWidth() && offsetY < this.getHeight()) {
                    if (mirrored) {
                        ingredient = ingredients.get(this.getWidth() - offsetX - 1 + offsetY * this.getWidth());
                    } else {
                        ingredient = ingredients.get(offsetX + offsetY * this.getWidth());
                    }
                }

                if (ingredient.map(Ingredient::getCustomIngredient).orElse(null) instanceof FluidContainerIngredient fluidContainerIngredient) {
                    int slot = x + y * inv.width();
                    ItemStack stack = inv.getItem(slot);
                    if (fluidContainerIngredient.test(stack)) {
                        return IntObjectPair.of(slot, fluidContainerIngredient.getExtractedStack(stack));
                    }
                }
            }
        }

        return IntObjectPair.of(-1, ItemStack.EMPTY);
    }
}
