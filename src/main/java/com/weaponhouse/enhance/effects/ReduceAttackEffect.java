package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.util.text.StringTextComponent;
public class ReduceAttackEffect extends BaseEffect {
    public ReduceAttackEffect() {
        super(EffectType.HARMFUL, 0xAA0000);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        float baseAttack = entity.getPersistentData().getFloat("attack");
        float reductionAmount = baseAttack * (0.05f * amplifier);
        float newAttack = Math.max(0, baseAttack - reductionAmount);
        entity.getPersistentData().putFloat("attackEffect", reductionAmount);
        entity.getPersistentData().putFloat("attack", newAttack);
        entity.setCustomName(new StringTextComponent("攻击力: " + newAttack));
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        float baseAttack = entity.getPersistentData().getFloat("attack");
        float reductionAmount = entity.getPersistentData().getFloat("attackEffect");
        entity.getPersistentData().putFloat("attack", baseAttack + reductionAmount);
        entity.getPersistentData().remove("attackEffect");
        entity.setCustomName(new StringTextComponent("攻击力: " + (baseAttack + reductionAmount)));
    }
}
