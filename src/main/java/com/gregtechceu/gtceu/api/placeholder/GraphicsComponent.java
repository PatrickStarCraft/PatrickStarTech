package com.gregtechceu.gtceu.api.placeholder;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorRenderSnapshot;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GraphicsComponent(float x, float y, float x2, float y2, String rendererId, CompoundTag renderData) {

    public GraphicsComponent(double x, double y, double x2, double y2, String rendererId, CompoundTag renderData) {
        this((float) x, (float) y, (float) x2, (float) y2, rendererId, renderData);
    }

    public static final Codec<GraphicsComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(GraphicsComponent::x),
            Codec.FLOAT.fieldOf("y").forGetter(GraphicsComponent::y),
            Codec.FLOAT.fieldOf("x2").forGetter(GraphicsComponent::x2),
            Codec.FLOAT.fieldOf("y2").forGetter(GraphicsComponent::y2),
            Codec.STRING.fieldOf("rendererId").forGetter(GraphicsComponent::rendererId),
            CompoundTag.CODEC.fieldOf("renderData").forGetter(GraphicsComponent::renderData))
            .apply(instance, GraphicsComponent::new));

    public MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group,
                                                    float partialTick) {
        var renderer = PlaceholderHandler.getRenderer(this.rendererId, this.renderData);
        return renderer == null ? null : renderer.extractRenderState(machine, group, partialTick, this.renderData.copy());
    }

    public Tag toTag() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow(message -> { GTCEu.LOGGER.error(message); return new RuntimeException(message); });
    }

    public static GraphicsComponent fromTag(Tag tag) {
        return CODEC.decode(NbtOps.INSTANCE, tag).getOrThrow(message -> { GTCEu.LOGGER.error(message); return new RuntimeException(message); }).getFirst();
    }
}
