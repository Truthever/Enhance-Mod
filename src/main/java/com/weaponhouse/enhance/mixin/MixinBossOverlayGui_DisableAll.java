package com.weaponhouse.enhance.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.weaponhouse.enhance.client.ClientConfigCache;
import net.minecraft.client.gui.overlay.BossOverlayGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossOverlayGui.class)
public abstract class MixinBossOverlayGui_DisableAll {
    @Inject(method = "func_238484_a_", at = @At("HEAD"), cancellable = true)
    private void enhance$disableVanillaBossBars(MatrixStack poseStack, CallbackInfo ci) {
        if (ClientConfigCache.isOriginalBossBarDisabled()) {
            ci.cancel();
        }
    }
}
