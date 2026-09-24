package com.gregtechceu.gtceu.client;

import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.common.item.armor.GTArmorItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import org.jetbrains.annotations.Nullable;

/** Bridges GregTech armor data to the 26.2 equipment renderer hooks. */
public final class GTArmorClientExtensions implements IClientItemExtensions {

    public static final GTArmorClientExtensions INSTANCE = new GTArmorClientExtensions();

    private GTArmorClientExtensions() {}

    @Override
    public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        if (stack.getItem() instanceof ArmorComponentItem armor && original instanceof HumanoidModel<?> humanoid) {
            // The 26.2 model-selection hook no longer receives the wearer. Existing GT armor logic defaults to the
            // supplied model; keep the legacy extension point callable for implementations that only need stack/slot.
            return armor.getArmorLogic().getArmorModel(null, stack, armor.getEquipmentSlot(), humanoid);
        }
        return original;
    }

    @Override
    public int getDefaultDyeColor(ItemStack stack) {
        if (stack.getItem() instanceof GTArmorItem armor) {
            return armor.material.getMaterialARGB();
        }
        return IClientItemExtensions.super.getDefaultDyeColor(stack);
    }

    @Override
    public @Nullable Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType layerType,
                                                EquipmentClientInfo.Layer layer, Identifier defaultTexture) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return null;
        EquipmentSlot slot = equippable.slot();
        String layerName = layer.dyeable().isPresent() ? "overlay" :
                (slot == EquipmentSlot.LEGS ? "layer_2" : "layer_1");
        var wearer = Minecraft.getInstance().player;

        String customTexture = null;
        if (stack.getItem() instanceof ArmorComponentItem armor) {
            customTexture = armor.getArmorTexture(stack, wearer, slot, layerName);
        } else if (stack.getItem() instanceof GTArmorItem armor) {
            customTexture = armor.getArmorTexture(stack, wearer, slot, layerName);
        }
        return customTexture == null ? null : Identifier.parse(customTexture);
    }
}
