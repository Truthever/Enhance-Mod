package com.weaponhouse.enhance.mixin;

import com.weaponhouse.enhance.client.ClientConfigCache;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RangedAttribute.class)
public abstract class MixinRangedAttribute_MaxHealthCap {
    @Shadow @Final @Mutable
    private double maximumValue;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enhance$setMaxHealthCap(String translationKey, double defaultValue, double min, double max, CallbackInfo ci) {
        if ("attribute.name.generic.max_health".equals(translationKey)) {
            this.maximumValue = ClientConfigCache.getMaxHealthCap();
        }
    }
}
