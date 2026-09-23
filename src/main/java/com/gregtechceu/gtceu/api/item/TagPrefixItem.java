package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.client.model.runtimegen.TagPrefixItemModelGenerator;
import com.gregtechceu.gtceu.common.data.GTDamageTypes;

import org.jspecify.annotations.NullMarked;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.FuelValues;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@NullMarked
public class TagPrefixItem extends Item {

    public final TagPrefix tagPrefix;
    public final Material material;

    public TagPrefixItem(Properties properties, TagPrefix tagPrefix, Material material) {
        super(properties);
        this.tagPrefix = tagPrefix;
        this.material = material;
        if (GTCEu.isClientSide()) {
            TagPrefixItemModelGenerator.add(this, tagPrefix.materialIconType(), material.getMaterialIconSet());
        }
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
        int burnTime = getItemBurnTime();
        return burnTime >= 0 ? burnTime : fuelValues.burnDuration(itemStack);
    }

    @OnlyIn(Dist.CLIENT)
    public static ItemColor tintColor(Material material) {
        return (itemStack, index) -> material.getLayerARGB(index);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, display, tooltip, isAdvanced);
        if (this.tagPrefix.tooltip() != null) {
            List<Component> tooltipComponents = new ArrayList<>();
            this.tagPrefix.tooltip().accept(material, tooltipComponents);
            tooltipComponents.forEach(tooltip);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return tagPrefix.getLocalizedName(material);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.tickCount % 20 == 0) {
                if (tagPrefix != TagPrefix.ingotHot || !material.hasProperty(PropertyKey.BLAST))
                    return;

                float heatDamage = ((material.getBlastTemperature() - 1750) / 1000.0F) + 2;
                ItemStack armor = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
                if (!armor.isEmpty() && armor.getItem() instanceof ArmorComponentItem armorItem) {
                    heatDamage *= armorItem.getArmorLogic().getHeatResistance();
                }
                if (heatDamage > 0.0) {
                    livingEntity.hurt(GTDamageTypes.HEAT.source(level), heatDamage);
                } else if (heatDamage < 0.0) {
                    livingEntity.hurt(livingEntity.damageSources().freeze(), -heatDamage);
                }
            }
        }
    }

    public int getItemBurnTime() {
        DustProperty property = material == null ? null : material.getProperty(PropertyKey.DUST);
        if (property != null)
            return (int) (property.getBurnTime() * tagPrefix.getMaterialAmount(material) / GTValues.M);
        return -1;
    }

    // TODO BEACON PAYMENT
}
