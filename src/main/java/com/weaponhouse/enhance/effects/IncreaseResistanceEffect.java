package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectType;
public class IncreaseResistanceEffect extends BaseEffect {
    public IncreaseResistanceEffect() {
        super(EffectType.BENEFICIAL, 0x00FF00);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        float naturalArmor = entity.getPersistentData().getFloat("naturalArmor");
        float increaseAmount = naturalArmor * (0.05f * amplifier);
        entity.getPersistentData().putFloat("naturalArmorEffect", increaseAmount);
        entity.getPersistentData().putFloat("naturalArmor", naturalArmor + increaseAmount);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        float naturalArmor = entity.getPersistentData().getFloat("naturalArmor");
        float increaseAmount = entity.getPersistentData().getFloat("naturalArmorEffect");
        entity.getPersistentData().putFloat("naturalArmor", naturalArmor - increaseAmount);
        entity.getPersistentData().remove("naturalArmorEffect");
    }
}
