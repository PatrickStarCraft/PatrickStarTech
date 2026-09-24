package com.gregtechceu.gtceu.common.item.armor;

import net.minecraft.util.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.common.util.Lazy;

import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumMap;

/**
 * GT's armour material presets.
 * <p>
 * {@code ArmorMaterial} is a final record in 26.2 and can no longer be implemented, so this enum keeps only the
 * {@link StringRepresentable} contract and exposes its stats through normal methods.
 */
@AllArgsConstructor
public enum GTArmorMaterials implements StringRepresentable {

    GOGGLES("goggles", 0, Util.make(new EnumMap<>(ArmorType.class), map -> {
        map.put(ArmorType.BOOTS, 0);
        map.put(ArmorType.LEGGINGS, 0);
        map.put(ArmorType.CHESTPLATE, 0);
        map.put(ArmorType.HELMET, 0);
    }), 50, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F),
    JETPACK("jetpack", 0, Util.make(new EnumMap<>(ArmorType.class), map -> {
        map.put(ArmorType.BOOTS, 0);
        map.put(ArmorType.LEGGINGS, 0);
        map.put(ArmorType.CHESTPLATE, 0);
        map.put(ArmorType.HELMET, 0);
    }), 50, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F),
    ARMOR("armor", 0, Util.make(new EnumMap<>(ArmorType.class), map -> {
        map.put(ArmorType.BOOTS, 0);
        map.put(ArmorType.LEGGINGS, 0);
        map.put(ArmorType.CHESTPLATE, 0);
        map.put(ArmorType.HELMET, 0);
    }), 50, SoundEvents.ARMOR_EQUIP_GENERIC, 5.0F, 0.0F),
    BAD_PPE_EQUIPMENT("bad_ppe_equipment", 10, Util.make(new EnumMap<>(ArmorType.class), map -> {
        map.put(ArmorType.BOOTS, 1);
        map.put(ArmorType.LEGGINGS, 2);
        map.put(ArmorType.CHESTPLATE, 3);
        map.put(ArmorType.HELMET, 1);
    }), 10, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F),
    GOOD_PPE_EQUIPMENT("good_ppe_equipment", 20, Util.make(new EnumMap<>(ArmorType.class), map -> {
        map.put(ArmorType.BOOTS, 2);
        map.put(ArmorType.LEGGINGS, 5);
        map.put(ArmorType.CHESTPLATE, 6);
        map.put(ArmorType.HELMET, 2);
    }), 10, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F),
    ;

    public static final Codec<GTArmorMaterials> CODEC = StringRepresentable.fromEnum(GTArmorMaterials::values);
    private static final EnumMap<ArmorType, Integer> HEALTH_FUNCTION_FOR_TYPE = Util
            .make(new EnumMap<>(ArmorType.class), map -> {
                map.put(ArmorType.BOOTS, 13);
                map.put(ArmorType.LEGGINGS, 15);
                map.put(ArmorType.CHESTPLATE, 16);
                map.put(ArmorType.HELMET, 11);
            });

    private final String name;
    private final int durabilityMultiplier;
    private final EnumMap<ArmorType, Integer> protectionFunctionForType;
    @Getter
    private final int enchantmentValue;
    @Getter
    private final Holder<SoundEvent> equipSound;
    @Getter
    private final float toughness;
    @Getter
    private final float knockbackResistance;


    public int getDurabilityForType(ArmorType type) {
        return HEALTH_FUNCTION_FOR_TYPE.get(type) * this.durabilityMultiplier;
    }

    public Item.Properties applyProperties(ArmorType type, Item.Properties properties) {
        // Powered armor computes its attributes per stack so its armor value can track stored charge.
        return properties.durability(Integer.MAX_VALUE).enchantable(50)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot()).setEquipSound(equipSound).build());
    }

    public ItemAttributeModifiers createAttributes(ArmorType type) {
        var id = Identifier.fromNamespaceAndPath("gtceu", "armor." + type.getName());
        var slot = EquipmentSlotGroup.bySlot(type.getSlot());
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, getDefenseForType(type), AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, knockbackResistance, AttributeModifier.Operation.ADD_VALUE), slot)
                .build();
    }

    public int getDefenseForType(ArmorType type) {
        return this.protectionFunctionForType.get(type);
    }

    public java.util.Optional<Ingredient> getRepairIngredient() {
        return java.util.Optional.empty();
    }

    public String getName() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
