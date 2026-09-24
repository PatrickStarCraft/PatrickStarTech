package com.gregtechceu.gtceu.common.item.tool.behavior;

import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.item.tool.behavior.IToolBehavior;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.misc.forge.VoidFluidHandlerItemStack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlungerBehavior implements IToolBehavior, IComponentCapability {

    public static final PlungerBehavior INSTANCE = PlungerBehavior.create();

    protected PlungerBehavior() {/**/}

    protected static PlungerBehavior create() {
        return new PlungerBehavior();
    }

    @Override
    public boolean shouldOpenUIAfterUse(UseOnContext context) {
        return !(context.getPlayer() != null && context.getPlayer().isShiftKeyDown());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null && !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        IFluidHandler fluidHandler;
        if (level.getBlockEntity(context.getClickedPos()) instanceof MetaMachine machine) {
            fluidHandler = machine.getFluidHandlerCap(context.getClickedFace(), false);
        } else {
            // noinspection DataFlowIssue
            fluidHandler = GTCapabilityHelper.getFluidHandler(level, context.getClickedPos(), context.getClickedFace());
        }
        if (fluidHandler == null) {
            return InteractionResult.PASS;
        }

        FluidStack drained = fluidHandler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
        if (!drained.isEmpty()) {
            fluidHandler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
            ToolHelper.onActionDone(player, stack, level, context.getClickLocation());
            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void addInformation(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip,
                               @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gtceu.tool.behavior.plunger"));
    }

    @Override
    public ResourceHandler<FluidResource> createFluidHandler(ItemAccess access) {
        return new VoidFluidResourceHandler(access);
    }

    private static final class VoidFluidResourceHandler extends SnapshotJournal<Integer>
            implements ResourceHandler<FluidResource> {

        private final ItemAccess access;
        private final Item item;
        private int committedInsertions;

        private VoidFluidResourceHandler(ItemAccess access) {
            this.access = access;
            this.item = access.getResource().getItem();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            java.util.Objects.checkIndex(index, size());
            return FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            java.util.Objects.checkIndex(index, size());
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            java.util.Objects.checkIndex(index, size());
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            java.util.Objects.checkIndex(index, size());
            return access.getAmount() > 0 && access.getResource().is(item);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            java.util.Objects.checkIndex(index, size());
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (amount == 0 || !isValid(index, resource)) {
                return 0;
            }
            updateSnapshots(transaction);
            committedInsertions++;
            return amount;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            java.util.Objects.checkIndex(index, size());
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }

        @Override
        protected Integer createSnapshot() {
            return committedInsertions;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            committedInsertions = snapshot;
        }

        @Override
        protected void onRootCommit(Integer initialInsertions) {
            int insertions = committedInsertions - initialInsertions;
            committedInsertions = 0;
            for (int i = 0; i < insertions; i++) {
                damageCommittedItem();
            }
        }

        private void damageCommittedItem() {
            ItemResource current = access.getResource();
            if (access.getAmount() == 0 || !current.is(item)) {
                return;
            }

            ItemStack damaged = current.toStack();
            ToolHelper.damageItem(damaged, null);
            try (Transaction transaction = Transaction.open(null)) {
                int changed = damaged.isEmpty() ? access.extract(current, 1, transaction) :
                        access.exchange(ItemResource.of(damaged), 1, transaction);
                if (changed > 0) {
                    transaction.commit();
                }
            }
        }
    }
}
