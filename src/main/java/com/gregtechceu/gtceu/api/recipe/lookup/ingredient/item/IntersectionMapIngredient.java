package com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class IntersectionMapIngredient extends AbstractMapIngredient {

    @Getter
    protected List<AbstractMapIngredient> children;

    public IntersectionMapIngredient(List<AbstractMapIngredient> children) {
        this.children = children;
        this.children.sort(Comparator.comparingInt(AbstractMapIngredient::hashCode));
    }

    @NotNull
    public static List<AbstractMapIngredient> from(IntersectionIngredient ingredient) {
        // Any matching stack must match each child. Index the union of child keys as candidates;
        // RecipeDB's validity predicate applies the full intersection after lookup.
        Set<AbstractMapIngredient> mapIngredients = new LinkedHashSet<>();
        for (Ingredient child : ingredient.children()) {
            mapIngredients.addAll(MapIngredientTypeManager.getFrom(child, ItemRecipeCapability.CAP));
        }
        return new ObjectArrayList<>(mapIngredients);
    }

    @NotNull
    public static List<AbstractMapIngredient> from(ItemStack stack) {
        MaterialEntry entry = ChemicalHelper.getMaterialEntry(stack.getItem());

        if (entry != null && TagPrefix.ORES.containsKey(entry.tagPrefix())) {
            List<AbstractMapIngredient> children = new ArrayList<>();
            children.add(new ItemTagMapIngredient(entry.tagPrefix().getItemTags(entry.material()).get(0)));
            children.add(new ItemTagMapIngredient(entry.tagPrefix().getItemParentTags().get(0)));

            return Collections.singletonList(new IntersectionMapIngredient(children));
        }
        return Collections.emptyList();
    }

    @Override
    protected int hash() {
        // This key can compare equal to a plain item key when every child accepts that item.
        // Use the common child bucket so hash-based recipe lookup reaches that comparison.
        return children.isEmpty() ? 0 : children.get(0).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            IntersectionMapIngredient other = (IntersectionMapIngredient) o;
            if (this.children != null) {
                if (other.children != null) {
                    if (this.children.size() != other.children.size()) return false;
                    for (int i = 0; i < this.children.size(); ++i) {
                        var ingredient1 = this.children.get(i);
                        var ingredient2 = other.children.get(i);
                        if (!ingredient1.equals(ingredient2)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } else if (o instanceof ItemStackMapIngredient stackIngredient) {
            return matchesItemStack(stackIngredient);
        }
        return false;
    }

    boolean matchesItemStack(ItemStackMapIngredient stackIngredient) {
        for (var child : this.children) {
            if (!child.equals(stackIngredient)) return false;
        }
        return true;
    }

    @Override
    public boolean isSpecialIngredient() {
        for (var child : this.children) {
            if (child.isSpecialIngredient()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "IntersectionMapIngredient{" + "children=" + children + "}";
    }
}
