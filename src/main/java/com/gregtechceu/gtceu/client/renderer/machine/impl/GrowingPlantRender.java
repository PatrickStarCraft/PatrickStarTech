package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.mixins.GrowingPlantBlockAccessor;
import com.gregtechceu.gtceu.core.mixins.client.StemBlockAccessorMixin;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.memoization.GTMemoizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class GrowingPlantRender extends DynamicRender<IRecipeLogicMachine, GrowingPlantRender> {

    // spotless:off
    @SuppressWarnings("deprecation")
    public static final MapCodec<GrowingPlantRender> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.VECTOR3F.listOf().fieldOf("offsets").forGetter(GrowingPlantRender::getOffsets),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("growing_block").forGetter(GrowingPlantRender::getGrowingBlock),
            GrowthMode.CODEC.optionalFieldOf("growth_mode").forGetter(GrowingPlantRender::getGrowthMode)
    ).apply(instance, GrowingPlantRender::new));
    public static final DynamicRenderType<IRecipeLogicMachine, GrowingPlantRender> TYPE = new DynamicRenderType<>(GrowingPlantRender.CODEC);
    // spotless:on

    private static final float EPSILON = 1e-25f;

    @Getter
    private final List<Vector3fc> offsets;
    @Getter
    private final Optional<Block> growingBlock;
    @Getter
    private final Optional<GrowthMode> growthMode;

    public GrowingPlantRender(List<? extends Vector3fc> offsets) {
        this(offsets, Optional.empty(), Optional.empty());
    }

    public GrowingPlantRender(List<? extends Vector3fc> offsets, Optional<Block> growingBlock,
                              Optional<GrowthMode> growthMode) {
        this.offsets = List.copyOf(offsets);
        this.growingBlock = growingBlock;
        this.growthMode = growthMode;
    }

    @Override
    public DynamicRenderType<IRecipeLogicMachine, GrowingPlantRender> getType() {
        return TYPE;
    }

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public AABB getRenderBoundingBox(IRecipeLogicMachine machine) {
        final BlockPos pos = machine.self().getBlockPos();

        List<BlockPos> positions = new ArrayList<>();
        Collections.addAll(positions, pos.offset(-1, 0, -1), pos.offset(2, 2, 2));
        for (Vector3fc offset : this.offsets) {
            positions.add(BlockPos.containing(offset.x(), offset.y(), offset.z()));
        }

        return BoundingBox.encapsulatingPositions(positions).map(AABB::of)
                .orElseGet(() -> super.getRenderBoundingBox(machine));
    }

    @Override
    public DynamicRenderSnapshot extractRenderState(IRecipeLogicMachine recipeMachine, float partialTicks) {
        if (!ConfigHolder.INSTANCE.client.renderer.renderGrowingPlants || !recipeMachine.isActive()) {
            return new GrowingPlantSnapshot(List.of());
        }

        RecipeLogic recipeLogic = recipeMachine.getRecipeLogic();
        Optional<Block> growingBlock = this.growingBlock
                .or(() -> Optional.ofNullable(recipeLogic.getLastUnrolledRecipe()).flatMap(this::findGrowing));
        if (growingBlock.isEmpty()) return new GrowingPlantSnapshot(List.of());

        Block growing = growingBlock.get();
        GrowthMode mode = this.growthMode.orElseGet(() -> getGrowthModeForBlock(growing));
        if (this.growthMode.isPresent() && !mode.predicate().test(growing)) {
            if (mode == GrowthMode.GROWING_PLANT && GrowthMode.DOUBLE_TRANSLATE.predicate().test(growing)) {
                mode = GrowthMode.DOUBLE_TRANSLATE;
            }
            if (mode == GrowthMode.AGE_4 && GrowthMode.PICKLES.predicate().test(growing)) {
                mode = GrowthMode.PICKLES;
            } else if (mode == GrowthMode.AGE_4 && GrowthMode.FLOWER_AMOUNT.predicate().test(growing)) {
                mode = GrowthMode.FLOWER_AMOUNT;
            } else if (mode == GrowthMode.AGE_7 && GrowthMode.STEM.predicate().test(growing)) {
                mode = GrowthMode.STEM;
            } else {
                mode = GrowthMode.SCALE;
            }
        }
        if (mode == GrowthMode.NONE) return new GrowingPlantSnapshot(List.of());

        MetaMachine machine = recipeMachine.self();
        Level level = machine.getLevel();
        if (!(level instanceof BlockAndTintGetter tintGetter)) return new GrowingPlantSnapshot(List.of());

        double progress = recipeLogic.getProgressPercent();
        BlockPos machinePos = machine.getBlockPos();
        BlockState initialState = growing.defaultBlockState();
        Collection<StateWithOffset> states = mode.renderFunction().configureState(initialState, progress);
        List<PlantBlockRenderState> renderedPlants = new ArrayList<>();
        var modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();

        for (Vector3fc offset : this.offsets) {
            Vector3f rotated = new Vector3f(offset);
            rotated.rotateX(-Mth.HALF_PI);
            machine.getFrontFacing().getRotation().transform(rotated);
            BlockPos pos = machinePos.offset(BlockPos.containing(rotated.x(), rotated.y(), rotated.z()));

            for (StateWithOffset stateWithOffset : states) {
                BlockState blockState = stateWithOffset.state();
                if (blockState.getRenderShape() == RenderShape.INVISIBLE) continue;

                PoseStack localTransform = new PoseStack();
                localTransform.translate(rotated.x(), rotated.y() + EPSILON, rotated.z());
                Vector3fc translation = stateWithOffset.offset();
                localTransform.translate(translation.x(), translation.y(), translation.z());
                if (mode == GrowthMode.SCALE) {
                    localTransform.last().pose().scaleAround((float) progress, 0.5f, 0.0f, 0.5f);
                    localTransform.last().normal().scale((float) progress);
                } else if (mode == GrowthMode.GROWING_PLANT) {
                    Quaternionf growthRotation = ((GrowingPlantBlockAccessor) blockState.getBlock())
                            .gtceu$getGrowthDirection().getRotation();
                    localTransform.rotateAround(growthRotation, 0.5f, 0.5f, 0.5f);
                }

                BlockStateModel model = modelSet.get(blockState);
                BlockModelRenderState modelState = new BlockModelRenderState();
                List<BlockStateModelPart> parts = modelState.setupModel(
                        new Matrix4f(localTransform.last().pose()),
                        model.hasMaterialFlag(tintGetter, pos, blockState, BakedQuad.FLAG_TRANSLUCENT));
                model.collectParts(tintGetter, pos, blockState, RandomSource.create(blockState.getSeed(pos)), parts);

                List<BlockTintSource> tintSources = Minecraft.getInstance().getBlockColors().getTintSources(blockState);
                if (tintSources.isEmpty()) {
                    IClientBlockExtensions.of(blockState).collectDynamicTintValues(blockState, tintGetter, pos,
                            modelState.tintLayers());
                } else {
                    for (BlockTintSource tintSource : tintSources) {
                        modelState.tintLayers().add(tintSource.colorInWorld(blockState, tintGetter, pos));
                    }
                }

                modelState.blockLightCoords = blockState.emissiveRendering()
                        ? LightCoordsUtil.FULL_BRIGHT
                        : LightCoordsUtil.pack(blockState.getLightEmission(tintGetter, pos), 0);
                int packedLight = LightCoordsUtil.max(LightCoordsUtil.getLightCoords(tintGetter, pos),
                        modelState.blockLightCoords);
                renderedPlants.add(new PlantBlockRenderState(modelState, packedLight));
            }
        }
        return new GrowingPlantSnapshot(List.copyOf(renderedPlants));
    }

    @Override
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                                 CameraRenderState camera) {
        if (!(state instanceof GrowingPlantSnapshot snapshot)) return;
        for (PlantBlockRenderState plant : snapshot.plants()) {
            plant.modelState().submitMultiLayer(poseStack, collector, plant.packedLight(),
                    OverlayTexture.NO_OVERLAY, 0);
        }
    }

    private record PlantBlockRenderState(BlockModelRenderState modelState, int packedLight) {}

    private record GrowingPlantSnapshot(List<PlantBlockRenderState> plants) implements DynamicRenderSnapshot {
        private GrowingPlantSnapshot {
            plants = List.copyOf(plants);
        }
    }

    protected Optional<Block> findGrowing(GTRecipe recipe) {
        return RECIPE_BLOCK_CACHE.apply(recipe);
    }

    private static final Function<GTRecipe, Optional<Block>> RECIPE_BLOCK_CACHE = GTMemoizer
            .memoizeFunctionWeakIdent(recipe -> {
                List<Content> allItemContents = new ArrayList<>();
                allItemContents.addAll(recipe.getInputContents(ItemRecipeCapability.CAP));
                allItemContents.addAll(recipe.getTickInputContents(ItemRecipeCapability.CAP));
                allItemContents.addAll(recipe.getOutputContents(ItemRecipeCapability.CAP));
                allItemContents.addAll(recipe.getTickOutputContents(ItemRecipeCapability.CAP));
                return allItemContents.stream()
                        .map(Content::content).map(ItemRecipeCapability.CAP::of)
                        .map(com.gregtechceu.gtceu.api.recipe.ingredient.IngredientStacks::getItems)
                        .flatMap(Arrays::stream)
                        .map(ItemStack::getItem)
                        .filter(BlockItem.class::isInstance)
                        .findFirst()
                        .map(BlockItem.class::cast)
                        .map(BlockItem::getBlock);
            });

    protected GrowthMode getGrowthModeForBlock(Block block) {
        if (block instanceof GrowingPlantBlock) {
            return GrowthMode.GROWING_PLANT;
        }
        if (block instanceof StemBlock) {
            return GrowthMode.STEM;
        }

        BlockState state = block.defaultBlockState();
        IntegerProperty ageProp = findAgeProperty(state.getProperties());
        if (ageProp != null) {
            GrowthMode mode = GrowthMode.MODE_BY_PROPERTY.get(ageProp);
            if (mode != null) return mode;
        }

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) ||
                state.hasProperty(BlockStateProperties.HALF) ||
                state.is(CustomTags.TALL_PLANTS)) {
            return GrowthMode.DOUBLE_TRANSLATE;
        } else if (state.is(BlockTags.FLOWERS)) {
            return GrowthMode.TRANSLATE;
        }
        // default to SCALE
        return GrowthMode.SCALE;
    }

    public static @Nullable IntegerProperty findAgeProperty(Collection<Property<?>> properties) {
        for (Property<?> prop : properties) {
            if ((prop.getName().equals("age") || prop.getName().equals("pickles") ||
                    prop.getName().equals("flower_amount")) &&
                    prop instanceof IntegerProperty intProp) {
                return intProp;
            }
        }
        return null;
    }

    public record GrowthMode(String name,
                             Predicate<Block> predicate,
                             RenderFunction renderFunction) {

        public static final Map<String, GrowthMode> VALUES = new HashMap<>();
        public static final Map<IntegerProperty, GrowthMode> MODE_BY_PROPERTY = new HashMap<>();

        public static final GrowthMode NONE = new GrowthMode("none", RenderFunction.NO_OP);
        public static final GrowthMode SCALE = new GrowthMode("scale", RenderFunction.SCALE);
        public static final GrowthMode TRANSLATE = new GrowthMode("translate", RenderFunction.TRANSLATE);

        public static final GrowthMode DOUBLE_TRANSLATE = new GrowthMode("double_translate",
                RenderFunction.DOUBLE_BLOCK);
        public static final GrowthMode GROWING_PLANT = new GrowthMode("growing_plant",
                block -> block instanceof GrowingPlantBlock, RenderFunction.GROWING_PLANT);

        public static final GrowthMode STEM = new GrowthMode("stem", block -> block instanceof StemBlock,
                RenderFunction.STEM);

        // all the different age properties. not going to add extras, though.
        public static final GrowthMode AGE_1 = ofIntegerProperty("age_1", BlockStateProperties.AGE_1);
        public static final GrowthMode AGE_2 = ofIntegerProperty("age_2", BlockStateProperties.AGE_2);
        public static final GrowthMode AGE_3 = ofIntegerProperty("age_3", BlockStateProperties.AGE_3);
        public static final GrowthMode AGE_4 = ofIntegerProperty("age_4", BlockStateProperties.AGE_4);
        public static final GrowthMode AGE_5 = ofIntegerProperty("age_5", BlockStateProperties.AGE_5);
        public static final GrowthMode AGE_7 = ofIntegerProperty("age_7", BlockStateProperties.AGE_7);
        public static final GrowthMode AGE_15 = ofIntegerProperty("age_15", BlockStateProperties.AGE_15);
        public static final GrowthMode AGE_25 = ofIntegerProperty("age_25", BlockStateProperties.AGE_25);

        public static final GrowthMode PICKLES = ofIntegerProperty("pickles", BlockStateProperties.PICKLES, 0, 4);
        public static final GrowthMode FLOWER_AMOUNT = ofIntegerProperty("flower_amount",
                BlockStateProperties.FLOWER_AMOUNT, 0, 4);

        private static final Codec<GrowthMode> CODEC = Codec.STRING.comapFlatMap(name -> {
            GrowthMode mode = VALUES.get(name);
            if (mode != null) {
                return DataResult.success(mode);
            } else {
                // default to SCALE in case of an error
                return DataResult.error(() -> "Could not find growth mode named " + name, SCALE);
            }
        }, GrowthMode::name);

        public GrowthMode {
            VALUES.put(name, this);
        }

        public GrowthMode(String name, RenderFunction renderFunction) {
            this(name, block -> true, renderFunction);
        }

        public static GrowthMode ofIntegerProperty(String name, IntegerProperty property) {
            return ofIntegerProperty(name, property, OptionalInt.empty(), OptionalInt.empty());
        }

        public static GrowthMode ofIntegerProperty(String name, IntegerProperty property, int min, int max) {
            return ofIntegerProperty(name, property, OptionalInt.of(min), OptionalInt.of(max));
        }

        public static GrowthMode ofIntegerProperty(String name, IntegerProperty property, OptionalInt min,
                                                   OptionalInt max) {
            GrowthMode mode = new GrowthMode(name,
                    block -> block.getStateDefinition().getProperties().contains(property),
                    RenderFunction.byIntegerProperty(property, min, max));
            MODE_BY_PROPERTY.put(property, mode);
            return mode;
        }
    }

    @FunctionalInterface
    public interface RenderFunction {

        Collection<StateWithOffset> configureState(BlockState state, double progress);

        RenderFunction NO_OP = (state, progress) -> Collections.singleton(new StateWithOffset(state));

        RenderFunction SCALE = (state, progress) -> Collections.singleton(new StateWithOffset(state));

        RenderFunction TRANSLATE = (state, progress) -> {
            Vector3fc translation = new Vector3f(0, (float) (progress - 1), 0);
            return Collections.singleton(new StateWithOffset(state, translation));
        };

        RenderFunction DOUBLE_BLOCK = (state, progress) -> {
            Vector3fc translation = new Vector3f(0, (float) (progress * 2 - 1), 0);

            if (progress > 0.5) {
                Vector3fc bottomTranslation = new Vector3f(translation.x(), translation.y() - 1, translation.z());

                BlockState topState = state;
                if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                    topState = topState.trySetValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                } else if (state.hasProperty(BlockStateProperties.HALF)) {
                    topState = topState.trySetValue(BlockStateProperties.HALF, Half.TOP);
                }

                return Arrays.asList(new StateWithOffset(state, bottomTranslation),
                        new StateWithOffset(topState, translation));
            } else {
                if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                    state = state.trySetValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                } else if (state.hasProperty(BlockStateProperties.HALF)) {
                    state = state.trySetValue(BlockStateProperties.HALF, Half.TOP);
                }

                return Collections.singleton(new StateWithOffset(state, translation));
            }
        };

        RenderFunction GROWING_PLANT = new RenderFunction() {

            @Override
            public Collection<StateWithOffset> configureState(BlockState state, double progress) {
                GrowingPlantBlockAccessor accessor = (GrowingPlantBlockAccessor) state.getBlock();

                Vector3fc translation = new Vector3f(0, (float) (progress * 2 - 1), 0);

                if (progress < 0.5) {
                    BlockState headState = accessor.gtceu$getHeadBlock().defaultBlockState();
                    IntegerProperty ageProp = findAgeProperty(headState.getProperties());
                    if (ageProp != null) {
                        int minValue = ageProp.getPossibleValues().getFirst();
                        int maxValue = ageProp.getPossibleValues().getLast();

                        int stage = GTMath.lerpInt(progress, minValue, maxValue + 1);
                        headState = headState.trySetValue(ageProp, Math.min(stage, maxValue));
                    }

                    if (progress >= 0.25 && headState.hasProperty(BlockStateProperties.BERRIES)) {
                        headState = headState.trySetValue(CaveVines.BERRIES, true);
                    }

                    return Collections.singleton(new StateWithOffset(headState, translation));
                } else {
                    BlockState headState = accessor.gtceu$getHeadBlock().defaultBlockState();
                    IntegerProperty ageProp = findAgeProperty(headState.getProperties());
                    if (ageProp != null) {
                        headState = headState.trySetValue(ageProp, ageProp.getPossibleValues().getLast());
                    }
                    if (headState.hasProperty(BlockStateProperties.BERRIES)) {
                        headState = headState.trySetValue(CaveVines.BERRIES, true);
                    }

                    BlockState bodyState = accessor.gtceu$getBodyBlock().defaultBlockState();
                    if (progress >= 0.75 && bodyState.hasProperty(BlockStateProperties.BERRIES)) {
                        bodyState = bodyState.trySetValue(CaveVines.BERRIES, true);
                    }
                    Vector3fc bodyTranslation = new Vector3f(translation.x(), translation.y() - 1, translation.z());

                    return Arrays.asList(new StateWithOffset(bodyState, bodyTranslation),
                            new StateWithOffset(headState, translation));
                }
            }
        };

        RenderFunction STEM = (state, progress) -> {
            final StemBlock block = (StemBlock) state.getBlock();
            final int growthStage = GTMath.lerpInt(progress, 0, StemBlock.MAX_AGE + 2);
            if (growthStage > StemBlock.MAX_AGE) {
                var fruitKey = ((StemBlockAccessorMixin) block).gtceu$getFruit();
                Block fruit = Objects.requireNonNull(BuiltInRegistries.BLOCK.getValue(fruitKey),
                        () -> "Unregistered stem fruit " + fruitKey);
                return List.of(new StateWithOffset(fruit.defaultBlockState()));
            }
            state = state.trySetValue(StemBlock.AGE, growthStage);
            return List.of(new StateWithOffset(state));
        };

        TriFunction<IntegerProperty, OptionalInt, OptionalInt, RenderFunction> PROPERTY_FUNCTION_CACHE = GTMemoizer
                .memoize((property, setMin, setMax) -> {
                    final int presumedMinValue = property.getPossibleValues().getFirst();
                    final int presumedMaxValue = property.getPossibleValues().getLast();
                    return (state, progress) -> {
                        final int min = setMin.orElse(presumedMinValue);
                        final int betterMaxValue = state.getBlock() instanceof CropBlock crop ?
                                Math.max(presumedMaxValue, crop.getMaxAge()) : presumedMaxValue;
                        final int max = setMax.orElse(betterMaxValue);
                        int growthStage = GTMath.lerpInt(progress, min, max + 1);
                        if (growthStage < presumedMinValue) {
                            return Collections.emptySet();
                        }
                        if (state.getBlock() instanceof CropBlock crop) {
                            state = crop.getStateForAge(Math.min(growthStage, betterMaxValue));
                        } else {
                            state = state.trySetValue(property, Math.min(growthStage, betterMaxValue));
                        }
                        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                            final var topState = state.trySetValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                                    DoubleBlockHalf.UPPER);
                            return List.of(new StateWithOffset(state),
                                    new StateWithOffset(topState, new Vector3f(0, 1, 0)));
                        }
                        if (state.hasProperty(BlockStateProperties.HALF)) {
                            final var topState = state.trySetValue(BlockStateProperties.HALF, Half.TOP);
                            return List.of(new StateWithOffset(state),
                                    new StateWithOffset(topState, new Vector3f(0, 1, 0)));
                        }

                        return List.of(new StateWithOffset(state));
                    };
                });

        static RenderFunction byIntegerProperty(IntegerProperty property, OptionalInt min, OptionalInt max) {
            return PROPERTY_FUNCTION_CACHE.apply(property, min, max);
        }
    }

    public record StateWithOffset(BlockState state, Vector3fc offset) {

        private static final Vector3fc ZERO_VECTOR = new Vector3f();

        public StateWithOffset(BlockState state) {
            this(state, ZERO_VECTOR);
        }
    }
}
