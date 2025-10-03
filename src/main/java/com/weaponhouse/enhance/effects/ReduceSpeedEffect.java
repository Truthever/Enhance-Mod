package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.util.text.StringTextComponent;
public class ReduceSpeedEffect extends BaseEffect {
    public ReduceSpeedEffect() {
        super(EffectType.HARMFUL, 0xFF0000);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        if (!entity.getPersistentData().contains("speed")) {
            float currentSpeed = (float) entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
            entity.getPersistentData().putFloat("speed", currentSpeed);
        }
        float baseSpeed = entity.getPersistentData().getFloat("speed");
        float reduceAmount = baseSpeed * (0.05f * amplifier);
        entity.getPersistentData().putFloat("speedEffect", reduceAmount);
        float newSpeed = baseSpeed - reduceAmount;
        entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
    }

    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        if (!entity.getPersistentData().contains("speed")) return;
        float baseSpeed = entity.getPersistentData().getFloat("speed");
        float reduceAmount = entity.getPersistentData().getFloat("speedEffect");
        float restoredSpeed = baseSpeed;
        entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(restoredSpeed);
        entity.getPersistentData().remove("speedEffect");
    }
}
