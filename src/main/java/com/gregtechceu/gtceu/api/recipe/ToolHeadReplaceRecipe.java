package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.tterrag.registrate.util.entry.ItemProviderEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolHeadReplaceRecipe extends CustomRecipe {

    private static final ToolHeadReplaceRecipe INSTANCE = new ToolHeadReplaceRecipe();
    public static final RecipeSerializer<ToolHeadReplaceRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));
    public static final Identifier ID = GTCEu.id("crafting/replace_tool_head");

    private static final Map<TagPrefix, GTToolType[]> TOOL_HEAD_TO_TOOL_MAP = new HashMap<>();

    public static void setToolHeadForTool(TagPrefix toolHead, GTToolType tool) {
        if (!(tool.electricTier > -1)) return;
        TOOL_HEAD_TO_TOOL_MAP.computeIfAbsent(toolHead, p -> new GTToolType[GTValues.MAX])[tool.electricTier] = tool;
    }

    public ToolHeadReplaceRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        List<ItemStack> list = new ArrayList<>();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                list.add(stack);
                if (list.size() > 2) {
                    return false;
                }
            }
        }

        if (list.size() == 2) {
            ItemStack stack1 = list.get(0);
            ItemStack stack2 = list.get(1);

            IGTTool tool;
            MaterialEntry toolHead;
            if (stack1.getItem() instanceof IGTTool) {
                tool = (IGTTool) stack1.getItem();
                toolHead = ChemicalHelper.getMaterialEntry(stack2.getItem());
            } else if (stack2.getItem() instanceof IGTTool) {
                tool = (IGTTool) stack2.getItem();
                toolHead = ChemicalHelper.getMaterialEntry(stack1.getItem());
            } else return false;

            if (!tool.isElectric()) return false;
            if (toolHead == null) return false;
            GTToolType[] output = TOOL_HEAD_TO_TOOL_MAP.get(toolHead.tagPrefix());
            return output != null && output[tool.getElectricTier()] != null &&
                    GTMaterialItems.TOOL_ITEMS.get(toolHead.material(), output[tool.getElectricTier()]) != null;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        List<ItemStack> list = new ArrayList<>();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack itemstack = inv.getItem(i);

            if (!itemstack.isEmpty()) {
                list.add(itemstack);
            }
        }

        if (list.size() == 2) {
            ItemStack first = list.get(0), second = list.get(1);

            IGTTool tool;
            MaterialEntry toolHead;
            ItemStack realTool;
            if (first.getItem() instanceof IGTTool) {
                tool = (IGTTool) first.getItem();
                toolHead = ChemicalHelper.getMaterialEntry(second.getItem());
                realTool = first;
            } else if (second.getItem() instanceof IGTTool) {
                tool = (IGTTool) second.getItem();
                toolHead = ChemicalHelper.getMaterialEntry(first.getItem());
                realTool = second;
            } else return ItemStack.EMPTY;
            if (!tool.isElectric()) return ItemStack.EMPTY;
            IElectricItem powerUnit = GTCapabilityHelper.getElectricItem(realTool);
            if (toolHead == null || powerUnit == null) return ItemStack.EMPTY;
            GTToolType[] toolArray = TOOL_HEAD_TO_TOOL_MAP.get(toolHead.tagPrefix());
            ItemProviderEntry<Item, ? extends Item> toolEntry = GTMaterialItems.TOOL_ITEMS.get(toolHead.material(),
                    toolArray[tool.getElectricTier()]);
            if (toolEntry == null) return ItemStack.EMPTY;
            Item replacementItem = toolEntry.get();
            if (!(replacementItem instanceof IGTTool replacementTool)) return ItemStack.EMPTY;
            return replacementTool.get(powerUnit.getCharge(), powerUnit.getMaxCharge());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> result = CraftingRecipe.defaultCraftingReminder(input);
        for (int i = 0; i < result.size(); i++) {
            if (input.getItem(i).getItem() instanceof IGTTool) {
                result.set(i, ItemStack.EMPTY);
            }
        }
        return result;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
