package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;

import java.util.UUID;
public class FrostEffect extends BaseEffect {
    private static final UUID FROST_SPEED_UUID = UUID.fromString("a3b2c1d0-e4f5-6789-0abc-def123456789");
    private static final String FROST_MODIFIER_NAME = "Frost Slow";
    public FrostEffect() {
        super(EffectType.HARMFUL, 0x88CCEE);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double slowPercentage = -0.15 * (amplifier + 1);
            applyAttributeModifier(speedAttr, slowPercentage);
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            removeAttributeModifier(speedAttr);
        }
    }
    @Override
    public boolean shouldRender(EffectInstance effect) {
        return effect != null && effect.getDuration() > 0;
    }
    @Override
    public boolean shouldRenderHUD(EffectInstance effect) {
        return effect != null && effect.getDuration() > 0;
    }
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, double amount) {
        AttributeModifier existingModifier = attribute.getModifier(FrostEffect.FROST_SPEED_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
        AttributeModifier newModifier = new AttributeModifier(
                FrostEffect.FROST_SPEED_UUID,
                FROST_MODIFIER_NAME,
                amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        attribute.applyNonPersistentModifier(newModifier);
    }
    private void removeAttributeModifier(ModifiableAttributeInstance attribute) {
        AttributeModifier existingModifier = attribute.getModifier(FrostEffect.FROST_SPEED_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
    }
}
