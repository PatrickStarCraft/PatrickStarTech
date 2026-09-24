package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.block.FluidBlockRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.common.machine.trait.multiblock.MultiblockFluidRendererTrait;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidAreaRender extends DynamicRender<WorkableMultiblockMachine, FluidAreaRender> {

    public static final List<RelativeDirection> DEFAULT_FACES = Collections.singletonList(RelativeDirection.UP);

    // spotless:off
    @SuppressWarnings("deprecation")
    public static final MapCodec<FluidAreaRender> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidBlockRenderer.CODEC.forGetter(FluidAreaRender::getFluidBlockRenderer),
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fixed_fluid").forGetter(FluidAreaRender::getFixedFluid),
            RelativeDirection.CODEC.listOf().optionalFieldOf("drawn_faces", DEFAULT_FACES).forGetter(FluidAreaRender::getDrawFaces)
    ).apply(instance, FluidAreaRender::new));
    public static final DynamicRenderType<WorkableMultiblockMachine, FluidAreaRender> TYPE = new DynamicRenderType<>(FluidAreaRender.CODEC);
    // spotless:on

    @Getter
    private final FluidBlockRenderer fluidBlockRenderer;
    private final boolean fixedFluid;
    @Getter
    private final List<RelativeDirection> drawFaces;

    private @Nullable Fluid cachedFluid;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public FluidAreaRender(FluidBlockRenderer fluidBlockRenderer,
                           Optional<Fluid> fixedFluid, List<RelativeDirection> drawFaces) {
        this.fluidBlockRenderer = fluidBlockRenderer;
        if (fixedFluid.isPresent()) {
            this.fixedFluid = true;
            this.cachedFluid = fixedFluid.get();
        } else {
            this.fixedFluid = false;
        }
        this.drawFaces = drawFaces.isEmpty() ? DEFAULT_FACES : drawFaces;
    }

    @Override
    public DynamicRenderType<WorkableMultiblockMachine, FluidAreaRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public boolean shouldRender(WorkableMultiblockMachine machine, Vec3 cameraPos) {
        return machine.getTrait(MultiblockFluidRendererTrait.class) != null;
    }

    @Override
    public DynamicRenderSnapshot extractRenderState(WorkableMultiblockMachine machine, float partialTicks) {
        if (!ConfigHolder.INSTANCE.client.renderer.renderFluids || !machine.isFormed()) return null;

        var trait = machine.getTrait(MultiblockFluidRendererTrait.class);
        if (trait == null || trait.getFluidOffsets().isEmpty()) return null;

        Fluid fluid = this.fixedFluid ? this.cachedFluid : getRecipeFluid(machine);
        if (fluid == null) return null;

        BlockPos origin = machine.getBlockPos().immutable();
        List<FluidBlockRenderer.LitOffset> offsets = new ArrayList<>();
        for (BlockPos offset : trait.getFluidOffsets()) {
            BlockPos immutableOffset = new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            int light = RenderUtil.getFluidLight(fluid, origin.offset(immutableOffset), machine.getLevel());
            offsets.add(new FluidBlockRenderer.LitOffset(immutableOffset, light));
        }
        List<FluidBlockRenderer.LitOffset> copiedOffsets = List.copyOf(offsets);

        List<FluidPlane> planes = new ArrayList<>(this.drawFaces.size());
        for (RelativeDirection face : this.drawFaces) {
            Direction direction = face.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                    machine.isFlipped());
            if (direction.getAxis() != Direction.Axis.Y) direction = direction.getOpposite();
            planes.add(new FluidPlane(direction, copiedOffsets));
        }

        FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(fluid.defaultFluidState());
        return new FluidAreaSnapshot(fluid, fluidModel.layer(), List.copyOf(planes));
    }

    @Override
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                                 CameraRenderState camera) {
        if (!(state instanceof FluidAreaSnapshot snapshot) || snapshot.planes().isEmpty()) return;

        RenderType renderType = switch (snapshot.layer()) {
            case SOLID -> RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS);
            case CUTOUT -> RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS);
            case TRANSLUCENT -> RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        };
        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            for (FluidPlane plane : snapshot.planes()) {
                this.fluidBlockRenderer.drawPlane(plane.direction(), plane.offsets(), pose, consumer,
                        snapshot.fluid(), RenderUtil.FluidTextureType.STILL, 0);
            }
        });
    }

    private static Fluid getRecipeFluid(WorkableMultiblockMachine machine) {
        var recipe = machine.getRecipeLogic().getLastUnrolledRecipe();
        if (recipe == null || !machine.isActive()) return null;
        return RenderUtil.getRecipeFluidToRender(recipe);
    }

    private record FluidPlane(Direction direction, List<FluidBlockRenderer.LitOffset> offsets) {
        private FluidPlane {
            offsets = List.copyOf(offsets);
        }
    }

    private record FluidAreaSnapshot(Fluid fluid, ChunkSectionLayer layer, List<FluidPlane> planes)
            implements DynamicRenderSnapshot {
        private FluidAreaSnapshot {
            planes = List.copyOf(planes);
        }
    }

    private Optional<Fluid> getFixedFluid() {
        if (fixedFluid) return Optional.ofNullable(cachedFluid);
        else return Optional.empty();
    }

    @Override
    public boolean shouldRenderOffScreen(WorkableMultiblockMachine machine) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(WorkableMultiblockMachine machine) {
        AABB box = super.getRenderBoundingBox(machine);
        var trait = machine.getTrait(MultiblockFluidRendererTrait.class);
        if (trait == null) return box;
        var offsets = trait.getFluidOffsets();
        for (var offset : offsets) {
            box = box.minmax(new AABB(offset));
        }
        return box.inflate(getViewDistance());
    }
}
