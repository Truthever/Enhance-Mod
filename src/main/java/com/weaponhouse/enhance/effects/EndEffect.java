package com.weaponhouse.enhance.effects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectType;
import java.util.UUID;
public class EndEffect extends BaseEffect {
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000900");
    private static final String NATURAL_ARMOR_TAG = "naturalArmor";
    private static final String DYNAMIC_ARMOR_TAG = "dynamicArmor";
    private static final String ORIGINAL_DYNAMIC_ARMOR_TAG = "originalDynamicArmor";
    private static final String NATURAL_ATTACK_TAG = "NaturalAttack";
    private static final String DYNAMIC_ATTACK_TAG = "DynamicAttack";
    private static final String ORIGINAL_DYNAMIC_ATTACK_TAG = "OriginalDynamicAttack";
    public EndEffect() {
        super(EffectType.BENEFICIAL, 0x9400D3);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        applyDefenseEffect(entity, amplifier);
        applyAttackEffect(entity, amplifier);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        removeDefenseEffect(entity);
        removeAttackEffect(entity);
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
        double adjustment = amplifier + 1;
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
            applyAttributeModifier(attackAttr, adjustment);
            nbt.putDouble(DYNAMIC_ATTACK_TAG, newDynamicAttack);
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
            removeAttributeModifier(attackAttr);
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
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, double amount) {
        AttributeModifier existingModifier = attribute.getModifier(EndEffect.ATTACK_MODIFIER_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
        AttributeModifier newModifier = new AttributeModifier(EndEffect.ATTACK_MODIFIER_UUID, "end_boost", amount, AttributeModifier.Operation.ADDITION);
        attribute.applyNonPersistentModifier(newModifier);
    }
    private void removeAttributeModifier(ModifiableAttributeInstance attribute) {
        AttributeModifier existingModifier = attribute.getModifier(EndEffect.ATTACK_MODIFIER_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
    }
}