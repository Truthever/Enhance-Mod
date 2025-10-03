package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectType;
public class ReduceResistanceEffect extends BaseEffect {
    public ReduceResistanceEffect() {
        super(EffectType.HARMFUL, 0xFF0000);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        float naturalArmor = entity.getPersistentData().getFloat("naturalArmor");
        float reductionAmount = naturalArmor * (0.05f * amplifier);
        float newArmor = Math.max(0, naturalArmor - reductionAmount);
        entity.getPersistentData().putFloat("naturalArmorEffect", reductionAmount);
        entity.getPersistentData().putFloat("naturalArmor", newArmor);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        float naturalArmor = entity.getPersistentData().getFloat("naturalArmor");
        float reductionAmount = entity.getPersistentData().getFloat("naturalArmorEffect");
        entity.getPersistentData().putFloat("naturalArmor", naturalArmor + reductionAmount);
        entity.getPersistentData().remove("naturalArmorEffect");
    }
}
