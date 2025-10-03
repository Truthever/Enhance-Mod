package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectType;
import java.util.UUID;
public class ReduceMaxHealthEffect extends BaseEffect {
    private static final UUID REDUCE_MAX_HEALTH_EFFECT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SET_HEALTH_MAX_MODIFIER_UUID = UUID.fromString("82322174-5616-7200-2618-900000000000");
    public ReduceMaxHealthEffect() {
        super(EffectType.HARMFUL, 0xFF0000);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double baseHealth = maxHealthAttr.getBaseValue();
            AttributeModifier setHealthMaxModifier = maxHealthAttr.getModifier(SET_HEALTH_MAX_MODIFIER_UUID);
            if (setHealthMaxModifier != null) {
                baseHealth += setHealthMaxModifier.getAmount();
            }
            AttributeModifier existingModifier = maxHealthAttr.getModifier(REDUCE_MAX_HEALTH_EFFECT_UUID);
            if (existingModifier != null) {
                maxHealthAttr.removeModifier(existingModifier);
            }
            double reductionPercentage = Math.min(1.0, amplifier * 0.05);
            double reductionAmount = baseHealth * reductionPercentage;
            AttributeModifier newModifier = new AttributeModifier(
                    REDUCE_MAX_HEALTH_EFFECT_UUID,
                    "reduce_max_health_potion_effect",
                    -reductionAmount,
                    AttributeModifier.Operation.ADDITION
            );
            maxHealthAttr.applyNonPersistentModifier(newModifier);
            double newMaxHealth = baseHealth - reductionAmount;
            if (entity.getHealth() > newMaxHealth) {
                entity.setHealth((float) newMaxHealth);
            }
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            AttributeModifier existingModifier = maxHealthAttr.getModifier(REDUCE_MAX_HEALTH_EFFECT_UUID);
            if (existingModifier != null) {
                maxHealthAttr.removeModifier(existingModifier);
            }
        }
    }
}
