package com.gregtechceu.gtceu.api.sync_system.data_transformers;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import com.gregtechceu.gtceu.api.sync_system.NBTSerializable;

import javax.annotation.Nullable;

public class NBTSerializableTransformer implements ValueTransformer<NBTSerializable<Tag>> {

    @Override
    public Tag serializeNBT(NBTSerializable<Tag> value,
                            ValueTransformer.TransformerContext<NBTSerializable<Tag>> context) {
        return value.serializeNBT(context.lookup());
    }

    @Override
    public @Nullable NBTSerializable<Tag> deserializeNBT(Tag tag,
                                                          ValueTransformer.TransformerContext<NBTSerializable<Tag>> context) {
        var currentVal = context.currentValue();
        if (currentVal == null) {
            GTCEu.LOGGER.warn(
                    "Sync: Deserialization of NBTSerializable objects requires an existing object, they cannot be instantiated purely from saved data.");
            return null;
        }
        currentVal.deserializeNBT(tag, context.lookup());
        return currentVal;
    }

    private static final String WRAPPED_TAG_KEY = "$$field$$";

    @Override
    public void writeToPacket(FriendlyByteBuf buf, NBTSerializable<Tag> value,
                              TransformerContext<NBTSerializable<Tag>> context) {
        Tag data = value.serializeNBT(context.lookup());
        if (data instanceof CompoundTag compoundTag) {
            buf.writeNbt(compoundTag);
        } else {
            CompoundTag wrapper = new CompoundTag();
            wrapper.put(WRAPPED_TAG_KEY, data);
            buf.writeNbt(wrapper);
        }
    }

    @Override
    public @Nullable NBTSerializable<Tag> readFromPacket(FriendlyByteBuf buf,
                                                          TransformerContext<NBTSerializable<Tag>> context) {
        var currentVal = context.currentValue();
        Tag read = buf.readNbt();
        if (read instanceof CompoundTag compound && compound.size() == 1 && compound.contains(WRAPPED_TAG_KEY)) {
            read = compound.get(WRAPPED_TAG_KEY);
        }
        if (currentVal == null) {
            GTCEu.LOGGER.warn(
                    "Sync: Deserialization of NBTSerializable objects requires an existing object, they cannot be instantiated purely from a client packet.");
            return null;
        }
        if (read != null) currentVal.deserializeNBT(read, context.lookup());
        return currentVal;
    }
}
