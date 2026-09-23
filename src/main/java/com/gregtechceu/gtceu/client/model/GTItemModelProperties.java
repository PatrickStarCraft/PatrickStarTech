package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.component.ElectricStats;
import com.gregtechceu.gtceu.api.item.data.ItemStackData;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.common.item.behavior.LighterBehavior;
import com.gregtechceu.gtceu.common.item.behavior.NanoSaberBehavior;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.ToDoubleFunction;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class GTItemModelProperties {

    private static final StackProperty BATTERY = new StackProperty(ElectricStats::getStoredPredicate);
    private static final StackProperty JETPACK = new StackProperty(ElectricStats::getStoredPredicate);
    private static final StackProperty CIRCUIT = new StackProperty(
            stack -> IntCircuitBehaviour.getCircuitConfiguration(stack) / 100.0);
    private static final StackProperty LIGHTER_OPEN = new StackProperty(
            stack -> ItemStackData.read(stack).getBooleanOr(LighterBehavior.LIGHTER_OPEN, false) ? 1.0 : 0.0);
    private static final StackProperty NANO_SABER_ACTIVE = new StackProperty(
            stack -> NanoSaberBehavior.isItemActive(stack) ? 1.0 : 0.0);

    private GTItemModelProperties() {}

    @SubscribeEvent
    public static void register(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(GTCEu.id("battery"), BATTERY.type());
        event.register(GTCEu.id("circuit"), CIRCUIT.type());
        event.register(GTCEu.id("lighter_open"), LIGHTER_OPEN.type());
        event.register(NanoSaberBehavior.OVERRIDE_KEY_LOCATION, NANO_SABER_ACTIVE.type());
        event.register(GTCEu.id("electric_jetpack"), JETPACK.type());
    }

    private static final class StackProperty implements RangeSelectItemModelProperty {

        private final ToDoubleFunction<ItemStack> value;
        private final MapCodec<StackProperty> codec;

        private StackProperty(ToDoubleFunction<ItemStack> value) {
            this.value = value;
            this.codec = MapCodec.unit(this);
        }

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            return (float) value.applyAsDouble(stack);
        }

        @Override
        public MapCodec<StackProperty> type() {
            return codec;
        }
    }
}
