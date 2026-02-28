package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectType;

import java.util.UUID;

public class NetherEffect extends BaseEffect {
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final UUID DAMAGE_MULTIPLIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final String NATURAL_ARMOR_TAG = "naturalArmor";
    private static final String DYNAMIC_ARMOR_TAG = "dynamicArmor";
    private static final String ORIGINAL_DYNAMIC_ARMOR_TAG = "originalDynamicArmor";
    private static final String NATURAL_ATTACK_TAG = "naturalAttack";
    private static final String DYNAMIC_ATTACK_TAG = "dynamicAttack";
    private static final String ORIGINAL_DYNAMIC_ATTACK_TAG = "originalDynamicAttack";
    public NetherEffect() {
        super(EffectType.BENEFICIAL, 0xFF4500);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        applyDefenseEffect(entity, amplifier);
        applyAttackEffect(entity, amplifier);
        applyDamageMultiplierEffect(entity, amplifier);
        handleFireImmunity(entity, amplifier);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        removeDefenseEffect(entity);
        removeAttackEffect(entity);
        removeDamageMultiplierEffect(entity);
    }
    private void applyDefenseEffect(LivingEntity entity, int amplifier) {
        CompoundNBT nbt = entity.getPersistentData();
        if (!nbt.contains(NATURAL_ARMOR_TAG)) {
            nbt.putDouble(NATURAL_ARMOR_TAG, 0.0D);
        }
        if (!nbt.contains(DYNAMIC_ARMOR_TAG)) {
            nbt.putDouble(DYNAMIC_ARMOR_TAG, nbt.getDouble(NATURAL_ARMOR_TAG));
        }
        if (!nbt.contains(ORIGINAL_DYNAMIC_ARMOR_TAG)) {
            nbt.putDouble(ORIGINAL_DYNAMIC_ARMOR_TAG, nbt.getDouble(DYNAMIC_ARMOR_TAG));
        }
        double currentDynamicArmor = nbt.getDouble(DYNAMIC_ARMOR_TAG);
        double adjustment = amplifier + 0.5D;
        double newDynamicArmor = currentDynamicArmor + adjustment;
        nbt.putDouble(DYNAMIC_ARMOR_TAG, newDynamicArmor);
    }
    private void applyAttackEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            CompoundNBT nbt = entity.getPersistentData();
            if (!nbt.contains(NATURAL_ATTACK_TAG)) {
                nbt.putDouble(NATURAL_ATTACK_TAG, attackAttr.getBaseValue());
            }
            if (!nbt.contains(DYNAMIC_ATTACK_TAG)) {
                nbt.putDouble(DYNAMIC_ATTACK_TAG, attackAttr.getBaseValue());
            }
            if (!nbt.contains(ORIGINAL_DYNAMIC_ATTACK_TAG)) {
                nbt.putDouble(ORIGINAL_DYNAMIC_ATTACK_TAG, nbt.getDouble(DYNAMIC_ATTACK_TAG));
            }
            double currentDynamicAttack = nbt.getDouble(DYNAMIC_ATTACK_TAG);
            double adjustment = (amplifier + 1) * 1.0D;
            double newDynamicAttack = currentDynamicAttack + adjustment;
            applyAttributeModifier(attackAttr, ATTACK_MODIFIER_UUID, adjustment, AttributeModifier.Operation.ADDITION);
            nbt.putDouble(DYNAMIC_ATTACK_TAG, newDynamicAttack);
        }
    }
    private void applyDamageMultiplierEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double multiplier = (amplifier + 1) * 0.05D;
            applyAttributeModifier(attackAttr, DAMAGE_MULTIPLIER_UUID, multiplier, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }
    private void removeDefenseEffect(LivingEntity entity) {
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains(ORIGINAL_DYNAMIC_ARMOR_TAG)) {
            nbt.putDouble(DYNAMIC_ARMOR_TAG, nbt.getDouble(ORIGINAL_DYNAMIC_ARMOR_TAG));
            nbt.remove(ORIGINAL_DYNAMIC_ARMOR_TAG);
        } else if (nbt.contains(DYNAMIC_ARMOR_TAG)) {
            nbt.remove(DYNAMIC_ARMOR_TAG);
        }
        if (entity.getActivePotionEffect(this) == null) {
            nbt.remove(NATURAL_ARMOR_TAG);
        }
    }
    private void removeAttackEffect(LivingEntity entity) {
        ModifiableAttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            removeAttributeModifier(attackAttr, ATTACK_MODIFIER_UUID);
            CompoundNBT nbt = entity.getPersistentData();
            if (nbt.contains(ORIGINAL_DYNAMIC_ATTACK_TAG)) {
                nbt.putDouble(DYNAMIC_ATTACK_TAG, nbt.getDouble(ORIGINAL_DYNAMIC_ATTACK_TAG));
                nbt.remove(ORIGINAL_DYNAMIC_ATTACK_TAG);
            } else if (nbt.contains(DYNAMIC_ATTACK_TAG)) {
                nbt.remove(DYNAMIC_ATTACK_TAG);
            }
            if (entity.getActivePotionEffect(this) == null) {
                nbt.remove(NATURAL_ATTACK_TAG);
            }
        }
    }
    private void removeDamageMultiplierEffect(LivingEntity entity) {
        ModifiableAttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            removeAttributeModifier(attackAttr, DAMAGE_MULTIPLIER_UUID);
        }
    }
    private void handleFireImmunity(LivingEntity entity, int amplifier) {
        int level = amplifier + 1;
        if (level >= 10) {
            if (entity.isBurning()) {
                entity.extinguish();
            }
        }
    }
    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        handleFireImmunity(entity, amplifier);
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, UUID uuid, double amount, AttributeModifier.Operation operation) {
        AttributeModifier existingModifier = attribute.getModifier(uuid);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
        AttributeModifier newModifier = new AttributeModifier(uuid, "nether", amount, operation);
        attribute.applyNonPersistentModifier(newModifier);
    }
    private void removeAttributeModifier(ModifiableAttributeInstance attribute, UUID uuid) {
        AttributeModifier existingModifier = attribute.getModifier(uuid);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
    }
}