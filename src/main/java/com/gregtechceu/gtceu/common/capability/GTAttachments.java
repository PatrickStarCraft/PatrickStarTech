package com.gregtechceu.gtceu.common.capability;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.compat.EUToFEProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class GTAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GTCEu.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MedicalConditionTracker>> MEDICAL_CONDITION_TRACKER =
            ATTACHMENT_TYPES.register("medical_condition_tracker", () -> AttachmentType
                    .<MedicalConditionTracker>builder(holder -> new MedicalConditionTracker((Player) holder))
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public MedicalConditionTracker read(IAttachmentHolder holder, net.minecraft.world.level.storage.ValueInput input) {
                            MedicalConditionTracker tracker = new MedicalConditionTracker((Player) holder);
                            input.read("data", CompoundTag.CODEC).ifPresent(tracker::deserializeNBT);
                            return tracker;
                        }

                        @Override
                        public boolean write(MedicalConditionTracker tracker,
                                             net.minecraft.world.level.storage.ValueOutput output) {
                            output.store("data", CompoundTag.CODEC, tracker.serializeNBT());
                            return true;
                        }
                    })
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EUToFEProvider>> EU_TO_FE_PROVIDER =
            ATTACHMENT_TYPES.register("eu_to_fe_provider", () -> AttachmentType
                    .<EUToFEProvider>builder(holder -> new EUToFEProvider((BlockEntity) holder))
                    .build());

    private GTAttachments() {}

    public static void init(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
