package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.common.item.armor.GTArmorItem;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EquipmentLayerRenderer.class)
public abstract class HumanoidArmorLayerMixin {

    @ModifyExpressionValue(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;" +
                    "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;" +
                    "Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;" +
                    "Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE", target =
                    "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;" +
                            "getArmorLayerTintColor(Lnet/minecraft/world/item/ItemStack;" +
                            "Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I"))
    private int gtceu$blendMaterialArmorTint(int layerTint, @Local(argsOnly = true) ItemStack itemStack) {
        Object item = itemStack.getItem();
        if (layerTint == 0 || !GTArmorItem.class.isInstance(item)) {
            return layerTint;
        }

        GTArmorItem armorItem = GTArmorItem.class.cast(item);
        int materialColor = armorItem.material.getMaterialARGB();
        int red = blendArmorChannel(ARGB.red(materialColor), ARGB.red(layerTint));
        int green = blendArmorChannel(ARGB.green(materialColor), ARGB.green(layerTint));
        int blue = blendArmorChannel(ARGB.blue(materialColor), ARGB.blue(layerTint));
        return (layerTint & 0xFF000000) | red << 16 | green << 8 | blue;
    }

    private static int blendArmorChannel(int material, int layer) {
        return layer == 255 ? material : (material + layer) / 2;
    }
}
