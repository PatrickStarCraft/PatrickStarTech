package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IDataItem;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.utils.GTStringUtils;
import com.gregtechceu.gtceu.utils.ResearchManager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class DataItemBehavior implements IInteractionItem, IAddInformation, IDataItem {

    private final boolean requireDataBank;
    @Getter
    private final int capacity;

    public DataItemBehavior(boolean requireDataBank, int capacity) {
        this.requireDataBank = requireDataBank;
        this.capacity = capacity;
    }

    @Override
    public InteractionResult use(Item item, Level level, Player player, InteractionHand usedHand) {
        if (player.isShiftKeyDown()) {
            ItemStack stack = player.getItemInHand(usedHand);
            if (!level.isClientSide()) {
                var permissions = player.permissions();
                int perm = permissions.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_OWNER) ? 4 :
                        permissions.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN) ? 3 :
                        permissions.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER) ? 2 :
                        permissions.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR) ? 1 : 0;
                com.gregtechceu.gtceu.api.item.data.ItemStackData.update(stack, tag -> {
                    tag.putString("boundPlayerName", com.gregtechceu.gtceu.utils.data.ComponentJson.toJson(player.getDisplayName()));
                    tag.putInt("boundPlayerPermLevel", perm);
                    tag.putString("boundPlayerUUID", player.getStringUUID());
                });
            }
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
        }
        return IInteractionItem.super.use(item, level, player, usedHand);
    }

    @Override
    public boolean requireDataBank() {
        return requireDataBank;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        var data = com.gregtechceu.gtceu.api.item.data.ItemStackData.read(stack);
        if (data.contains("boundPlayerName")) {
            MutableComponent name = com.gregtechceu.gtceu.utils.data.ComponentJson.fromJson(data.getStringOr("boundPlayerName", ""));
            tooltipComponents.add(Component.translatable("gtceu.tooltip.player_bind", name));
        }
        if (data.contains("targetX")) {
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.wireless_transmitter_bind",
                    Component.literal("" + data.getIntOr("targetX", 0)).withStyle(ChatFormatting.GOLD),
                    Component.literal("" + data.getIntOr("targetY", 0)).withStyle(ChatFormatting.GOLD),
                    Component.literal("" + data.getIntOr("targetZ", 0)).withStyle(ChatFormatting.GOLD),
                    Component.literal(data.getStringOr("face", "")).withStyle(ChatFormatting.DARK_PURPLE),
                    Component.literal(data.getStringOr("dim", "")).withStyle(ChatFormatting.GREEN)));
        }
        if (data.contains("computer_monitor_cover_config")) {
            tooltipComponents.add(Component.translatable("gtceu.tooltip.computer_monitor_config"));
        }
        if (data.contains("computer_monitor_cover_data")) {
            tooltipComponents.add(
                    Component.translatable("gtceu.tooltip.computer_monitor_data",
                            GTStringUtils.toComponent(
                                    com.gregtechceu.gtceu.utils.data.TypedTagList.read(data, "computer_monitor_cover_data", Tag.TAG_STRING))));
        }
        ResearchManager.ResearchItem researchData = ResearchManager.readResearchId(stack);
        if (researchData == null) {
            int[] posArray = data.getIntArray("pos").orElseGet(() -> new int[0]);
            if (posArray.length == 3) {
                tooltipComponents.add(Component.translatable(
                        "gtceu.tooltip.proxy_bind",
                        Component.literal("" + posArray[0]).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal("" + posArray[1]).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal("" + posArray[2]).withStyle(ChatFormatting.LIGHT_PURPLE)));
            }
        } else {
            Collection<GTRecipe> recipes = researchData.recipeType().getDataStickEntry(researchData.researchId());
            if (recipes != null && !recipes.isEmpty()) {
                tooltipComponents.add(Component.translatable("behavior.data_item.title",
                        Component.translatable(researchData.recipeType().registryName.toLanguageKey())));
                Collection<ItemStack> addedItems = new ObjectOpenHashSet<>();
                Collection<FluidStack> addedFluids = new ObjectOpenHashSet<>();
                outerItems:
                for (GTRecipe recipe : recipes) {
                    var contents = recipe.getOutputContents(ItemRecipeCapability.CAP);
                    if (contents.isEmpty()) continue;
                    ItemStack outputItems = com.gregtechceu.gtceu.api.recipe.ingredient.IngredientStacks.getItems(
                            ItemRecipeCapability.CAP.of(contents.get(0).content()))[0];
                    for (var item : addedItems) {
                        if (outputItems.is(item.getItem())) continue outerItems;
                    }
                    if (addedItems.add(outputItems)) {
                        tooltipComponents.add(
                                Component.translatable("behavior.data_item.data",
                                        outputItems.getDisplayName()));
                    }
                }
                outerFluids:
                for (GTRecipe recipe : recipes) {
                    var contents = recipe.getOutputContents(FluidRecipeCapability.CAP);
                    if (contents.isEmpty()) continue;
                    FluidStack outputFluids = FluidRecipeCapability.CAP
                            .of(contents.get(0).content()).getStacks()[0];
                    for (var fluid : addedFluids) {
                        if (FluidStack.matches(outputFluids, fluid)) continue outerFluids;
                    }
                    if (addedFluids.add(outputFluids)) {
                        tooltipComponents.add(
                                Component.translatable("behavior.data_item.data",
                                        outputFluids.getHoverName()));
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        ICoverable coverable = GTCapabilityHelper.getCoverable(context.getLevel(), context.getClickedPos(),
                context.getClickedFace());
        if (coverable != null &&
                coverable.getCoverAtSide(context.getClickedFace()) instanceof IDataStickInteractable interactable) {
            if (context.isSecondaryUseActive()) {
                if (ResearchManager.readResearchId(itemStack) == null) {
                    return interactable.onDataStickShiftUse(context.getPlayer(), itemStack);
                }
                return (context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
            } else {
                return interactable.onDataStickUse(context.getPlayer(), itemStack);
            }
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MetaMachine machine) {
            if (!MachineOwner.canOpenOwnerMachine(context.getPlayer(), machine)) {
                return InteractionResult.FAIL;
            }
            if (machine instanceof IDataStickInteractable interactable) {
                if (context.isSecondaryUseActive()) {
                    if (ResearchManager.readResearchId(itemStack) == null) {
                        return interactable.onDataStickShiftUse(context.getPlayer(), itemStack);
                    }
                    return (context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
                } else {
                    return interactable.onDataStickUse(context.getPlayer(), itemStack);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
