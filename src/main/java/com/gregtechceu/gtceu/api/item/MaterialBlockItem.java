package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaterialBlockItem extends BlockItem {

    public final TagPrefix tagPrefix;
    public final Material material;

    public MaterialBlockItem(Block block, Properties properties, TagPrefix tagPrefix, Material material) {
        super(block, properties);
        this.tagPrefix = tagPrefix;
        this.material = material;
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
        int burnTime = getItemBurnTime();
        return burnTime >= 0 ? burnTime : fuelValues.burnDuration(itemStack);
    }

    @Override
    @NotNull
    public MaterialBlock getBlock() {
        return (MaterialBlock) super.getBlock();
    }

    @Override
    public Component getName(ItemStack stack) {
        return getBlock().getName();
    }

    public int getItemBurnTime() {
        DustProperty property = material == null ? null : material.getProperty(PropertyKey.DUST);
        if (property != null) {
            return (int) (property.getBurnTime() * tagPrefix.getMaterialAmount(material) / GTValues.M);
        }
        return -1;
    }
}
