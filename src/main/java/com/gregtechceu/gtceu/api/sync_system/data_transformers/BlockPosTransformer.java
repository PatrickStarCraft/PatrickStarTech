package com.gregtechceu.gtceu.api.sync_system.data_transformers;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.Nullable;

public class BlockPosTransformer implements ValueTransformer<BlockPos> {

    @Override
    public Tag serializeNBT(BlockPos value, TransformerContext<BlockPos> context) {
        return BlockPos.CODEC.encodeStart(context.nbtOps(), value).getOrThrow(message -> { GTCEu.LOGGER.error(message); return new RuntimeException(message); });
    }

    @Override
    public @Nullable BlockPos deserializeNBT(Tag tag, TransformerContext<BlockPos> context) {
        if (tag instanceof CompoundTag compoundTag) {
            return new BlockPos(compoundTag.getIntOr("X", 0), compoundTag.getIntOr("Y", 0),
                    compoundTag.getIntOr("Z", 0));
        }
        return BlockPos.CODEC.parse(context.nbtOps(), tag).getOrThrow(message -> { GTCEu.LOGGER.error(message); return new RuntimeException(message); });
    }

    @Override
    public void writeToPacket(FriendlyByteBuf buf, BlockPos value, TransformerContext<BlockPos> context) {
        buf.writeBlockPos(value);
    }

    @Override
    public @Nullable BlockPos readFromPacket(FriendlyByteBuf buf, TransformerContext<BlockPos> context) {
        return buf.readBlockPos();
    }
}
