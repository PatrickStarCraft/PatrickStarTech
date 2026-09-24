package com.gregtechceu.gtceu.client.renderer.entity;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.entity.GTBoat;
import com.gregtechceu.gtceu.common.entity.GTChestBoat;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionf;

import java.util.EnumMap;
import java.util.Map;

public class GTBoatRenderer extends AbstractBoatRenderer {

    private final Map<GTBoat.BoatType, BoatVariant> boats = new EnumMap<>(GTBoat.BoatType.class);
    private final Model.Simple waterPatchModel;

    public GTBoatRenderer(EntityRendererProvider.Context context, boolean chestBoat) {
        super(context, GTCEu.id(getTextureLocation(GTBoat.BoatType.RUBBER, chestBoat)));
        this.waterPatchModel = new Model.Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), t -> RenderTypes.waterMask());
        for (GTBoat.BoatType type : GTBoat.BoatType.values()) {
            ModelLayerLocation modelLocation = chestBoat ? getChestBoatModelName(type) : getBoatModelName(type);
            ModelPart root = context.bakeLayer(modelLocation);
            boats.put(type, new BoatVariant(GTCEu.id(getTextureLocation(type, chestBoat)), new BoatModel(root)));
        }
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return boats.get(GTBoat.BoatType.RUBBER).model();
    }

    private static String getTextureLocation(GTBoat.BoatType type, boolean chest) {
        return chest ? "textures/entity/boat/" + type.getName() + "_chest_boat.png" :
                "textures/entity/boat/" + type.getName() + "_boat.png";
    }

    @Override
    public GTBoatRenderState createRenderState() {
        return new GTBoatRenderState();
    }

    @Override
    public void extractRenderState(AbstractBoat boat, BoatRenderState renderState, float partialTicks) {
        super.extractRenderState(boat, renderState, partialTicks);
        GTBoat.BoatType type = boat instanceof GTChestBoat chest ? chest.getBoatType() : ((GTBoat) boat).getBoatType();
        BoatVariant variant = boats.get(type);
        GTBoatRenderState state = (GTBoatRenderState) renderState;
        state.model = variant.model();
        state.texture = variant.texture();
    }

    @Override
    public void submit(BoatRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        GTBoatRenderState state = (GTBoatRenderState) renderState;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir));
        }
        if (!state.isUnderWater && !Mth.equal(state.bubbleAngle, 0.0F)) {
            poseStack.mulPose(new Quaternionf().setAngleAxis(state.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        collector.submitModel(state.model, state, poseStack, state.texture, state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (!state.isUnderWater) {
            collector.submitModel(waterPatchModel, Unit.INSTANCE, poseStack, state.texture, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static ModelLayerLocation getChestBoatModelName(GTBoat.BoatType type) {
        return new ModelLayerLocation(GTCEu.id("chest_boat/" + type.getName()), "main");
    }

    public static ModelLayerLocation getBoatModelName(GTBoat.BoatType type) {
        return new ModelLayerLocation(GTCEu.id("boat/" + type.getName()), "main");
    }

    private record BoatVariant(Identifier texture, EntityModel<BoatRenderState> model) {}

    public static final class GTBoatRenderState extends BoatRenderState {
        private EntityModel<BoatRenderState> model;
        private Identifier texture;
    }
}
