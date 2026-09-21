package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;

import com.mojang.serialization.Codec;
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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

@NullMarked
public class ShapedEnergyTransferRecipe extends NormalCraftingRecipe {

    public static final MapCodec<ShapedEnergyTransferRecipe> MAP_CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                    Ingredient.CODEC.fieldOf("chargeIngredient").forGetter(recipe -> recipe.chargeIngredient),
                    Codec.BOOL.optionalFieldOf("overrideCharge", false).forGetter(recipe -> recipe.overrideCharge),
                    Codec.BOOL.optionalFieldOf("transferMaxCharge", false)
                            .forGetter(recipe -> recipe.transferMaxCharge),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result))
                    .apply(instance, ShapedEnergyTransferRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedEnergyTransferRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());
    public static final RecipeSerializer<ShapedEnergyTransferRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    @Getter
    private final Ingredient chargeIngredient;
    @Getter
    private final boolean transferMaxCharge;
    @Getter
    private final boolean overrideCharge;

    public ShapedEnergyTransferRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                      ShapedRecipePattern pattern, Ingredient chargeIngredient,
                                      boolean overrideCharge, boolean transferMaxCharge, ItemStackTemplate result) {
        super(commonInfo, bookInfo);
        this.pattern = pattern;
        this.result = result;
        this.chargeIngredient = chargeIngredient;
        this.transferMaxCharge = transferMaxCharge;
        this.overrideCharge = overrideCharge;
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
        long maxCharge = 0L;
        long charge = 0L;
        ItemStack resultStack = this.result.create();
        for (ItemStack chargeStack : chargeIngredient.items().map(ItemStack::new).toList()) {
            for (int i = 0; i < input.size(); i++) {
                if (ItemStack.isSameItem(input.getItem(i), chargeStack)) {
                    ItemStack stack = input.getItem(i);
                    IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
                    if (electricItem != null) {
                        maxCharge += electricItem.getMaxCharge();
                        charge += electricItem.getCharge();
                        resultStack.getOrCreateTag().putLong("MaxCharge", maxCharge);
                        resultStack.getOrCreateTag().putLong("Charge", charge);
                        return resultStack;
                    }
                }
            }
        }
        return resultStack;
    }

    /** Result as it would appear in the recipe book, charged from the configured charge ingredient. */
    public ItemStack getResultItem() {
        long maxCharge = 0L;
        long charge = 0L;
        ItemStack resultStack = this.result.create();
        for (ItemStack chargeStack : chargeIngredient.items().map(ItemStack::new).toList()) {
            IElectricItem electricItem = GTCapabilityHelper.getElectricItem(chargeStack);
            if (electricItem != null) {
                maxCharge += electricItem.getMaxCharge();
                charge += electricItem.getCharge();
                resultStack.getOrCreateTag().putLong("MaxCharge", maxCharge);
                resultStack.getOrCreateTag().putLong("Charge", charge);
                return resultStack;
            }
        }
        return resultStack;
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
    public @NotNull RecipeSerializer<ShapedEnergyTransferRecipe> getSerializer() {
        return SERIALIZER;
    }
}
