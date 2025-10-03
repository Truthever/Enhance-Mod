package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.nbt.CompoundNBT;
import java.util.UUID;
public class MushroomHealthEffect extends BaseEffect {
    private static final UUID MAX_HEALTH_MODIFIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final double HEALTH_PER_LEVEL = 25.0;
    public MushroomHealthEffect() {
        super(EffectType.BENEFICIAL, 0xFFFFFF);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;
        CompoundNBT nbt = entity.getPersistentData();
        if (!nbt.contains("BaseMaxHealth")) {
            nbt.putDouble("BaseMaxHealth", maxHealthAttr.getBaseValue());
        }
        if (!nbt.contains("OriginalMaxHealth")) {
            nbt.putDouble("OriginalMaxHealth", maxHealthAttr.getBaseValue());
        }
        int effectLevel = amplifier + 1;
        double totalIncrease = effectLevel * HEALTH_PER_LEVEL;
        double baseHealth = nbt.getDouble("BaseMaxHealth");
        double adjustment = (baseHealth + totalIncrease) - maxHealthAttr.getBaseValue();
        applyAttributeModifier(maxHealthAttr, MAX_HEALTH_MODIFIER_UUID, adjustment, AttributeModifier.Operation.ADDITION);
        float newHealth = (float) Math.min(entity.getHealth() + adjustment, maxHealthAttr.getBaseValue() + adjustment);
        entity.setHealth(newHealth);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;
        CompoundNBT nbt = entity.getPersistentData();
        removeAttributeModifier(maxHealthAttr, MAX_HEALTH_MODIFIER_UUID);
        if (nbt.contains("OriginalMaxHealth")) {
            double originalHealth = nbt.getDouble("OriginalMaxHealth");
            if (entity.getHealth() > originalHealth) {
                entity.setHealth((float) originalHealth);
            }
            nbt.remove("OriginalMaxHealth");
        }
    }
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, UUID uuid, double amount, AttributeModifier.Operation operation) {
        AttributeModifier existingModifier = attribute.getModifier(uuid);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
        AttributeModifier newModifier = new AttributeModifier(uuid, "mushroom_health_boost", amount, operation);
        attribute.applyNonPersistentModifier(newModifier);
    }
    private void removeAttributeModifier(ModifiableAttributeInstance attribute, UUID uuid) {
        AttributeModifier existingModifier = attribute.getModifier(uuid);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }
    @Override
    public boolean shouldRender(EffectInstance effect) {
        return true;
    }
    @Override
    public boolean shouldRenderHUD(EffectInstance effect) {
        return true;
    }
}
