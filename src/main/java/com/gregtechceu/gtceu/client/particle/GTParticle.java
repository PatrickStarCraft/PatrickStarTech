package com.gregtechceu.gtceu.client.particle;

import com.gregtechceu.gtceu.client.bloom.EffectRenderContext;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * A custom particle implementation with framework for more advanced rendering capabilities.
 * <p/>
 * GTParticle instances are managed by {@link GTParticleManager}. Particles with the same target render type are
 * submitted together through the level's custom-geometry collector.
 */
public abstract class GTParticle {

    public double posX;
    public double posY;
    public double posZ;

    /**
     * render range. If the distance between particle and render view entity exceeds this value, the particle
     * will not be rendered. If render range is negative value or {@code NaN}, then the check is disabled and
     * the
     * particle will be rendered regardless of the distance.
     */
    @Getter
    private double renderRange = -1;
    /**
     * squared render range, or negative value if render distance check is disabled.
     */
    @Getter
    private double squaredRenderRange = -1;

    @Getter
    private boolean expired;

    protected GTParticle(double posX, double posY, double posZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public boolean shouldRender(EffectRenderContext context) {
        if (squaredRenderRange < 0) return true;
        return context.getRenderViewEntity().getEyePosition(context.partialTicks())
                .distanceToSqr(posX, posY, posZ) <= squaredRenderRange;
    }

    public final boolean isAlive() {
        return !expired;
    }

    public final void setExpired() {
        if (this.expired) return;
        this.expired = true;
        onExpired();
    }

    /**
     * @return {@code true} to render the particle with
     *         {@link com.mojang.blaze3d.systems.RenderSystem#depthMask(boolean) depth mask} feature disabled; in
     *         other words, render the particle without modifying depth buffer.
     */
    public boolean shouldDisableDepth() {
        return false;
    }

    /**
     * Sets the render range. If the distance between particle and render view entity exceeds this value, the particle
     * will not be rendered. If render range is negative value or {@code NaN}, then the check is disabled and the
     * particle will be rendered regardless of the distance.
     *
     * @param renderRange Render range
     */
    public final void setRenderRange(double renderRange) {
        this.renderRange = renderRange;
        if (renderRange >= 0) this.squaredRenderRange = renderRange * renderRange;
        else this.squaredRenderRange = -1;
    }

    /**
     * Update the particle. This method is called each tick.
     */
    public void onUpdate() {}

    /**
     * Called once on expiration.
     */
    protected void onExpired() {}

    /** Returns the pipeline for this particle's depth-write mode, or {@code null} when it has no geometry. */
    public @Nullable RenderType getRenderType(boolean writeDepth) {
        return null;
    }

    /** Submit the particle through the target level-render collector. */
    @OnlyIn(Dist.CLIENT)
    public void renderParticle(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
                               EffectRenderContext context) {}
}
