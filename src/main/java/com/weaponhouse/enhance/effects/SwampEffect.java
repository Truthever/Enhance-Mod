package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
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
}
