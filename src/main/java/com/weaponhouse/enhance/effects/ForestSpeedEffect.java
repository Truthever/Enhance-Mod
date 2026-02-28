package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectType;

import java.util.UUID;
public class ForestSpeedEffect extends BaseEffect {
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("1a8c8f6e-0b0a-4e8a-ba07-9b8c9d7e6f5a");
    public ForestSpeedEffect() {
        super(EffectType.BENEFICIAL, 0x4CAF50);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double baseSpeed = speedAttr.getBaseValue();
            double boostPercentage = 0.05 * (amplifier + 1);
            double boostAmount = baseSpeed * boostPercentage;
            applyAttributeModifier(speedAttr, boostAmount);
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            removeAttributeModifier(speedAttr);
        }
    }
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, double amount) {
        AttributeModifier existingModifier = attribute.getModifier(ForestSpeedEffect.SPEED_MODIFIER_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
        AttributeModifier newModifier = new AttributeModifier(ForestSpeedEffect.SPEED_MODIFIER_UUID, "forest_speed_boost", amount, AttributeModifier.Operation.ADDITION);
        attribute.applyNonPersistentModifier(newModifier);
    }
    private void removeAttributeModifier(ModifiableAttributeInstance attribute) {
        AttributeModifier existingModifier = attribute.getModifier(ForestSpeedEffect.SPEED_MODIFIER_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
    }
}