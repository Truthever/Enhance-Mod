package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
public class SwampEffect extends BaseEffect {
    private static final int EFFECT_COLOR = 0x2D5D28;
    public SwampEffect() {
        super(EffectType.BENEFICIAL, EFFECT_COLOR);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
    }
    @Override
    public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {}
    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }
    @Override
    public boolean shouldRender(EffectInstance effect) {
        return true;
    }
    @Override
    public boolean shouldRenderHUD(EffectInstance effect) {
        return true;
    }


}
