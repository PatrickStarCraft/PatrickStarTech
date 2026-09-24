package com.gregtechceu.gtceu.integration.jade;

import net.minecraft.world.phys.Vec2;

import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

public class GTElementHelper {

    public static final Vec2 SMALL_FLUID_SIZE = new Vec2(10.0F, 10.0F);
    public static final Vec2 SMALL_FLUID_OFFSET = new Vec2(0.0F, -1.0F);

    public static Element smallFluid(JadeFluidObject fluid) {
        return JadeUI.fluid(fluid).size((int) SMALL_FLUID_SIZE.x, (int) SMALL_FLUID_SIZE.y)
                .offset((int) SMALL_FLUID_OFFSET.x, (int) SMALL_FLUID_OFFSET.y);
    }

}
