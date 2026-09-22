package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.gregtechceu.gtceu.data.recipe.builder.RecipeBuilderCodecs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

/** An ingredient with a count rolled when the recipe starts or completes. */
public class IntProviderIngredient implements IRangedIngredient<Ingredient>, ICustomIngredient {

    public static final Identifier TYPE = GTCEu.id("int_provider");
    public static final ItemStack[] EMPTY_STACK_ARRAY = new ItemStack[0];
    private static final Codec<IntProvider> NONNEGATIVE_COUNT_PROVIDER = IntProviders.CODEC.validate(provider ->
            provider.minInclusive() >= 0 ? DataResult.success(provider) :
                    DataResult.error(() -> "IntProviderIngredient must have a min value of at least 0."));
    public static final MapCodec<IntProviderIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(IntProviderIngredient::getInner),
            NONNEGATIVE_COUNT_PROVIDER.fieldOf("count_provider").forGetter(IntProviderIngredient::getCountProvider),
            com.mojang.serialization.Codec.INT.optionalFieldOf("sampledCount", -1).forGetter(IntProviderIngredient::getSampledCount))
            .apply(instance, IntProviderIngredient::new));
    public static final IngredientType<IntProviderIngredient> INGREDIENT_TYPE = new IngredientType<>(CODEC);

    protected final IntProvider countProvider;
    protected int sampledCount = -1;
    protected final Ingredient inner;

    protected IntProviderIngredient(Ingredient inner, IntProvider countProvider) {
        this(inner, countProvider, -1);
    }

    protected IntProviderIngredient(Ingredient inner, IntProvider countProvider, int sampledCount) {
        this.inner = inner;
        this.countProvider = countProvider;
        this.sampledCount = sampledCount;
    }

    public Ingredient copy() {
        return new IntProviderIngredient(SizedIngredient.copyDelegate(inner), countProvider, sampledCount).toVanilla();
    }

    public static Ingredient of(Ingredient inner, IntProvider countProvider) {
        if (countProvider.minInclusive() < 0)
            throw new IllegalArgumentException("IntProviderIngredient must have a min value of at least 0.");
        return new IntProviderIngredient(inner, countProvider).toVanilla();
    }

    public static Ingredient of(ItemStack stack, IntProvider countProvider) {
        Ingredient ingredient = stack.isEmpty() ? EmptyIngredient.VANILLA : stack.getComponentsPatch().isEmpty() ? Ingredient.of(stack.getItem()) :
                DataComponentIngredient.of(true, stack);
        return of(ingredient, countProvider);
    }

    public IntProvider getCountProvider() { return countProvider; }
    public int getSampledCount() { return sampledCount; }
    public void setSampledCount(int count) { sampledCount = count; }
    public Ingredient getInner() { return inner; }

    @Override
    public boolean test(@Nullable ItemStack stack) { return stack != null && inner.test(stack); }

    /** Returns representative stacks at the rolled count, or the maximum count before rolling. */
    public ItemStack @NotNull [] getItems() {
        var stacks = IngredientStacks.getItems(inner);
        int count = isRolled() ? sampledCount : countProvider.maxInclusive();
        for (int i = 0; i < stacks.length; i++) stacks[i] = stacks[i].copyWithCount(count);
        return stacks;
    }

    public @NotNull ItemStack getMaxSizeStack() {
        var stacks = IngredientStacks.getItems(inner);
        return stacks.length == 0 ? ItemStack.EMPTY : stacks[0].copyWithCount(countProvider.maxInclusive());
    }

    public int rollSampledCount(@NotNull RandomSource random) {
        if (!isRolled()) sampledCount = countProvider.sample(random);
        return sampledCount;
    }

    @Override
    public Ingredient collapse() {
        IRangedIngredient.super.collapse();
        return SizedIngredient.create(inner, rollSampledCount());
    }

    public void reset() { sampledCount = -1; }

    @Override
    public Stream<Holder<Item>> items() { return inner.items(); }

    @Override
    public boolean isSimple() { return false; }

    @Override
    public IngredientType<IntProviderIngredient> getType() { return INGREDIENT_TYPE; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntProviderIngredient other && inner.equals(other.inner)
                && countProvider.equals(other.countProvider) && sampledCount == other.sampledCount;
    }

    @Override
    public int hashCode() { return Objects.hash(inner, countProvider, sampledCount); }
}
