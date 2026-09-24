package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.IMergeableNBTSerializable;
import com.gregtechceu.gtceu.api.item.ISpoilableItemStackExtension;
import com.gregtechceu.gtceu.api.item.component.*;
import com.gregtechceu.gtceu.api.sync_system.NBTSerializable;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ISpoilableItemStackExtension {

    // ************************* //
    // Shadow fields and methods //
    // ************************* //

    @Shadow
    @Mutable
    @Final
    @Nullable
    private Holder<Item> item;

    @Shadow
    @Mutable
    @Final
    private PatchedDataComponentMap components;

    // ************* //
    // Unique fields //
    // ************* //

    @Shadow
    private int count;
    /**
     * Whether {@link ItemStackMixin#gtceu$updateFreshness(SpoilContext, boolean)}
     * was called and did not return yet.
     * <br>
     * Used to prevent stack overflows.
     */
    @Unique
    private boolean gtceu$isUpdating = false;

    /**
     * Whether to display a fake "spoils into" tooltip for an item if it's not spoilable.
     * <br>
     * Has a 10% chance of being {@code true} for any stack on april fools.
     * The chance is rolled once in the constructor (injected by {@link ItemStackMixin#gtceu$injectFakeTooltipInit}).
     */
    @Unique
    private boolean gtceu$fakeTooltip;

    @Unique
    private ItemStack gtceu$self() {
        return (ItemStack) (Object) this;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static @Nullable NBTSerializable<Tag> gtceu$asSerializable(@Nullable Object value) {
        return value instanceof NBTSerializable<?> serializable ? (NBTSerializable<Tag>) serializable : null;
    }

    @Unique
    private static void gtceu$prepareSpoilageMerge(ItemStack first, ItemStack second) {
        if (!ItemStack.isSameItem(first, second)) return;
        var firstSpoilable = GTCapabilityHelper.getSpoilable(first);
        var secondSpoilable = GTCapabilityHelper.getSpoilable(second);
        if (firstSpoilable instanceof IMergeableNBTSerializable mergeable) {
            mergeable.prepareForComparisonWith(gtceu$asSerializable(secondSpoilable));
        }
        if (secondSpoilable instanceof IMergeableNBTSerializable mergeable) {
            mergeable.prepareForComparisonWith(gtceu$asSerializable(firstSpoilable));
        }
    }

    // ************************* //
    // Interface implementations //
    // ************************* //

    @Unique
    @Override
    public void gtceu$setStack(ItemStack newStack) {
        ItemStackAccessor source = (ItemStackAccessor) (Object) newStack;
        item = source.gtceu$getItemHolder();
        count = newStack.getCount();
        components = source.gtceu$getComponents().copy();
    }

    @Unique
    public void gtceu$updateFreshness(@NotNull SpoilContext spoilContext, boolean createTag) {
        if (!gtceu$isUpdating) {
            gtceu$isUpdating = true;
            ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
            if (spoilable != null) spoilable.updateFreshness(spoilContext, createTag);
            gtceu$isUpdating = false;
        }
    }

    // ********* //
    // Injectors //
    // ********* //

    @Inject(at = @At("HEAD"), method = "isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z")
    private static void gtceu$mergeSpoilageBeforeComponentComparison(ItemStack first, ItemStack second,
                                                                     CallbackInfoReturnable<Boolean> cir) {
        gtceu$prepareSpoilageMerge(first, second);
    }

    @Inject(at = @At("HEAD"), method = { "getItem", "getCount" })
    private void gtceu$injectedFreshnessUpdate(CallbackInfoReturnable<?> cir) {
        gtceu$updateFreshness(new SpoilContext(), false);
    }

    @Inject(at = @At("HEAD"), method = "inventoryTick")
    private void gtceu$tickFreshness(Level level, Entity entity, @Nullable EquipmentSlot equipmentSlot,
                                     CallbackInfo ci) {
        if (entity instanceof Player player) {
            int inventorySlot = -1;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == gtceu$self()) {
                    inventorySlot = i;
                    break;
                }
            }
            gtceu$updateFreshness(new SpoilContext(player, inventorySlot), true);
        }
        else gtceu$updateFreshness(new SpoilContext(entity), true);
    }

    @Inject(at = @At("HEAD"), method = "onCraftedBy")
    private void gtceu$updateFreshnessOnCraft(Player player, int amount, CallbackInfo ci) {
        gtceu$updateFreshness(new SpoilContext(player, -1), true);
    }

    @Inject(at = @At("RETURN"),
            method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/PatchedDataComponentMap;)V")
    private void gtceu$injectFakeTooltipInit(Holder<Item> item, int count, PatchedDataComponentMap components,
                                             CallbackInfo ci) {
        gtceu$fakeTooltip = GTValues.FOOLS.getAsBoolean() && GTValues.RNG.nextFloat() < .1f;
    }

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IAddInformation} to
     * actually display the added tooltip.
     */
    @Inject(at = @At("TAIL"), method = "addDetailsToTooltip")
    private void gtceu$spoilageTooltip(Item.TooltipContext context, TooltipDisplay display,
                                       @Nullable Player player, TooltipFlag flag,
                                       Consumer<Component> tooltip, CallbackInfo ci) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (spoilable instanceof IAddInformation addInformation) {
            List<Component> lines = new ArrayList<>();
            addInformation.appendHoverText(gtceu$self(), player == null ? null : player.level(), lines, flag);
            lines.forEach(tooltip);
        } else if (gtceu$fakeTooltip) {
            tooltip.accept(Component.translatable(
                    "gtceu.tooltip.spoil_time_remaining",
                    Component.literal(FormattingUtil.formatTime(100)).withStyle(ChatFormatting.DARK_AQUA)));
            tooltip.accept(Component.translatable(
                    "gtceu.tooltip.spoils_into",
                    Items.DIRT.getDefaultInstance().getDisplayName()));
        }
    }

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IDurabilityBar} to
     * actually display the bar.
     */
    @Inject(at = @At("HEAD"), method = "isBarVisible", cancellable = true)
    private void gtceu$spoilageBarVisible(CallbackInfoReturnable<Boolean> cir) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.isBarVisible(gtceu$self()));
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IDurabilityBar} to
     * actually display the bar.
     */
    @Inject(at = @At("HEAD"), method = "getBarColor", cancellable = true)
    private void gtceu$spoilageBarColor(CallbackInfoReturnable<Integer> cir) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.getBarColor(gtceu$self()));
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(ARGB.color(255, 255, 255, 255));
        }
    }

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IDurabilityBar} to
     * actually display the bar.
     */
    @Inject(at = @At("HEAD"), method = "getBarWidth", cancellable = true)
    private void gtceu$spoilageBarWidth(CallbackInfoReturnable<Integer> cir) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.getBarWidth(gtceu$self()));
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(13);
        }
    }
}
