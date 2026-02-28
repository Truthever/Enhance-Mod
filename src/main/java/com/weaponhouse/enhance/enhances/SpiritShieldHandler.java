package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.network.SpiritShieldPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.fml.network.PacketDistributor;
public class SpiritShieldHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String SPIRIT_SHIELD_TAG = "spirit_shield";
    private static final String SPIRIT_SHIELD_DATA = "SpiritShieldData";
    private static final float SHIELD_PER_LEVEL = 2.0f;
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!hasSpiritShieldBuff(entity)) {
            return;
        }
        float currentHealth = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healAmount = event.getAmount();
        float overflowHeal = 0.0f;
        if (currentHealth + healAmount > maxHealth) {
            overflowHeal = (currentHealth + healAmount) - maxHealth;
        }
        if (overflowHeal > 0) {
            convertHealToShield(entity, overflowHeal);
        }
    }
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!hasSpiritShield(entity)) {
            return;
        }
        float damage = event.getAmount();
        SpiritShieldData shieldData = getSpiritShieldData(entity);
        if (shieldData.currentShield > 0) {
            float shieldDamage = Math.min(damage, shieldData.currentShield);
            shieldData.currentShield -= shieldDamage;
            damage -= shieldDamage;
            setSpiritShieldData(entity, shieldData);
            if (damage <= 0) {
                event.setCanceled(true);
                event.setAmount(0.001f);
            } else {
                event.setAmount(damage);
            }
            syncSpiritShieldToClient(entity, shieldData);
        }
    }
    private static boolean hasSpiritShieldBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        return entityData.contains(BUFF_TAG) &&
                entityData.getCompound(BUFF_TAG).contains(SPIRIT_SHIELD_TAG) &&
                entityData.getCompound(BUFF_TAG).getInt(SPIRIT_SHIELD_TAG) > 0;
    }
    private static int getSpiritShieldLevel(LivingEntity entity) {
        CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
        return buffs.getInt(SPIRIT_SHIELD_TAG);
    }
    private static void convertHealToShield(LivingEntity entity, float overflowHeal) {
        SpiritShieldData shieldData = getSpiritShieldData(entity);
        int shieldLevel = getSpiritShieldLevel(entity);
        float maxShield = shieldLevel * SHIELD_PER_LEVEL;
        shieldData.currentShield = Math.min(shieldData.currentShield + overflowHeal, maxShield);
        shieldData.maxShield = maxShield;
        setSpiritShieldData(entity, shieldData);
        syncSpiritShieldToClient(entity, shieldData);
    }
    private static boolean hasSpiritShield(LivingEntity entity) {
        return hasSpiritShieldBuff(entity) && getSpiritShieldData(entity).currentShield > 0;
    }
    public static SpiritShieldData getSpiritShieldData(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (!entityData.contains(SPIRIT_SHIELD_DATA)) {
            return new SpiritShieldData(0, 0);
        }
        CompoundNBT shieldData = entityData.getCompound(SPIRIT_SHIELD_DATA);
        float currentShield = shieldData.getFloat("currentShield");
        float maxShield = shieldData.getFloat("maxShield");
        return new SpiritShieldData(currentShield, maxShield);
    }
    private static void setSpiritShieldData(LivingEntity entity, SpiritShieldData data) {
        CompoundNBT entityData = entity.getPersistentData();
        CompoundNBT shieldData = new CompoundNBT();
        shieldData.putFloat("currentShield", data.currentShield);
        shieldData.putFloat("maxShield", data.maxShield);
        entityData.put(SPIRIT_SHIELD_DATA, shieldData);
    }

    private static void syncSpiritShieldToClient(LivingEntity entity, SpiritShieldData data) {
        // 同步灵盾数据到客户端（如果是玩家）
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            Enhance.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SpiritShieldPacket(data.currentShield, data.maxShield)
            );
        }
    }
    public static void initializeSpiritShield(LivingEntity entity) {
        if (hasSpiritShieldBuff(entity)) {
            int shieldLevel = getSpiritShieldLevel(entity);
            float maxShield = shieldLevel * SHIELD_PER_LEVEL;
            SpiritShieldData data = new SpiritShieldData(0, maxShield);
            setSpiritShieldData(entity, data);
            syncSpiritShieldToClient(entity, data);
        }
    }
    public static void resetSpiritShield(LivingEntity entity) {
        setSpiritShieldData(entity, new SpiritShieldData(0, 0));
        syncSpiritShieldToClient(entity, new SpiritShieldData(0, 0));
    }

    public static class SpiritShieldData {
        public float currentShield;
        public float maxShield;
        public SpiritShieldData(float currentShield, float maxShield) {
            this.currentShield = currentShield;
            this.maxShield = maxShield;
        }
    }
}