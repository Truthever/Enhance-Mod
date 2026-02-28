package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectType;
import java.util.UUID;
public class CorrosionEffect extends BaseEffect {
    private static final UUID CORROSION_MAX_HEALTH_MODIFIER = UUID.randomUUID();
    public CorrosionEffect() {
        super(EffectType.HARMFUL, 0x4A4A4A);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double reductionPercent = Math.min((amplifier + 1) * 0.05, 0.5);
            AttributeModifier modifier = new AttributeModifier(
                    CORROSION_MAX_HEALTH_MODIFIER,
                    "Corrosion health reduction",
                    -reductionPercent,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            maxHealthAttr.removeModifier(CORROSION_MAX_HEALTH_MODIFIER);
            maxHealthAttr.applyPersistentModifier(modifier);
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.removeModifier(CORROSION_MAX_HEALTH_MODIFIER);
        }
    }
}