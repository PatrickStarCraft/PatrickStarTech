package com.gregtechceu.gtceu.common.block;

import org.jspecify.annotations.NullMarked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.redstone.Orientation;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@NullMarked
public class LampBlock extends Block {

    public static final BooleanProperty BLOOM = BlockStateProperties.BLOOM;
    public static final BooleanProperty LIGHT = BlockStateProperties.LIT;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final String TAG_INVERTED = "inverted";
    public static final String TAG_BLOOM = "bloom";
    public static final String TAG_LIGHT = "lit";

    public static final int BLOOM_FLAG = 0b001;
    public static final int LIGHT_FLAG = 0b010;
    public static final int INVERTED_FLAG = 0b100;

    public final DyeColor color;
    public final boolean bordered;

    public LampBlock(Properties properties, DyeColor color, boolean bordered) {
        super(properties);
        this.color = color;
        this.bordered = bordered;
        registerDefaultState(defaultBlockState()
                .setValue(BLOOM, true)
                .setValue(LIGHT, true)
                .setValue(INVERTED, false)
                .setValue(POWERED, false));
    }

    public static boolean isLightActive(BlockState state) {
        return state.getValue(INVERTED) != state.getValue(POWERED);
    }

    public static boolean isInverted(CompoundTag tag) {
        return tag.getBooleanOr(TAG_INVERTED, false);
    }

    public static boolean isLightEnabled(CompoundTag tag) {
        return tag.getBooleanOr(TAG_LIGHT, false);
    }

    public static boolean isBloomEnabled(CompoundTag tag) {
        return tag.getBooleanOr(TAG_BLOOM, false);
    }

    public CompoundTag getTagFromState(BlockState state) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_BLOOM, state.getValue(BLOOM));
        tag.putBoolean(TAG_LIGHT, state.getValue(LIGHT));
        tag.putBoolean(TAG_INVERTED, state.getValue(INVERTED));
        return tag;
    }

    public ItemStack getStackFromIndex(int i) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(LampBlock.TAG_INVERTED, (i & LampBlock.INVERTED_FLAG) != 0);
        tag.putBoolean(LampBlock.TAG_BLOOM, (i & LampBlock.BLOOM_FLAG) != 0);
        tag.putBoolean(LampBlock.TAG_LIGHT, (i & LampBlock.LIGHT_FLAG) != 0);
        ItemStack stack = new ItemStack(this);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        return stack;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INVERTED, BLOOM, LIGHT, POWERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState originalState = super.getStateForPlacement(context);
        if (originalState == null) return null;
        return originalState.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIGHT) && isLightActive(state) ? 15 : 0;
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockAndLightGetter level, BlockPos pos, Direction side,
                                    @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        return state.getBlock().defaultBlockState();
    }

    public void update(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(POWERED) != level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                @Nullable Orientation orientation,
                                boolean movedByPiston) {
        if (!level.isClientSide()) {
            update(state, level, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        update(state, level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData,
                                       Player player) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData, player);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, getTagFromState(state));
        return stack;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> returnValue = super.getDrops(state, params);
        for (ItemStack stack : returnValue) {
            if (stack.is(this.asItem())) {
                net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, getTagFromState(state));
                break;
            }
        }
        return returnValue;
    }
}
