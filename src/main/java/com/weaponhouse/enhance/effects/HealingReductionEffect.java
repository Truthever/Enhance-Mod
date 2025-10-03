package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;
public class HealingReductionEffect extends Effect {
    public HealingReductionEffect() {
        super(EffectType.HARMFUL, 0xFF4500);
    }
    public static float getHealingMultiplier(int level) {
        int reduction = level * 5;
        float multiplier;
        if (reduction >= 100) {
            multiplier = -((reduction - 100) / 100.0f);
        } else {
            multiplier = 1 - (reduction / 100.0f);
        }
        return multiplier;
    }
    public static float handleHealing(LivingEntity entity, float healingAmount) {
        if (entity.isPotionActive(EffectRegistry.HEALING_REDUCTION)) {
            int level = entity.getActivePotionEffect(EffectRegistry.HEALING_REDUCTION).getAmplifier() + 1;
            float multiplier = getHealingMultiplier(level);
            if (multiplier < 0) {
                float healthReduction = healingAmount * Math.abs(multiplier);
                entity.attackEntityFrom(DamageSource.MAGIC, healthReduction);
                return 0;
            }
            return healingAmount * multiplier;
        }
        return healingAmount;
    }
}
