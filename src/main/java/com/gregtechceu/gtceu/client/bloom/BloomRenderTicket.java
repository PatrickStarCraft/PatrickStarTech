package com.gregtechceu.gtceu.client.bloom;

import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.rendertype.RenderType;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BloomRenderTicket {

    public static final BloomRenderTicket INVALID = new BloomRenderTicket();

    final RenderType renderType;
    final IBloomEffect render;
    final @Nullable Predicate<BloomRenderTicket> validityChecker;
    final @Nullable Supplier<@Nullable Level> worldContext;

    private boolean invalidated;

    private BloomRenderTicket() {
        this(GTRenderTypes.bloom(), (p, c, r, context) -> {}, null, null);
        this.invalidated = true;
    }

    BloomRenderTicket(RenderType renderType, IBloomEffect render,
                      @Nullable Predicate<BloomRenderTicket> validityChecker,
                      @Nullable Supplier<@Nullable Level> worldContext) {
        this.renderType = Objects.requireNonNull(renderType, "renderType == null");
        this.render = Objects.requireNonNull(render, "render == null");
        this.validityChecker = validityChecker;
        this.worldContext = worldContext;
    }

    public boolean isValid() {
        return !this.invalidated;
    }

    public void invalidate() {
        this.invalidated = true;
    }

    void checkValidity() {
        if (!this.invalidated && this.validityChecker != null && !this.validityChecker.test(this)) {
            invalidate();
        }
    }
}
