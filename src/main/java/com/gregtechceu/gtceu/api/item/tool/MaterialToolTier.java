package com.gregtechceu.gtceu.api.item.tool;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.ToolProperty;
import com.gregtechceu.gtceu.api.data.tag.TagUtil;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class MaterialToolTier {

    public final Material material;

    public final ToolProperty property;

    public MaterialToolTier(Material material) {
        this.material = material;
        if (!material.hasProperty(PropertyKey.TOOL)) {
            throw new IllegalArgumentException("material %s hasn't got Tool Property".formatted(material));
        }
        this.property = material.getProperty(PropertyKey.TOOL);
    }

    public int getUses() {
        return property.getDurability() * property.getDurabilityMultiplier();
    }

    public float getSpeed() {
        return property.getHarvestSpeed();
    }

    public float getAttackDamageBonus() {
        return property.getAttackDamage();
    }

    public int getLevel() {
        return property.getHarvestLevel();
    }

    public int getEnchantmentValue() {
        return property.getEnchantability();
    }

    public TagKey<Block> getIncorrectBlocksForDropsTag() {
        int level = Math.max(0, Math.min(getLevel(), CustomTags.INCORRECT_FOR_GT_TOOL_TIERS.length - 1));
        return CustomTags.INCORRECT_FOR_GT_TOOL_TIERS[level];
    }

    public TagKey<Item> getRepairItemsTag() {
        return getRepairItemsTag(material);
    }

    public static TagKey<Item> getRepairItemsTag(Material material) {
        Identifier tagId = Identifier.fromNamespaceAndPath(material.getModid(),
                "repairable/tools/" + material.getName());
        return TagUtil.optionalTag(Registries.ITEM, tagId);
    }
}
