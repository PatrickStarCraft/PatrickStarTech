package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.RecipeBuilderCodecs;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public class SizedIngredient implements ICustomIngredient {

    public static final Identifier TYPE = GTCEu.id("sized");
    public static final MapCodec<SizedIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::getInner),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(SizedIngredient::getAmount))
            .apply(instance, SizedIngredient::new));
    public static final IngredientType<SizedIngredient> INGREDIENT_TYPE = new IngredientType<>(CODEC);

    protected int amount;
    protected final Ingredient inner;
    protected SizedIngredient(Ingredient inner, int amount) {
        this.amount = amount;
        this.inner = inner;
    }

    protected SizedIngredient(@NotNull TagKey<Item> tag, int amount) {
        this(RecipeBuilderCodecs.tag(tag), amount);
    }

    protected SizedIngredient(ItemStack itemStack) {
        this(itemStack.isEmpty() ? EmptyIngredient.VANILLA : itemStack.getComponentsPatch().isEmpty() ? Ingredient.of(itemStack.getItem()) :
                DataComponentIngredient.of(true, itemStack), itemStack.getCount());
    }

    public static Ingredient create(ItemStack inner) {
        return new SizedIngredient(inner).toVanilla();
    }

    public static Ingredient create(Ingredient inner, int amount) {
        return new SizedIngredient(inner, amount).toVanilla();
    }

    public static Ingredient create(Ingredient inner) {
        return create(inner, 1);
    }

    public static Ingredient create(TagKey<Item> tag, int amount) {
        return new SizedIngredient(tag, amount).toVanilla();
    }

    public int getAmount() { return amount; }
    public Ingredient getInner() { return inner; }
    public boolean isEmpty() { return inner.isEmpty(); }

    public static Ingredient copy(Ingredient ingredient) {
        if (ingredient.getCustomIngredient() instanceof SizedIngredient sizedIngredient) {
            return SizedIngredient.create(copyDelegate(sizedIngredient.inner), sizedIngredient.amount);
        } else if (ingredient.getCustomIngredient() instanceof IntCircuitIngredient) {
            return ingredient;
        } else if (ingredient.getCustomIngredient() instanceof IntProviderIngredient provider) {
            return provider.copy();
        }
        var items = IngredientStacks.getItems(ingredient);
        return SizedIngredient.create(ingredient, items.length == 0 ? 1 : items[0].getCount());
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null) return false;
        return inner.isEmpty() ? stack.isEmpty() : inner.test(stack);
    }

    /** Representative stacks, sized to this ingredient's configured count. */
    public ItemStack @NotNull [] getItems() {
        var innerStacks = IngredientStacks.getItems(inner);
        var stacks = new ItemStack[innerStacks.length];
        for (int i = 0; i < stacks.length; i++) stacks[i] = innerStacks[i].copyWithCount(amount);
        return stacks;
    }

    public void setAmount(int amount) { this.amount = amount; }

    @Override
    public Stream<Holder<Item>> items() { return inner.items(); }

    @Override
    public boolean isSimple() { return false; }

    @Override
    public IngredientType<SizedIngredient> getType() { return INGREDIENT_TYPE; }

    public static Ingredient getInner(Ingredient ingredient) {
        var custom = ingredient.getCustomIngredient();
        if (custom instanceof SizedIngredient sizedIngredient) return getInner(sizedIngredient.getInner());
        if (custom instanceof IntProviderIngredient provider) return getInner(provider.getInner());
        return ingredient;
    }

    /** Copies the mutable GT custom delegates while leaving immutable vanilla delegates shared. */
    static Ingredient copyDelegate(Ingredient ingredient) {
        var custom = ingredient.getCustomIngredient();
        if (custom instanceof SizedIngredient sized) {
            return new SizedIngredient(copyDelegate(sized.inner), sized.amount).toVanilla();
        }
        if (custom instanceof IntProviderIngredient provider) return provider.copy();
        return ingredient;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SizedIngredient other && amount == other.amount && inner.equals(other.inner);
    }

    @Override
    public int hashCode() { return Objects.hash(inner, amount); }
}
