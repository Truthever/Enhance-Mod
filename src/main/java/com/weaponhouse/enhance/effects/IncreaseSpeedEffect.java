package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.util.text.StringTextComponent;
public class IncreaseSpeedEffect extends BaseEffect {
    public IncreaseSpeedEffect() {
        super(EffectType.BENEFICIAL, 0x00FF00);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        if (!entity.getPersistentData().contains("speed")) {
            float currentSpeed = (float) entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
            entity.getPersistentData().putFloat("speed", currentSpeed);
        }
        float baseSpeed = entity.getPersistentData().getFloat("speed");
        float increaseAmount = baseSpeed * (0.05f * (amplifier));
        entity.getPersistentData().putFloat("speedEffect", increaseAmount);
        float newSpeed = baseSpeed + increaseAmount;
        entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        if (!entity.getPersistentData().contains("speed")) return;
        float baseSpeed = entity.getPersistentData().getFloat("speed");
        float increaseAmount = entity.getPersistentData().getFloat("speedEffect");
        float restoredSpeed = baseSpeed;
        entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(restoredSpeed);
        entity.getPersistentData().remove("speedEffect");
    }
}
