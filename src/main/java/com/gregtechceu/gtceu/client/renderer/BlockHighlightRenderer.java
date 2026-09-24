package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.IToolGridHighlight;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.pipenet.IPipeType;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.common.item.behavior.CoverPlaceBehavior;
import com.gregtechceu.gtceu.common.item.tool.rotation.CustomBlockRotations;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import brachy.modularui.drawable.UITexture;
import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Set;
import java.util.function.Function;

import static com.gregtechceu.gtceu.utils.GTMatrixUtils.*;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public class BlockHighlightRenderer {

    @SubscribeEvent
    public static void extractBlockHighlight(ExtractBlockOutlineRenderStateEvent event) {
        var mc = Minecraft.getInstance();
        var level = event.getLevel();
        var player = mc.player;
        if (player != null) {
            ItemStack held = player.getMainHandItem();
            BlockPos blockPos = event.getBlockPos();
            BlockHitResult target = event.getHitResult();
            Vector3fc blockCenter = net.minecraft.world.phys.Vec3.atCenterOf(blockPos).toVector3f();
            GeometryBuilder geometry = new GeometryBuilder();

            Set<GTToolType> toolType = ToolHelper.getToolTypes(held);
            BlockEntity blockEntity = level.getBlockEntity(blockPos);

            // draw tool grid highlight
            if ((!toolType.isEmpty()) || (held.isEmpty() && player.isShiftKeyDown())) {
                IToolGridHighlight gridHighlight = null;
                if (blockEntity instanceof IToolGridHighlight highLight) {
                    gridHighlight = highLight;
                } else if (level.getBlockState(blockPos).getBlock() instanceof IToolGridHighlight highLight) {
                    gridHighlight = highLight;
                } else if (toolType.contains(GTToolType.WRENCH)) {
                    var behavior = CustomBlockRotations.getCustomRotation(level.getBlockState(blockPos).getBlock());
                    if (behavior != null && behavior.showGrid()) {
                        gridHighlight = new IToolGridHighlight() {

                            @Override
                            public @Nullable UITexture sideTips(@NotNull Player player, @NotNull BlockPos pos,
                                                                @NotNull BlockState state,
                                                                @NotNull Set<GTToolType> toolTypes,
                                                                ItemStack held, @NotNull Direction side) {
                                return behavior.showSideTip(state, side) ? GTGuiTextures.TOOL_FRONT_FACING_ROTATION :
                                        null;
                            }
                        };
                    }
                }
                if (gridHighlight == null) {
                    return;
                }
                BlockState state = event.getBlockState();
                if (gridHighlight.shouldRenderGrid(player, blockPos, state, held, toolType)) {
                    final IToolGridHighlight finalGridHighlight = gridHighlight;
                    collectGridOverlays(geometry, target,
                            side -> finalGridHighlight.sideTips(player, blockPos, state, toolType, held, side));
                } else {
                    Direction facing = target.getDirection();
                    var texture = gridHighlight.sideTips(player, blockPos, state, toolType, held, facing);
                    if (texture != null) {
                        PoseStack poseStack = new PoseStack();
                        poseStack.translate(facing.getStepX() * 0.01f, facing.getStepY() * 0.01f,
                                facing.getStepZ() * 0.01f);

                        RenderUtil.moveToFace(poseStack, blockCenter, facing);
                        if (facing.getAxis() == Direction.Axis.Y) {
                            RenderUtil.rotateToFace(poseStack, facing, Direction.SOUTH);
                        } else {
                            RenderUtil.rotateToFace(poseStack, facing, Direction.NORTH);
                        }
                        poseStack.scale(1f / 16, 1f / 16, 0);
                        poseStack.translate(-8, -8, 0);

                        geometry.addOverlay(poseStack, texture, 0xffffffff,
                                4, 4, 8, 8);
                    }
                }
                addCustomRenderer(event, geometry);
                return;
            }

            // draw cover grid highlight
            ICoverable coverable = GTCapabilityHelper.getCoverable(level, blockPos, target.getDirection());
            if (coverable != null && CoverPlaceBehavior.isCoverBehaviorItem(held, coverable::hasAnyCover,
                    coverDef -> ICoverable.canPlaceCover(coverDef, coverable))) {
                collectGridOverlays(geometry, target,
                        side -> coverable.hasCover(side) ? null : GTGuiTextures.TOOL_ATTACH_COVER);
            }

            // draw pipe connection grid highlight
            var pipeType = held.getItem() instanceof PipeBlockItem pipeBlockItem ? pipeBlockItem.getBlock().pipeType :
                    null;
            if (pipeType instanceof IPipeType<?> type && blockEntity instanceof PipeBlockEntity<?, ?> pipeBlockEntity &&
                    pipeBlockEntity.getPipeType().type().equals(type.type())) {
                collectGridOverlays(geometry, target,
                        side -> level.isEmptyBlock(blockPos.relative(side)) ?
                                pipeBlockEntity.getPipeTexture(true) : null);
            }
            addCustomRenderer(event, geometry);
        }
    }

    private static void collectGridOverlays(GeometryBuilder geometry, BlockHitResult blockHitResult,
                                            Function<Direction, UITexture> texture) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        float minX = blockPos.getX();
        float maxX = blockPos.getX() + 1;
        float minY = blockPos.getY();
        float maxY = blockPos.getY() + 1;
        float maxZ = blockPos.getZ() + 1.01f;
        Direction attachSide = ICoverable.traceCoverSide(blockHitResult);
        Vector3f topRight = new Vector3f(maxX, maxY, maxZ);
        Vector3f bottomRight = new Vector3f(maxX, minY, maxZ);
        Vector3f bottomLeft = new Vector3f(minX, minY, maxZ);
        Vector3f topLeft = new Vector3f(minX, maxY, maxZ);
        Vector3f shiftX = new Vector3f(0.25f, 0, 0);
        Vector3f shiftY = new Vector3f(0, 0.25f, 0);

        Vector3f cubeCenter = net.minecraft.world.phys.Vec3.atCenterOf(blockPos).toVector3f();

        topRight.sub(cubeCenter);
        bottomRight.sub(cubeCenter);
        bottomLeft.sub(cubeCenter);
        topLeft.sub(cubeCenter);

        Direction front = blockHitResult.getDirection();
        Direction back = front.getOpposite();
        Direction left = RelativeDirection.LEFT.applyDirection(front);
        Direction right = RelativeDirection.RIGHT.applyDirection(front);
        Direction top = RelativeDirection.UP.applyDirection(front);
        Direction bottom = RelativeDirection.DOWN.applyDirection(front);
        if (front.getAxis() == Direction.Axis.Y) {
            Direction tmp = left;
            left = right;
            right = tmp;
        }

        Quaternionfc rotation = getRotation(Direction.SOUTH, front);
        topRight.rotate(rotation);
        bottomRight.rotate(rotation);
        bottomLeft.rotate(rotation);
        topLeft.rotate(rotation);
        shiftX.rotate(rotation);
        shiftY.rotate(rotation);

        UITexture leftBlocked = texture.apply(left);
        UITexture rightBlocked = texture.apply(right);
        UITexture topBlocked = texture.apply(top);
        UITexture bottomBlocked = texture.apply(bottom);
        UITexture frontBlocked = texture.apply(front);
        UITexture backBlocked = texture.apply(back);

        topRight.add(cubeCenter);
        bottomRight.add(cubeCenter);
        bottomLeft.add(cubeCenter);
        topLeft.add(cubeCenter);

        PoseStack poseStack = new PoseStack();
        PoseStack.Pose pose = poseStack.last();
        // straight top bottom lines
        geometry.addLine(pose, new Vector3f(topRight).sub(shiftX), new Vector3f(bottomRight).sub(shiftX));
        geometry.addLine(pose, new Vector3f(bottomLeft).add(shiftX), new Vector3f(topLeft).add(shiftX));
        // straight side to side lines
        geometry.addLine(pose, new Vector3f(topLeft).sub(shiftY), new Vector3f(topRight).sub(shiftY));
        geometry.addLine(pose, new Vector3f(bottomLeft).add(shiftY), new Vector3f(bottomRight).add(shiftY));

        poseStack.pushPose();
        poseStack.translate(front.getStepX() * 0.01f, front.getStepY() * 0.01f, front.getStepZ() * 0.01f);

        RenderUtil.moveToFace(poseStack, cubeCenter, front);
        RenderUtil.rotateToFace(poseStack, front, Direction.SOUTH);
        poseStack.scale(1f / 16, 1f / 16, 0);
        poseStack.translate(-8, -8, 0);

        // set margin to 1/18 of scaled texture edge length
        float MARGIN = 0.2f;

        if (leftBlocked != null) {
            int color = attachSide == left ? 0xffffffff : 0x44ffffff;
            addOverlayTextureWithMargin(geometry, poseStack, leftBlocked, color, 0, 6, MARGIN);
        }
        if (topBlocked != null) {
            int color = attachSide == top ? 0xffffffff : 0x44ffffff;
            addOverlayTextureWithMargin(geometry, poseStack, topBlocked, color, 6, 12, MARGIN);
        }
        if (rightBlocked != null) {
            int color = attachSide == right ? 0xffffffff : 0x44ffffff;
            addOverlayTextureWithMargin(geometry, poseStack, rightBlocked, color, 12, 6, MARGIN);
        }
        if (bottomBlocked != null) {
            int color = attachSide == bottom ? 0xffffffff : 0x44ffffff;
            addOverlayTextureWithMargin(geometry, poseStack, bottomBlocked, color, 6, 0, MARGIN);
        }
        if (frontBlocked != null) {
            int color = attachSide == front ? 0xffffffff : 0x44ffffff;
            addOverlayTextureWithMargin(geometry, poseStack, frontBlocked, color, 6, 6, MARGIN);
        }
        if (backBlocked != null) {
            int color = attachSide == back ? 0xffffffff : 0x44ffffff;
            addOverlayTextureWithMargin(geometry, poseStack, backBlocked, color, 0, 0, MARGIN);
            addOverlayTextureWithMargin(geometry, poseStack, backBlocked, color, 12, 0, MARGIN);
            addOverlayTextureWithMargin(geometry, poseStack, backBlocked, color, 0, 12, MARGIN);
            addOverlayTextureWithMargin(geometry, poseStack, backBlocked, color, 12, 12, MARGIN);
        }

        poseStack.popPose();
    }

    private static void addOverlayTextureWithMargin(GeometryBuilder geometry, PoseStack poseStack,
                                                    UITexture texture, int color,
                                                    float x, float y, float margin) {
        geometry.addOverlay(poseStack, texture, color,
                x + margin, y + margin, 4f - 2 * margin, 4f - 2 * margin);
    }

    private static void addCustomRenderer(ExtractBlockOutlineRenderStateEvent event, GeometryBuilder geometry) {
        HighlightGeometry snapshot = geometry.build();
        if (!snapshot.isEmpty()) {
            event.addCustomRenderer(new HighlightRenderer(snapshot));
        }
    }

    private record HighlightRenderer(HighlightGeometry geometry) implements CustomBlockOutlineRenderer {
        @Override
        public boolean render(BlockOutlineRenderState renderState, SubmitNodeCollector collector,
                              PoseStack poseStack, LevelRenderState levelRenderState) {
            Vec3 camera = levelRenderState.cameraRenderState.pos;
            if (!this.geometry.lines().isEmpty()) {
                collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
                    for (LineSegment line : this.geometry.lines()) {
                        Vector3f normal = new Vector3f(line.fromX() - line.toX(), line.fromY() - line.toY(),
                                line.fromZ() - line.toZ());
                        buffer.addVertex(pose, (float) (line.fromX() - camera.x()),
                                (float) (line.fromY() - camera.y()), (float) (line.fromZ() - camera.z()))
                                .setColor(this.geometry.red(), this.geometry.green(), this.geometry.blue(), 1f)
                                .setNormal(pose, normal).setLineWidth(3f);
                        buffer.addVertex(pose, (float) (line.toX() - camera.x()),
                                (float) (line.toY() - camera.y()), (float) (line.toZ() - camera.z()))
                                .setColor(this.geometry.red(), this.geometry.green(), this.geometry.blue(), 1f)
                                .setNormal(pose, normal).setLineWidth(3f);
                    }
                });
            }
            for (OverlayBatch overlay : this.geometry.overlays()) {
                RenderType renderType = RenderTypes.textSeeThrough(overlay.texture());
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                    for (OverlayVertex vertex : overlay.vertices()) {
                        buffer.addVertex(pose, (float) (vertex.x() - camera.x()),
                                (float) (vertex.y() - camera.y()), (float) (vertex.z() - camera.z()))
                                .setColor(vertex.color()).setUv(vertex.u(), vertex.v())
                                .setLight(LightCoordsUtil.FULL_BRIGHT);
                    }
                });
            }
            return false;
        }
    }

    private record HighlightGeometry(java.util.List<LineSegment> lines, java.util.List<OverlayBatch> overlays,
                                     float red, float green, float blue) {
        private boolean isEmpty() {
            return this.lines.isEmpty() && this.overlays.isEmpty();
        }
    }

    private record LineSegment(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {}

    private record OverlayVertex(float x, float y, float z, float u, float v, int color) {}

    private record OverlayBatch(net.minecraft.resources.Identifier texture, java.util.List<OverlayVertex> vertices) {}

    private static final class GeometryBuilder {
        private final java.util.List<LineSegment> lines = new java.util.ArrayList<>();
        private final java.util.Map<net.minecraft.resources.Identifier, java.util.List<OverlayVertex>> overlays =
                new java.util.LinkedHashMap<>();
        private final float pulse = 0.2F + (float) Math.sin((System.currentTimeMillis() % (Mth.PI * 800)) / 800) / 2;

        private void addLine(PoseStack.Pose pose, Vector3fc from, Vector3fc to) {
            Vector3f transformedFrom = pose.pose().transformPosition(from.x(), from.y(), from.z(), new Vector3f());
            Vector3f transformedTo = pose.pose().transformPosition(to.x(), to.y(), to.z(), new Vector3f());
            this.lines.add(new LineSegment(transformedFrom.x(), transformedFrom.y(), transformedFrom.z(),
                    transformedTo.x(), transformedTo.y(), transformedTo.z()));
        }

        private void addOverlay(PoseStack poseStack, UITexture texture, int color,
                                float x, float y, float width, float height) {
            float u0 = texture.u0;
            float u1 = texture.u1;
            float v0 = texture.v0;
            float v1 = texture.v1;
            java.util.List<OverlayVertex> vertices = this.overlays.computeIfAbsent(texture.location,
                    unused -> new java.util.ArrayList<>());
            // Keep the winding and UV orientation used by the original face overlays.
            this.addOverlayVertex(vertices, poseStack, x, y + height, u0, v0 + v1, color);
            this.addOverlayVertex(vertices, poseStack, x + width, y + height, u0 + u1, v0 + v1, color);
            this.addOverlayVertex(vertices, poseStack, x + width, y, u0 + u1, v0, color);
            this.addOverlayVertex(vertices, poseStack, x, y, u0, v0, color);
        }

        private void addOverlayVertex(java.util.List<OverlayVertex> vertices, PoseStack poseStack,
                                     float x, float y, float u, float v, int color) {
            Vector3f position = poseStack.last().pose().transformPosition(x, y, 0, new Vector3f());
            vertices.add(new OverlayVertex(position.x(), position.y(), position.z(), u, v, color));
        }

        private HighlightGeometry build() {
            java.util.List<OverlayBatch> batches = this.overlays.entrySet().stream()
                    .map(entry -> new OverlayBatch(entry.getKey(), java.util.List.copyOf(entry.getValue())))
                    .toList();
            return new HighlightGeometry(java.util.List.copyOf(this.lines), batches, this.pulse, this.pulse, 1f);
        }
    }
}
