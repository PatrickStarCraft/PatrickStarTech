package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.client.model.machine.MachineOutputRenderState;
import com.gregtechceu.gtceu.client.model.machine.ControllerPartRenderState;
import com.gregtechceu.gtceu.client.model.item.FacadeRenderState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

import java.util.Map;
import java.util.Objects;

public class GTModelProperties {

    public static final ModelProperty<BlockAndTintGetter> LEVEL = new ModelProperty<>();
    public static final ModelProperty<BlockPos> POS = new ModelProperty<>();
    public static final ModelProperty<MachineRenderState> MACHINE_RENDER_STATE = new ModelProperty<>();
    public static final ModelProperty<MachineOutputRenderState> MACHINE_OUTPUT_RENDER_STATE = new ModelProperty<>();
    public static final ModelProperty<ControllerPartRenderState> FORMED_PART_RENDER_STATE = new ModelProperty<>();
    public static final ModelProperty<ModelData> PARENT_MODEL_DATA = new ModelProperty<>();

    public static final ModelProperty<Map<Direction, ModelData>> COVER_MODEL_DATA = new ModelProperty<>();
    public static final ModelProperty<CoverRenderState> COVER_RENDER_STATE = new ModelProperty<>();
    public static final ModelProperty<FacadeRenderState> FACADE_RENDER_STATE = new ModelProperty<>();

    public static final ModelProperty<Integer> PIPE_CONNECTION_MASK = new ModelProperty<>();
    public static final ModelProperty<Integer> PIPE_BLOCKED_MASK = new ModelProperty<>();
    public static final ModelProperty<Integer> PIPE_PAINTING_COLOR = new ModelProperty<>();
    public static final ModelProperty<BlockState> PIPE_FRAME_STATE = new ModelProperty<>();

    public static final ModelProperty<ModelData> CHILD_MODEL_DATA = new ModelProperty<>(Objects::nonNull);
}
