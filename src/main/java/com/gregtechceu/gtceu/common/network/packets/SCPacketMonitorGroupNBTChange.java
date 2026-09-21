package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.common.network.GTNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class SCPacketMonitorGroupNBTChange implements GTNetwork.INetPacket {

    private final ItemStack stack;
    private final int monitorGroupId;
    private final BlockPos pos;

    public SCPacketMonitorGroupNBTChange(ItemStack stack, MonitorGroup group, CentralMonitorMachine machine) {
        this.stack = stack;
        this.monitorGroupId = machine.getMonitorGroups().indexOf(group);
        this.pos = machine.getBlockPos();
    }

    public SCPacketMonitorGroupNBTChange(RegistryFriendlyByteBuf buf) {
        this.stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.monitorGroupId = buf.readVarInt();
        this.pos = buf.readBlockPos();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        buffer.writeVarInt(monitorGroupId);
        buffer.writeBlockPos(pos);
    }

    @Override
    public void execute(IPayloadContext context) {
        Level level = context.player().level();
        if (!level.hasChunkAt(pos)) return;
        if (context.player() instanceof ServerPlayer player &&
                (!player.mayInteract(player.level(), pos) ||
                        player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64)) return;

        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (machine instanceof CentralMonitorMachine centralMonitor) {
            if (monitorGroupId < 0 || monitorGroupId >= centralMonitor.getMonitorGroups().size()) return;
            IItemHandlerModifiable itemHandler = centralMonitor.getMonitorGroups().get(monitorGroupId)
                    .getItemStackHandler();
            if (ItemStack.isSameItem(itemHandler.getStackInSlot(0), stack)) {
                itemHandler.setStackInSlot(0, stack);
            }
        }
    }

}
