package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;

import org.jspecify.annotations.NullMarked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@NullMarked
public class MetaMachineItem extends BlockItem {

    private BiFunction<ItemStack, Integer, Integer> tintColor = (stack, index) -> -1;

    public MetaMachineItem(MetaMachineBlock block, Properties properties) {
        super(block, properties);
    }

    public MachineDefinition getDefinition() {
        return ((MetaMachineBlock) getBlock()).getDefinition();
    }

    public void setTintColor(BiFunction<ItemStack, Integer, Integer> tintColor) {
        this.tintColor = tintColor;
    }

    public int getTintColor(ItemStack stack, int index) {
        return this.tintColor.apply(stack, index);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        List<Component> components = new ArrayList<>();
        getDefinition().getTooltipBuilder().accept(stack, components);
        String mainKey = String.format("%s.machine.%s.tooltip", getDefinition().getId().getNamespace(),
                getDefinition().getId().getPath());
        if (Language.getInstance().has(mainKey)) {
            components.add(Math.min(1, components.size()), Component.translatable(mainKey));
        }
        components.forEach(tooltip);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();

        boolean superVal = super.placeBlock(context, state);

        if (!level.isClientSide()) {
            BlockPos possiblePipe = pos.offset(side.getOpposite().getUnitVec3i());
            Block block = level.getBlockState(possiblePipe).getBlock();
            if (block instanceof PipeBlock<?, ?, ?>) {
                IPipeNode pipeTile = ((PipeBlock<?, ?, ?>) block).getPipeTile(level, possiblePipe);
                if (pipeTile != null && ((PipeBlock<?, ?, ?>) block).canPipeConnectToBlock(pipeTile, side.getOpposite(),
                        level.getBlockEntity(pos))) {
                    pipeTile.setConnection(side, true, false);
                }
            }
        }
        return superVal;
    }

}
