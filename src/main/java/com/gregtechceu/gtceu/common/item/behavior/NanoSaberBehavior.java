package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.component.IEnchantableItem;
import com.gregtechceu.gtceu.api.item.component.IItemAttributes;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class NanoSaberBehavior extends ToggleEnergyConsumerBehavior implements IItemAttributes, IEnchantableItem {

    public static final Identifier OVERRIDE_KEY_LOCATION = GTCEu.id("nano_saber_active");

    private final double baseAttackDamage;
    private final double additionalAttackDamage;

    public NanoSaberBehavior() {
        super(ConfigHolder.INSTANCE.tools.nanoSaber.energyConsumption);
        this.baseAttackDamage = ConfigHolder.INSTANCE.tools.nanoSaber.nanoSaberBaseDamage;
        this.additionalAttackDamage = ConfigHolder.INSTANCE.tools.nanoSaber.nanoSaberDamageBoost;
    }

    @Override
    public ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        double attackDamage = baseAttackDamage + (isItemActive(stack) ? additionalAttackDamage : 0.0D);
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.UNBREAKING) &&
                !enchantment.is(Enchantments.MENDING) &&
                enchantment.value().isSupportedItem(new ItemStack(Items.IRON_SWORD));
    }
}
