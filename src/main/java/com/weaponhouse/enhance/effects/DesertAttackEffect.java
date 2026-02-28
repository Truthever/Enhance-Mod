package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectType;

import java.util.UUID;

public class DesertAttackEffect extends BaseEffect {
    private static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final String VILLAGER_ORIGINAL_ATTACK = "DesertEffect_OriginalVillagerAttack";
    private static final String VILLAGER_BONUS_ATTACK = "DesertEffect_VillagerBonusAttack";
    public DesertAttackEffect() {
        super(EffectType.BENEFICIAL, 0xFFA500);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof VillagerEntity) {
            applyVillagerEffect((VillagerEntity) entity, amplifier);
        } else {
            applyNormalEffect(entity, amplifier);
        }
    }
    private void applyVillagerEffect(VillagerEntity villager, int amplifier) {
        CompoundNBT nbt = villager.getPersistentData();
        if (!nbt.contains(VILLAGER_ORIGINAL_ATTACK)) {
            double currentDynamicDamage = 5.0;
            if (nbt.contains("DynamicAttackDamage")) {
                currentDynamicDamage = nbt.getDouble("DynamicAttackDamage");
            } else if (nbt.contains("CustomAttackDamage")) {
                currentDynamicDamage = nbt.getDouble("CustomAttackDamage");
            }
            nbt.putDouble(VILLAGER_ORIGINAL_ATTACK, currentDynamicDamage);
        }
        double originalDynamicDamage = nbt.getDouble(VILLAGER_ORIGINAL_ATTACK);
        double bonusAmount = (amplifier + 1) * 1.0;
        if (nbt.contains(VILLAGER_BONUS_ATTACK)) {
            originalDynamicDamage = nbt.getDouble(VILLAGER_ORIGINAL_ATTACK);
        }
        double newDynamicDamage = originalDynamicDamage + bonusAmount;
        nbt.putDouble("DynamicAttackDamage", newDynamicDamage);
        nbt.putDouble(VILLAGER_BONUS_ATTACK, bonusAmount);
        nbt.putBoolean("HasCustomAttackDamage", true);
    }
    private void applyNormalEffect(LivingEntity entity, int amplifier) {
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
            double adjustment = (amplifier + 1) * 2.0;
            double newDynamicAttack = currentDynamicAttack + adjustment;
            applyAttributeModifier(attackDamageAttr, adjustment);
            nbt.putDouble("dynamicAttackDamage", newDynamicAttack);
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof VillagerEntity) {
            removeVillagerEffect((VillagerEntity) entity);
        } else {
            removeNormalEffect(entity);
        }
    }
    private void removeVillagerEffect(VillagerEntity villager) {
        CompoundNBT nbt = villager.getPersistentData();
        if (nbt.contains(VILLAGER_ORIGINAL_ATTACK)) {
            double originalDynamicDamage = nbt.getDouble(VILLAGER_ORIGINAL_ATTACK);
            nbt.putDouble("DynamicAttackDamage", originalDynamicDamage);
            double baseDamage = 5.0;
            if (nbt.contains("BaseAttackDamage")) {
                baseDamage = nbt.getDouble("BaseAttackDamage");
            }
            if (originalDynamicDamage == baseDamage && !hasOtherAttackBuffs(villager)) {
                nbt.remove("DynamicAttackDamage");
                nbt.remove("HasCustomAttackDamage");
            }
            nbt.remove(VILLAGER_BONUS_ATTACK);
            nbt.remove(VILLAGER_ORIGINAL_ATTACK);
        } else {
            com.weaponhouse.enhance.enhances.AttackHandler.recalculateVillagerDynamicAttack(villager);
        }
    }
    private void removeNormalEffect(LivingEntity entity) {
        ModifiableAttributeInstance attackDamageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            removeAttributeModifier(attackDamageAttr);
        }
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains("originalDynamicAttackDamage")) {
            nbt.putDouble("dynamicAttackDamage", nbt.getDouble("originalDynamicAttackDamage"));
            nbt.remove("originalDynamicAttackDamage");
        } else if (nbt.contains("dynamicAttackDamage")) {
            nbt.remove("dynamicAttackDamage");
        }
    }
    private boolean hasOtherAttackBuffs(VillagerEntity villager) {
        CompoundNBT data = villager.getPersistentData();
        if (data.contains("WeaponHouseBuffs")) {
            CompoundNBT buffs = data.getCompound("WeaponHouseBuffs");
            return buffs.contains("attack") && buffs.getInt("attack") > 0;
        }
        return false;
    }
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, double amount) {
        AttributeModifier existingModifier = attribute.getModifier(DesertAttackEffect.ATTACK_DAMAGE_MODIFIER_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
        AttributeModifier newModifier = new AttributeModifier(DesertAttackEffect.ATTACK_DAMAGE_MODIFIER_UUID, "desert_attack_boost", amount, AttributeModifier.Operation.ADDITION);
        attribute.applyNonPersistentModifier(newModifier);
    }
    private void removeAttributeModifier(ModifiableAttributeInstance attribute) {
        AttributeModifier existingModifier = attribute.getModifier(DesertAttackEffect.ATTACK_DAMAGE_MODIFIER_UUID);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return super.isReady(duration, amplifier);
    }
}