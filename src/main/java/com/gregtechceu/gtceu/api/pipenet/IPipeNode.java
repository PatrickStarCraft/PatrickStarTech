package com.gregtechceu.gtceu.api.pipenet;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.blockentity.IGregtechBlockEntity;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.blockentity.ITickSubscription;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.client.model.GTModelProperties;
import com.gregtechceu.gtceu.client.model.CoverRenderState;
import com.gregtechceu.gtceu.client.model.item.FacadeRenderState;
import com.gregtechceu.gtceu.common.cover.FacadeCover;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public interface IPipeNode<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType>
                          extends ITickSubscription, IPaintable, IGregtechBlockEntity {

    long getOffsetTimer();

    /**
     * Get Cover Container.
     */
    ICoverable getCoverContainer();

    /**
     * If tube is set to block connection from the specific side
     * 
     * @param side face
     */
    default boolean isBlocked(Direction side) {
        return PipeBlockEntity.isFaceBlocked(getBlockedConnections(), side);
    }

    /**
     * Unsafe!!! to set internal connections.
     * In general, you shouldn't call it yourself.
     */
    void setConnections(int connections);

    int getConnections();

    int getNumConnections();

    /**
     * set to block connection from the specific side
     * 
     * @param side      face
     * @param isBlocked is blocked
     */
    void setBlocked(Direction side, boolean isBlocked);

    /**
     * Whether pipe can attach to specific side.
     * e.g. check if there is an energyContainer nearby.
     */
    boolean canAttachTo(Direction side);

    /**
     * get connections for rendering and collision.
     */
    int getVisualConnections();

    /**
     * If node is connected to the specific side
     * 
     * @param side face
     */
    default boolean isConnected(Direction side) {
        return PipeBlockEntity.isConnected(getConnections(), side);
    }

    void setConnection(Direction side, boolean connected, boolean fromNeighbor);

    // if a face is blocked it will still render as connected, but it won't be able to receive stuff from that direction
    default boolean canHaveBlockedFaces() {
        return true;
    }

    int getBlockedConnections();

    @SuppressWarnings("unchecked")
    default PipeBlock<PipeType, NodeDataType, ?> getPipeBlock() {
        return (PipeBlock<PipeType, NodeDataType, ?>) self().getBlockState().getBlock();
    }

    @Nullable
    default PipeNet<NodeDataType> getPipeNet() {
        if (self().getLevel() instanceof ServerLevel serverLevel) {
            return getPipeBlock().getWorldPipeNet(serverLevel).getNetFromPos(self().getBlockPos());
        }
        return null;
    }

    default PipeType getPipeType() {
        return getPipeBlock().pipeType;
    }

    @Nullable
    default NodeDataType getNodeData() {
        var net = getPipeNet();
        if (net != null) {
            return net.getNodeAt(self().getBlockPos()).data;
        }
        return null;
    }

    void notifyBlockUpdate();

    default void serverTick() {}

    @Override
    default int getDefaultPaintingColor() {
        return 0xFFFFFF;
    }

    @Nullable
    Material getFrameMaterial();

    @ApiStatus.Internal
    @Override
    default @NotNull ModelData getModelData() {
        var pos = self().getBlockPos();
        var builder = ModelData.builder()
                .with(GTModelProperties.PIPE_CONNECTION_MASK, this.getVisualConnections())
                .with(GTModelProperties.PIPE_BLOCKED_MASK, this.getBlockedConnections());

        int paintingColor = getPaintingColor();
        if (isPainted() && paintingColor != -1) {
            builder.with(GTModelProperties.PIPE_PAINTING_COLOR, paintingColor);
        }
        Material frameMaterial = getFrameMaterial();
        if (frameMaterial != null) {
            BlockState frameState = java.util.Objects.requireNonNull(
                    GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, frameMaterial))
                    .getDefaultState();
            builder.with(GTModelProperties.PIPE_FRAME_STATE, frameState);
        }

        if (self().getLevel() instanceof BlockAndTintGetter renderLevel) {
            ModelData parentModelData = builder.build();
            var result = parentModelData.derive();
            ICoverable coverable = getCoverContainer();
            Map<Direction, ModelData> coverModelData = new EnumMap<>(Direction.class);
            Map<Direction, FacadeRenderState.Facade> facades = new EnumMap<>(Direction.class);
            EnumSet<Direction> occupiedFaces = EnumSet.noneOf(Direction.class);
            for (Direction direction : Direction.values()) {
                CoverBehavior cover = coverable.getCoverAtSide(direction);
                if (cover == null) continue;

                occupiedFaces.add(direction);
                coverModelData.put(direction, cover.getCoverRenderer().get().getModelData(
                        cover, pos, renderLevel, parentModelData));
                if (cover instanceof FacadeCover facadeCover) {
                    facades.put(direction, new FacadeRenderState.Facade(facadeCover.getFacadeState(),
                            facadeCover.shouldRenderPlate(), coverable.shouldRenderBackSide()));
                }
            }
            result.with(GTModelProperties.COVER_MODEL_DATA, Map.copyOf(coverModelData));
            result.with(GTModelProperties.COVER_RENDER_STATE, CoverRenderState.capture(coverable));
            result.with(GTModelProperties.FACADE_RENDER_STATE, new FacadeRenderState(facades,
                    coverable.getCoverPlateThickness(), occupiedFaces));
            return result.build();
        }
        return builder.build();
    }
}
