package com.gregtechceu.gtceu.api.item;

import net.minecraft.nbt.Tag;
import com.gregtechceu.gtceu.api.sync_system.NBTSerializable;

/**
 * An interface for stack-owned state that needs custom merge preparation before item stacks are compared.
 */
public interface IMergeableNBTSerializable extends NBTSerializable<Tag> {

    /**
     * Called immediately before the serialized state is compared with another stack's state.
     * 
     * @param other the other stack's serializable state, or {@code null} when it is absent
     */
    void prepareForComparisonWith(NBTSerializable<Tag> other);
}
