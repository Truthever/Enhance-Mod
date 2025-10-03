package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.nbt.CompoundNBT;
import java.util.UUID;
public class DesertAttackEffect extends BaseEffect {
    private static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public DesertAttackEffect() {
        super(EffectType.BENEFICIAL, 0xFFA500);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance attackDamageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            CompoundNBT nbt = entity.getPersistentData();
            if (!nbt.contains("BaseAttackDamage")) {
                nbt.putDouble("BaseAttackDamage", attackDamageAttr.getBaseValue());
            }
            if (!nbt.contains("dynamicAttackDamage")) {
                nbt.putDouble("dynamicAttackDamage", attackDamageAttr.getBaseValue());
            }
            if (!nbt.contains("originalDynamicAttackDamage")) {
                nbt.putDouble("originalDynamicAttackDamage", nbt.getDouble("dynamicAttackDamage"));
            }
            double currentDynamicAttack = nbt.getDouble("dynamicAttackDamage");
            double amount = (amplifier + 1) * 2.0;
            double adjustment = amount;
            double newDynamicAttack = currentDynamicAttack + adjustment;
            applyAttributeModifier(attackDamageAttr, ATTACK_DAMAGE_MODIFIER_UUID, adjustment, AttributeModifier.Operation.ADDITION);
            nbt.putDouble("dynamicAttackDamage", newDynamicAttack);
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance attackDamageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            removeAttributeModifier(attackDamageAttr, ATTACK_DAMAGE_MODIFIER_UUID);
        }
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains("originalDynamicAttackDamage")) {
            nbt.putDouble("dynamicAttackDamage", nbt.getDouble("originalDynamicAttackDamage"));
            nbt.remove("originalDynamicAttackDamage");
        } else if (nbt.contains("dynamicAttackDamage")) {
            nbt.remove("dynamicAttackDamage");
        }
    }
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, UUID uuid, double amount, AttributeModifier.Operation operation) {
        AttributeModifier existingModifier = attribute.getModifier(uuid);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
        AttributeModifier newModifier = new AttributeModifier(uuid, "desert_attack_boost", amount, operation);
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