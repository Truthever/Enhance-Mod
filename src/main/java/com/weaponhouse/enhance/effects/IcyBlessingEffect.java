package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectType;
public class IcyBlessingEffect extends BaseEffect {
    private static final int EFFECT_COLOR = 0x87CEEB;
    public IcyBlessingEffect() {
        super(EffectType.BENEFICIAL, EFFECT_COLOR);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
    }
}
