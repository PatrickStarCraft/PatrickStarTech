package com.gregtechceu.gtceu.client.model.machine;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.client.renderer.cover.DynamicCoverRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Per-frame machine render input. It intentionally contains no block entity, world, or baked-model reference. */
public final class MachineRenderSnapshot extends BlockEntityRenderState {

    private BlockState blockState = Blocks.AIR.defaultBlockState();
    private MachineDefinition definition;
    private List<DynamicRenderEntry> dynamicRenders = List.of();
    private List<DynamicCoverRenderSnapshot> dynamicCovers = List.of();

    public BlockState blockState() {
        return this.blockState;
    }

    public List<DynamicRenderEntry> dynamicRenders() {
        return this.dynamicRenders;
    }

    public MachineDefinition definition() {
        return this.definition;
    }

    public List<DynamicCoverRenderSnapshot> dynamicCovers() {
        return this.dynamicCovers;
    }

    public void capture(MachineDefinition definition, BlockState blockState,
                        List<DynamicRenderEntry> dynamicRenders,
                        List<DynamicCoverRenderSnapshot> dynamicCovers) {
        this.definition = definition;
        this.blockState = blockState;
        this.dynamicRenders = List.copyOf(dynamicRenders);
        this.dynamicCovers = List.copyOf(dynamicCovers);
    }

    public record DynamicRenderEntry(int descriptorIndex, DynamicRenderSnapshot snapshot) {}
}
