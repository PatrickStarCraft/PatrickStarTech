package com.gregtechceu.gtceu.api.sync_system.managed;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.ExtraCodecs;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A BlockEntity that manages sync and save data via the {@code ISyncManaged} syncdata system.
 * 
 * @see ISyncManaged
 */
public abstract class ManagedSyncBlockEntity extends BlockEntity implements ISyncManaged {

    @Getter
    protected final SyncDataHolder syncDataHolder = new SyncDataHolder(this);
    @Getter
    @Setter
    private boolean isDirty;

    public ManagedSyncBlockEntity(BlockEntityCreationInfo info) {
        super(info.type(), info.pos(), info.state());
    }

    public ManagedSyncBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Saves BE data to world save.
     */
    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(getSyncDataHolder().serializeNBT(getHolderLookup()));
    }

    /**
     * Loads BE data from world save.<br>
     * Override this to add logic for modifying saved data before it is loaded (e.g. for cross-version
     * compatibility).<br>
     * When overriding, {@code super.load(tag)} must be called <b>AFTER</b> any custom logic.
     *
     * @param tag The tag to load
     */
    @Override
    @MustBeInvokedByOverriders
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        byte[] data = input.read("data", ExtraCodecs.NBT).filter(ByteArrayTag.class::isInstance)
                .map(ByteArrayTag.class::cast).map(ByteArrayTag::getAsByteArray).orElse(new byte[0]);
        if (data.length > 0) {
            getSyncDataHolder().readClientPacket(getHolderLookup(), new FriendlyByteBuf(Unpooled.wrappedBuffer(data)));
        } else {
            CompoundTag savedData = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
            getSyncDataHolder().deserializeNBT(getHolderLookup(), savedData);
        }
    }

    /**
     * Called to gather BE data to be sent when a client loads this BE.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return writeClientPacket(true);
    }

    /**
     * Called to get an update packet which is sent to clients to notify them when a loaded BE's data changes.
     */
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, registries) -> writeClientPacket(false));
    }

    private CompoundTag writeClientPacket(boolean fullSync) {
        var stream = new FriendlyByteBuf(Unpooled.buffer());
        if (fullSync) getSyncDataHolder().resyncAllFields();
        getSyncDataHolder().writeClientPacket(getHolderLookup(), stream);

        stream.capacity(stream.readableBytes());
        CompoundTag data = new CompoundTag();
        data.putByteArray("data", stream.array());
        return data;
    }

    private HolderLookup.Provider getHolderLookup() {
        Level level = getLevel();
        return level == null ? GTRegistries.builtinRegistry() : level.registryAccess();
    }

    @Override
    public @Nullable ISyncManaged getParentSyncObject() {
        return null;
    }

    @Override
    public final void markAsChanged() {
        isDirty = true;
    }

    @Override
    public void setChanged() {
        if (getLevel() != null) {
            getLevel().blockEntityChanged(getBlockPos());
        }
    }

    /**
     * Called each tick on the server side.
     */
    @MustBeInvokedByOverriders
    public void serverTick() {
        setChanged();
        if (isDirty) {
            Objects.requireNonNull(getLevel()).sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
            isDirty = false;
        }
    }

    /**
     * Called each tick on the client side.
     */
    public void clientTick() {}
}
