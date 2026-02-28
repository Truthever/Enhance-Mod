package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.util.math.vector.Vector3d;

import java.util.UUID;
public class PlainsEffect extends BaseEffect {
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("1a8c8f6e-0b0a-4e8a-ba07-9b8c9d7e6f5c"); // 速度UUID
    private static final double MAX_SPEED_BOOST = 1.4;
    private static final double MAX_JUMP_BOOST = 1.2;
    private boolean wasOnGround = false;
    public PlainsEffect() {
        super(EffectType.BENEFICIAL, 0x8B4513);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        applySpeedEffect(entity, amplifier);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            AttributeModifier existingModifier = speedAttr.getModifier(SPEED_MODIFIER_UUID);
            if (existingModifier != null) {
                speedAttr.removeModifier(existingModifier);
            }
        }
    }
    private void applySpeedEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double boostPercentage = 0.05 * (amplifier + 1);
            boostPercentage = Math.min(boostPercentage, MAX_SPEED_BOOST);
            double baseSpeed = speedAttr.getBaseValue();
            double boostAmount = baseSpeed * boostPercentage;
            AttributeModifier existingModifier = speedAttr.getModifier(SPEED_MODIFIER_UUID);
            if (existingModifier != null) {
                speedAttr.removeModifier(existingModifier);
            }
            AttributeModifier newModifier = new AttributeModifier(
                    SPEED_MODIFIER_UUID,
                    "plains_speed_boost",
                    boostAmount,
                    AttributeModifier.Operation.ADDITION
            );
            speedAttr.applyNonPersistentModifier(newModifier);
        }
    }
    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (entity.isOnGround()) {
            wasOnGround = true;
        } else if (wasOnGround && !entity.isOnGround()) {
            Vector3d motion = entity.getMotion();
            if (motion.y > 0) {
                boolean isFlying = false;
                if (entity instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) entity;
                    isFlying = player.abilities.isFlying || player.isElytraFlying();
                }
                if (!isFlying) {
                    double jumpBoost = 1.0 + (0.05 * (amplifier + 1));
                    if (jumpBoost > MAX_JUMP_BOOST) {
                        jumpBoost = MAX_JUMP_BOOST;
                    }
                    entity.setMotion(motion.x, motion.y * jumpBoost, motion.z);
                }
            }
            wasOnGround = false;
        }
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }
}
