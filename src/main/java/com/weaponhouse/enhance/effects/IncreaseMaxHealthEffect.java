package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectType;

import java.util.UUID;
public class IncreaseMaxHealthEffect extends BaseEffect {
    private static final UUID MAX_HEALTH_EFFECT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SET_HEALTH_MAX_MODIFIER_UUID = UUID.fromString("82322174-5616-7200-2618-900000000000");
    private static final String DYNAMIC_MAX_HEALTH_KEY = "DynamicMaxHealth";
    public IncreaseMaxHealthEffect() {
        super(EffectType.BENEFICIAL, 0x00FF00);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double baseHealth = maxHealthAttr.getBaseValue();
            CompoundNBT nbt = entity.getPersistentData();
            double dynamicMaxHealth = nbt.contains(DYNAMIC_MAX_HEALTH_KEY) ? nbt.getDouble(DYNAMIC_MAX_HEALTH_KEY) : baseHealth;
            AttributeModifier setHealthMaxModifier = maxHealthAttr.getModifier(SET_HEALTH_MAX_MODIFIER_UUID);
            if (setHealthMaxModifier != null) {
                baseHealth += setHealthMaxModifier.getAmount();
            }
            if (dynamicMaxHealth == baseHealth) {
                dynamicMaxHealth += baseHealth * (0.05 * amplifier);
            } else {
                dynamicMaxHealth += dynamicMaxHealth * (0.05 * amplifier);
            }
            nbt.putDouble(DYNAMIC_MAX_HEALTH_KEY, dynamicMaxHealth);
            AttributeModifier existingModifier = maxHealthAttr.getModifier(MAX_HEALTH_EFFECT_UUID);
            if (existingModifier != null) {
                maxHealthAttr.removeModifier(existingModifier);
            }
            AttributeModifier newModifier = new AttributeModifier(
                    MAX_HEALTH_EFFECT_UUID,
                    "increase_max_health_potion_effect",
                    dynamicMaxHealth - baseHealth,
                    AttributeModifier.Operation.ADDITION
            );
            maxHealthAttr.applyNonPersistentModifier(newModifier);
        }
    }

    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            AttributeModifier existingModifier = maxHealthAttr.getModifier(MAX_HEALTH_EFFECT_UUID);
            if (existingModifier != null) {
                maxHealthAttr.removeModifier(existingModifier);
            }
            CompoundNBT nbt = entity.getPersistentData();
            if (nbt.contains(DYNAMIC_MAX_HEALTH_KEY)) {
                double dynamicMaxHealth = nbt.getDouble(DYNAMIC_MAX_HEALTH_KEY);
                nbt.putDouble(DYNAMIC_MAX_HEALTH_KEY, maxHealthAttr.getBaseValue());
            }
        }
    }
}
