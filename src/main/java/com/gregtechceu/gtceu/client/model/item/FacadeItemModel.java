package com.gregtechceu.gtceu.client.model.item;

import com.gregtechceu.gtceu.client.model.quad.MeshBuilder;
import com.gregtechceu.gtceu.client.model.quad.MutableQuadView;
import com.gregtechceu.gtceu.client.model.quad.StaticFaceBakery;
import com.gregtechceu.gtceu.client.util.quad.transformers.QuadPositionForcer;
import com.gregtechceu.gtceu.client.util.quad.transformers.QuadReInterpolator;
import com.gregtechceu.gtceu.client.util.quad.transformers.QuadTinter;
import com.gregtechceu.gtceu.common.item.behavior.FacadeItemBehaviour;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Dynamic facade item model; all collected geometry belongs to this model-bake generation. */
public final class FacadeItemModel implements ItemModel {

    private static final Direction ITEM_FACADE_SIDE = Direction.NORTH;
    private static final double FACADE_PLANE_BACK = 1.0 / 16;

    private final QuadCollection baseQuads;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final List<ItemTintSource> baseTints;
    private final ConcurrentMap<BlockState, List<BakedQuad>> facadeQuads = new ConcurrentHashMap<>();

    private FacadeItemModel(QuadCollection baseQuads, ModelRenderProperties properties,
                            Matrix4fc transformation, List<ItemTintSource> baseTints) {
        this.baseQuads = baseQuads;
        this.properties = properties;
        this.transformation = transformation;
        this.baseTints = List.copyOf(baseTints);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            output.setAnimated();
        }
        if (!this.baseTints.isEmpty()) {
            var tintLayers = layer.tintLayers();
            for (ItemTintSource tintSource : this.baseTints) {
                tintLayers.add(tintSource.calculate(item, level, owner == null ? null : owner.asLivingEntity()));
            }
        }

        List<BakedQuad> quads = layer.prepareQuadList();
        quads.addAll(this.baseQuads.getAll());
        BlockState facadeState = FacadeItemBehaviour.getFacadeStateNullable(item);
        if (facadeState != null && facadeState.getRenderShape() == RenderShape.MODEL) {
            quads.addAll(this.facadeQuads.computeIfAbsent(facadeState, FacadeItemModel::bakeFacadeQuads));
        }

        List<BakedQuad> extentQuads = List.copyOf(quads);
        layer.setExtents(() -> CuboidItemModelWrapper.computeExtents(extentQuads));
        layer.setLocalTransform(this.transformation);
        this.properties.applyToLayer(layer, displayContext);
        if (extentQuads.stream().anyMatch(quad -> (quad.materialInfo().flags() & BakedQuad.FLAG_ANIMATED) != 0)) {
            output.setAnimated();
        }
    }

    private static List<BakedQuad> bakeFacadeQuads(BlockState facadeState) {
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        BlockStateModel facadeModel = modelSet.get(facadeState);
        List<BlockStateModelPart> parts = new ArrayList<>();
        facadeModel.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, facadeState,
                RandomSource.create(facadeState.hashCode()), parts);

        QuadPositionForcer clamper = new QuadPositionForcer(
                StaticFaceBakery.COVER_OVERLAY.setMaxZ(FACADE_PLANE_BACK));
        List<BakedQuad> result = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> candidates = new ArrayList<>(part.getQuads(null));
            candidates.addAll(part.getQuads(ITEM_FACADE_SIDE));
            for (BakedQuad quad : candidates) {
                if (quad.direction() != ITEM_FACADE_SIDE) continue;

                MutableQuadView emitter = MeshBuilder.getInstance().getEmitter();
                emitter.fromVanilla(quad, null);
                QuadReInterpolator interpolator = new QuadReInterpolator();
                interpolator.setInputQuad(emitter);

                int tintIndex = emitter.tintIndex();
                if (tintIndex != -1) {
                    BlockTintSource tintSource = Minecraft.getInstance().getBlockColors()
                            .getTintSource(facadeState, tintIndex);
                    new QuadTinter(tintSource == null ? -1 : tintSource.color(facadeState)).transform(emitter);
                }

                clamper.transform(emitter);
                interpolator.transform(emitter);
                result.add(emitter.toBlockBakedQuad());
                emitter.emit();
            }
        }
        return List.copyOf(result);
    }

    public record Unbaked(Identifier base, Optional<Transformation> transformation,
                          List<ItemTintSource> tints) implements ItemModel.Unbaked {
        public static final MapCodec<FacadeItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
                Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation),
                ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(Unbaked::tints))
                .apply(instance, Unbaked::new));

        public Unbaked {
            tints = List.copyOf(tints);
        }

        @Override
        public MapCodec<FacadeItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolved = baker.getModel(this.base);
            TextureSlots textureSlots = resolved.getTopTextureSlots();
            QuadCollection quads = resolved.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, textureSlots);
            Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
            return new FacadeItemModel(quads, properties, modelTransform, this.tints);
        }
    }
}
