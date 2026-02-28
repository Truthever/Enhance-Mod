package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.UUID;
public class AttackHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    public static final String ATTACK_TAG = "attack";
    public static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("e7e8c2c3-2b3d-5e9f-8a7b-1d6c5b4a3e2f");
    public static final float ATTACK_BONUS_PER_LEVEL = 0.05f;
    public static final String INTERNAL_ORIGINAL_ATTACK = "Enhance_Internal_OriginalAttack";
    private static final String VILLAGER_BASE_ATTACK = "BaseAttackDamage";
    private static final String VILLAGER_DYNAMIC_ATTACK = "DynamicAttackDamage";
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            applyAttackBuff((LivingEntity) event.getEntity());
        }
    }
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        applyAttackBuff(event.getPlayer());
    }
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        applyAttackBuff(event.getPlayer());
    }
    public static void applyAttackBuff(LivingEntity entity) {
        if (entity.world.isRemote) return;
        if (entity instanceof VillagerEntity) {
            applyVillagerAttackBuff((VillagerEntity) entity);
            return;
        }
        int attackLevel = getAttackLevel(entity);
        ModifiableAttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr == null) return;
        if (attackLevel <= 0) {
            restoreOriginalAttack(entity, attackAttr);
            return;
        }
        CompoundNBT entityData = entity.getPersistentData();
        double originalBaseAttack;
        if (!entityData.contains(INTERNAL_ORIGINAL_ATTACK)) {
            originalBaseAttack = attackAttr.getBaseValue();
            entityData.putDouble(INTERNAL_ORIGINAL_ATTACK, originalBaseAttack);
        } else {
            originalBaseAttack = entityData.getDouble(INTERNAL_ORIGINAL_ATTACK);
        }
        AttributeModifier existing = attackAttr.getModifier(ATTACK_MODIFIER_UUID);
        if (existing != null) {
            attackAttr.removeModifier(existing);
        }
        double bonusAmount = originalBaseAttack * (attackLevel * ATTACK_BONUS_PER_LEVEL);
        AttributeModifier modifier = new AttributeModifier(
                ATTACK_MODIFIER_UUID,
                "AttackBuffModifier",
                bonusAmount,
                AttributeModifier.Operation.ADDITION
        );
        attackAttr.applyPersistentModifier(modifier);
    }
    private static void applyVillagerAttackBuff(VillagerEntity villager) {
        int attackLevel = getAttackLevel(villager);
        if (attackLevel <= 0) return;
        CompoundNBT nbt = villager.getPersistentData();
        if (!nbt.contains(VILLAGER_BASE_ATTACK)) {
            nbt.putDouble(VILLAGER_BASE_ATTACK, 5.0);
        }
        double baseDamage = nbt.getDouble(VILLAGER_BASE_ATTACK);
        double attackBonus = baseDamage * attackLevel * ATTACK_BONUS_PER_LEVEL;
        double newDynamicDamage = baseDamage + attackBonus;
        nbt.putDouble(VILLAGER_DYNAMIC_ATTACK, newDynamicDamage);
        nbt.putBoolean("HasCustomAttackDamage", true);
    }
    public static void restoreOriginalAttack(LivingEntity entity, ModifiableAttributeInstance attackAttr) {
        if (attackAttr == null || entity.world.isRemote) return;
        if (entity instanceof VillagerEntity) return;
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(INTERNAL_ORIGINAL_ATTACK)) {
            double originalBaseAttack = entityData.getDouble(INTERNAL_ORIGINAL_ATTACK);
            attackAttr.setBaseValue(originalBaseAttack);
            AttributeModifier existing = attackAttr.getModifier(ATTACK_MODIFIER_UUID);
            if (existing != null) {
                attackAttr.removeModifier(existing);
            }
            entityData.remove(INTERNAL_ORIGINAL_ATTACK);
        }
    }
    private static int getAttackLevel(LivingEntity entity) {
        CompoundNBT data = entity.getPersistentData();
        if (data.contains(BUFF_TAG)) {
            CompoundNBT buffs = data.getCompound(BUFF_TAG);
            if (buffs.contains(ATTACK_TAG)) {
                return buffs.getInt(ATTACK_TAG);
            }
        }
        return 0;
    }
    public static void syncAttackBuff(LivingEntity entity) {
        if (!(entity instanceof VillagerEntity)) {
            restoreOriginalAttack(entity, entity.getAttribute(Attributes.ATTACK_DAMAGE));
        }
        applyAttackBuff(entity);
    }
    public static double getVillagerDynamicAttack(VillagerEntity villager) {
        CompoundNBT nbt = villager.getPersistentData();
        if (nbt.contains(VILLAGER_DYNAMIC_ATTACK)) {
            return nbt.getDouble(VILLAGER_DYNAMIC_ATTACK);
        }
        if (nbt.contains("CustomAttackDamage")) {
            return nbt.getDouble("CustomAttackDamage");
        }
        return 5.0;
    }
    public static double getVillagerBaseAttack(VillagerEntity villager) {
        CompoundNBT nbt = villager.getPersistentData();
        if (nbt.contains(VILLAGER_BASE_ATTACK)) {
            return nbt.getDouble(VILLAGER_BASE_ATTACK);
        }
        return 5.0;
    }
    public static void recalculateVillagerDynamicAttack(VillagerEntity villager) {
        CompoundNBT nbt = villager.getPersistentData();
        double baseDamage = getVillagerBaseAttack(villager);
        double dynamicDamage = baseDamage;
        int attackLevel = getAttackLevel(villager);
        if (attackLevel > 0) {
            double attackBonus = baseDamage * attackLevel * ATTACK_BONUS_PER_LEVEL;
            dynamicDamage += attackBonus;
        }
        nbt.putDouble(VILLAGER_DYNAMIC_ATTACK, dynamicDamage);
        nbt.putBoolean("HasCustomAttackDamage", true);
    }
}