package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.ArmorProperty;
import com.gregtechceu.gtceu.client.model.runtimegen.ArmorItemModelGenerator;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GTArmorItem extends Item {

    public final Material material;
    public final ArmorProperty armorProperty;
    protected final ArmorType type;

    public GTArmorItem(ArmorProperty.ArmorMaterial armorMaterial, ArmorType type, Item.Properties properties,
                       Material material, ArmorProperty armorProperty) {
        super(createProperties(armorMaterial, type, properties));
        this.material = material;
        this.armorProperty = armorProperty;
        this.type = type;
        if (GTCEu.isClientSide()) {
            ArmorItemModelGenerator.add(this, type);
        }
    }

    private static Item.Properties createProperties(ArmorProperty.ArmorMaterial armorMaterial, ArmorType type,
                                                    Item.Properties properties) {
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        Identifier modifierId = Identifier.fromNamespaceAndPath(GTCEu.MOD_ID, "armor." + type.getName());
        ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(modifierId, armorMaterial.getDefenseForType(type),
                        AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(modifierId, armorMaterial.getToughness(),
                        AttributeModifier.Operation.ADD_VALUE), slot);
        if (armorMaterial.getKnockbackResistance() > 0.0F) {
            attributes.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(modifierId, armorMaterial.getKnockbackResistance(),
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        ResourceKey<EquipmentAsset> equipmentAsset = ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.parse(armorMaterial.getName()));
        int durability = armorMaterial.getDurabilityForType(type);
        Item.Properties result = durability > 0 ? properties.durability(durability) : properties;
        result = result.attributes(attributes.build())
                .enchantable(armorMaterial.getEnchantmentValue())
                .component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot())
                        .setEquipSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(armorMaterial.getEquipSound()))
                        .setAsset(equipmentAsset)
                        .build());

        // The 26.2 repairable component uses the same holder set as a simple ingredient,
        // preserving material repair tags without taking a tag snapshot.
        var repairIngredient = armorMaterial.getRepairIngredient();
        if (repairIngredient.isSimple()) {
            var customRepairIngredient = repairIngredient.getCustomIngredient();
            HolderSet<Item> repairItems = customRepairIngredient == null
                    ? repairIngredient.getValues()
                    : HolderSet.direct(customRepairIngredient.items().toList());
            if (repairItems.size() > 0) {
                result.component(DataComponents.REPAIRABLE, new Repairable(repairItems));
            }
        }
        // A non-simple custom predicate cannot be represented by the target Repairable holder-set component.
        return result;
    }

    public @NotNull Component getDescription() {
        return Component.translatable("item.gtceu.armor." + type.getName(), material.getLocalizedName());
    }

    @Override
    public Component getName(ItemStack stack) {
        return this.getDescription();
    }

    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        Identifier id = armorProperty.getCustomTextureGetter()
                .getCustomTexture(stack, entity, slot, Objects.equals(type, "overlay"));
        if (id != null) {
            return id.toString();
        }
        return null;
    }
}
