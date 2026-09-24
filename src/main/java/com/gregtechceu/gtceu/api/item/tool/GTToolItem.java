package com.gregtechceu.gtceu.api.item.tool;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.common.item.tool.behavior.DisableShieldBehavior;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.client.model.runtimegen.ToolItemModelGenerator;

import org.jspecify.annotations.NullMarked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@NullMarked
@ParametersAreNonnullByDefault
public class GTToolItem extends Item implements IGTTool {

    @Getter
    protected final GTToolType toolType;
    @Getter
    protected final int electricTier;
    @Getter
    protected final Material material;
    @Getter
    private IGTToolDefinition toolStats;

    public GTToolItem(GTToolType toolType, MaterialToolTier tier, Material material, IGTToolDefinition definition,
                      Item.Properties properties) {
        super(createProperties(toolType, tier, material, properties));
        this.toolType = toolType;
        this.material = material;
        this.electricTier = toolType.electricTier;
        this.toolStats = definition;
        if (GTCEu.isClientSide()) {
            ToolItemModelGenerator.add(this, toolType);
        }
        definition$init();
    }

    private static Item.Properties createProperties(GTToolType toolType, MaterialToolTier tier, Material material,
                                                    Item.Properties properties) {
        if (material.hasFlag(MaterialFlags.FIRE_RESISTANT)) {
            properties = properties.fireResistant();
        }

        properties.durability(tier.getUses())
                .enchantable(tier.getEnchantmentValue())
                .repairable(tier.getRepairItemsTag());

        HolderGetter<Block> blockLookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        List<Tool.Rule> rules = new ArrayList<>();
        if ("sword".equals(toolType.name)) {
            rules.add(Tool.Rule.minesAndDrops(
                    net.minecraft.core.HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F));
            rules.add(Tool.Rule.overrideSpeed(blockLookup.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE));
            rules.add(Tool.Rule.overrideSpeed(blockLookup.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F));
        }
        rules.add(Tool.Rule.deniesDrops(blockLookup.getOrThrow(tier.getIncorrectBlocksForDropsTag())));
        for (TagKey<Block> harvestTag : toolType.harvestTags) {
            rules.add(Tool.Rule.minesAndDrops(blockLookup.getOrThrow(harvestTag), tier.getSpeed()));
        }

        float disableBlockingForSeconds = toolType.toolDefinition.getBehaviors().contains(DisableShieldBehavior.INSTANCE) ?
                Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS : 0.0F;
        return properties
                .component(DataComponents.TOOL, new Tool(rules, 1.0F, 0, true))
                // GT handles wear itself in hurtEnemy/mineBlock; this keeps attack use
                // semantics while avoiding a second vanilla durability charge.
                .component(DataComponents.WEAPON, new Weapon(0, disableBlockingForSeconds));
    }

    @Override
    public ItemStack getDefaultInstance() {
        return get();
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility action) {
        if (stack instanceof ItemStack itemStack) {
            return definition$canPerformAction(itemStack, action);
        }
        return getToolType().defaultAbilities.contains(action);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        return definition$onItemUseFirst(itemStack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return definition$onItemUse(context);
    }

    public Component getDescription() {
        return Component.translatable(toolType.getUnlocalizedName(), material.getLocalizedName());
    }

    @Override
    public Component getName(ItemStack stack) {
        return this.getDescription();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        return definition$mineBlock(stack, level, state, pos, miningEntity);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        return definition$use(level, player, usedHand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget,
                                                  InteractionHand usedHand) {
        return definition$interactLivingEntity(stack, player, interactionTarget, usedHand);
    }

    @Override
    public boolean isElectric() {
        return electricTier > -1;
    }

    @Nullable
    @Override
    public SoundEntry getSound() {
        return toolType.soundEntry;
    }

    @Override
    public boolean playSoundOnBlockDestroy() {
        return toolType.playSoundOnBlockDestroy;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return definition$getDestroySpeed(stack, state);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        definition$hurtEnemy(stack, target, attacker);
    }

    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        return definition$onBlockStartBreak(stack, pos, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flags) {
        List<Component> components = new ArrayList<>();
        definition$appendHoverText(stack, null, components, flags);
        components.forEach(tooltip);
    }

    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return definition$isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return definition$isFoil(stack);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return definition$doesSneakBypassUse(stack, level, pos, player);
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return definition$shouldCauseBlockBreakReset(oldStack, newStack);
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return definition$hasCraftingRemainingItem(stack);
    }

    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        return definition$getCraftingRemainingItem(itemStack);
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        ItemStack workingStack;
        if (instance instanceof ItemStack itemStack) {
            workingStack = itemStack.copy();
        } else if (instance instanceof ItemStackTemplate template) {
            workingStack = new ItemStack(template.item(), template.count(), template.components());
        } else {
            workingStack = new ItemStack(instance.typeHolder(), instance.count());
        }

        ItemStack remainder = definition$getCraftingRemainingItem(workingStack);
        return remainder.isEmpty() ? null : ItemStackTemplate.fromStack(remainder);
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return definition$shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    @Override
    public int getDamage(ItemStack stack) {
        return definition$getDamage(stack);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return definition$getMaxDamage(stack);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return definition$isCorrectToolForDrops(stack, state);
    }
}
