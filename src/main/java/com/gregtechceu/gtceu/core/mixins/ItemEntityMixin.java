package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.item.component.SpoilContext;
import com.gregtechceu.gtceu.api.item.component.SpoilUtils;

import net.minecraft.world.entity.item.ItemEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void gtceu$updateSpoilageInWorld(CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (!itemEntity.level().isClientSide()) {
            SpoilUtils.update(itemEntity.getItem(), new SpoilContext(itemEntity));
        }
    }
}
