package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class ComboHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String COMBO_TAG = "combo";
    private static final String COMBO_DATA_TAG = "ComboData";
    private static final String COMBO_LEVEL_TAG = "ComboLevel";
    private static final String LAST_ATTACK_TIME_TAG = "LastAttackTime";
    private static final String CURRENT_ATTACK_COUNT_TAG = "CurrentAttackCount";
    private static final UUID COMBO_ATTACK_UUID = UUID.randomUUID();
    private static final UUID COMBO_SPEED_UUID = UUID.randomUUID();
    private static final int MAX_COMBO_LEVEL = 10;
    private static final float ATTACK_BONUS_PER_LEVEL = 0.1f;
    private static final float SPEED_BONUS_PER_LEVEL = 0.01f;
    private static final String COMBO_DISPLAY_KEY = "enhance.combo.display";
    private static final String COMBO_RESET_KEY = "enhance.combo.reset";
    private static final RedstoneParticleData[] COMBO_PARTICLES = {
            new RedstoneParticleData(1.0F, 0.0F, 0.0F, 1.0F),
            new RedstoneParticleData(1.0F, 0.5F, 0.0F, 1.0F),
            new RedstoneParticleData(1.0F, 1.0F, 0.0F, 1.0F),
            new RedstoneParticleData(0.5F, 1.0F, 0.0F, 1.0F),
            new RedstoneParticleData(0.0F, 1.0F, 0.0F, 1.0F),
            new RedstoneParticleData(0.0F, 1.0F, 0.5F, 1.0F),
            new RedstoneParticleData(0.0F, 1.0F, 1.0F, 1.0F),
            new RedstoneParticleData(0.0F, 0.5F, 1.0F, 1.0F),
            new RedstoneParticleData(0.5F, 0.0F, 1.0F, 1.0F),
            new RedstoneParticleData(1.0F, 0.0F, 1.0F, 1.0F)
    };
    private static final Map<UUID, Long> ACTIVE_COMBO_ENTITIES = new ConcurrentHashMap<>();
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        LivingEntity target = (LivingEntity) event.getEntity();
        if (hasComboBuff(attacker)) {
            return;
        }
        processCombo(attacker, target.getPosX(), target.getPosY(), target.getPosZ());
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        LivingEntity shooter = null;
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            shooter = (LivingEntity) arrow.getShooter();
        }
        else if (event.getEntity() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) event.getEntity();
            shooter = (LivingEntity) throwable.getShooter();
        }
        if (shooter == null || hasComboBuff(shooter)) {
            return;
        }
        if (event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
            Entity hitEntity = ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
            if (hitEntity instanceof LivingEntity) {
                processCombo(shooter, hitEntity.getPosX(), hitEntity.getPosY(), hitEntity.getPosZ());
            }
        }
    }
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = ACTIVE_COMBO_ENTITIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID entityId = entry.getKey();
            Long lastAttackTime = entry.getValue();
            if (currentTime - lastAttackTime > 3000) {
                for (ServerWorld world : ServerLifecycleHooks.getCurrentServer().getWorlds()) {
                    Entity entity = world.getEntityByUuid(entityId);
                    if (entity instanceof LivingEntity) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        int comboLevel = getCurrentComboLevel(livingEntity);
                        if (comboLevel > 0) {
                            sendComboResetMessage(livingEntity, comboLevel);
                            resetCombo(livingEntity);
                            int comboBuffLevel = getComboBuffLevel(livingEntity);
                            int attacksPerLevel = calculateAttacksPerLevel(comboBuffLevel);
                            updateComboDisplay(livingEntity, 0, 0, attacksPerLevel);
                        }
                    }
                }
                iterator.remove();
            }
        }
    }
    private static void processCombo(LivingEntity attacker, double hitX, double hitY, double hitZ) {
        CompoundNBT entityData = attacker.getPersistentData();
        CompoundNBT comboData = getComboData(attacker);
        long currentTime = System.currentTimeMillis();
        int currentAttackCount = comboData.getInt(CURRENT_ATTACK_COUNT_TAG);
        int comboLevel = comboData.getInt(COMBO_LEVEL_TAG);
        int comboBuffLevel = getComboBuffLevel(attacker);
        int attacksPerLevel = calculateAttacksPerLevel(comboBuffLevel);
        int previousAttackCount = currentAttackCount;
        currentAttackCount++;
        boolean levelUp = false;
        if (currentAttackCount >= attacksPerLevel && comboLevel < MAX_COMBO_LEVEL) {
            comboLevel++;
            currentAttackCount = 0;
            levelUp = true;
            applyComboBonuses(attacker, comboLevel);
            if (attacker.world instanceof ServerWorld) {
                spawnComboParticles(attacker, comboLevel, (ServerWorld) attacker.world);
                spawnHitParticles(hitX, hitY, hitZ, (ServerWorld) attacker.world, comboLevel);
            }
        }
        comboData.putLong(LAST_ATTACK_TIME_TAG, currentTime);
        comboData.putInt(CURRENT_ATTACK_COUNT_TAG, currentAttackCount);
        comboData.putInt(COMBO_LEVEL_TAG, comboLevel);
        entityData.put(COMBO_DATA_TAG, comboData);
        applyComboBonuses(attacker, comboLevel);
        ACTIVE_COMBO_ENTITIES.put(attacker.getUniqueID(), currentTime);
        if (previousAttackCount != currentAttackCount || levelUp) {
            updateComboDisplay(attacker, comboLevel, currentAttackCount, attacksPerLevel);
        }
    }
    private static int calculateAttacksPerLevel(int comboBuffLevel) {
        return Math.max(1, 6 - comboBuffLevel);
    }
    private static void applyComboBonuses(LivingEntity entity, int comboLevel) {
        removeComboModifiers(entity);

        if (comboLevel <= 0) {
            return;
        }
        float attackBonus = comboLevel * ATTACK_BONUS_PER_LEVEL;
        float speedBonus = comboLevel * SPEED_BONUS_PER_LEVEL;
        net.minecraft.entity.ai.attributes.ModifiableAttributeInstance attackAttr = entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            net.minecraft.entity.ai.attributes.AttributeModifier attackModifier = new net.minecraft.entity.ai.attributes.AttributeModifier(
                    COMBO_ATTACK_UUID,
                    "ComboAttackBonus",
                    attackBonus,
                    net.minecraft.entity.ai.attributes.AttributeModifier.Operation.ADDITION
            );
            attackAttr.applyPersistentModifier(attackModifier);
        }
        net.minecraft.entity.ai.attributes.ModifiableAttributeInstance speedAttr = entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            net.minecraft.entity.ai.attributes.AttributeModifier speedModifier = new net.minecraft.entity.ai.attributes.AttributeModifier(
                    COMBO_SPEED_UUID,
                    "ComboSpeedBonus",
                    speedBonus,
                    net.minecraft.entity.ai.attributes.AttributeModifier.Operation.ADDITION
            );
            speedAttr.applyPersistentModifier(speedModifier);
        }
    }
    private static void removeComboModifiers(LivingEntity entity) {
        net.minecraft.entity.ai.attributes.ModifiableAttributeInstance attackAttr = entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            net.minecraft.entity.ai.attributes.AttributeModifier existingAttack = attackAttr.getModifier(COMBO_ATTACK_UUID);
            if (existingAttack != null) {
                attackAttr.removeModifier(existingAttack);
            }
        }
        net.minecraft.entity.ai.attributes.ModifiableAttributeInstance speedAttr = entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            net.minecraft.entity.ai.attributes.AttributeModifier existingSpeed = speedAttr.getModifier(COMBO_SPEED_UUID);
            if (existingSpeed != null) {
                speedAttr.removeModifier(existingSpeed);
            }
        }
    }
    private static void resetCombo(LivingEntity entity) {
        removeComboModifiers(entity);
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(COMBO_DATA_TAG)) {
            CompoundNBT comboData = entityData.getCompound(COMBO_DATA_TAG);
            comboData.putInt(COMBO_LEVEL_TAG, 0);
            comboData.putInt(CURRENT_ATTACK_COUNT_TAG, 0);
            entityData.put(COMBO_DATA_TAG, comboData);
        }
        ACTIVE_COMBO_ENTITIES.remove(entity.getUniqueID());
    }
    private static CompoundNBT getComboData(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(COMBO_DATA_TAG)) {
            return entityData.getCompound(COMBO_DATA_TAG);
        } else {
            CompoundNBT comboData = new CompoundNBT();
            comboData.putInt(COMBO_LEVEL_TAG, 0);
            comboData.putInt(CURRENT_ATTACK_COUNT_TAG, 0);
            comboData.putLong(LAST_ATTACK_TIME_TAG, 0);
            return comboData;
        }
    }
    private static boolean hasComboBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT buffs = entityData.getCompound(BUFF_TAG);
            if (buffs.contains(COMBO_TAG) && buffs.getInt(COMBO_TAG) > 0) {
                if (!ACTIVE_COMBO_ENTITIES.containsKey(entity.getUniqueID())) {
                    CompoundNBT comboData = getComboData(entity);
                    long lastAttackTime = comboData.getLong(LAST_ATTACK_TIME_TAG);
                    ACTIVE_COMBO_ENTITIES.put(entity.getUniqueID(), lastAttackTime);
                }
                return false;
            }
        }
        ACTIVE_COMBO_ENTITIES.remove(entity.getUniqueID());
        return true;
    }
    private static int getComboBuffLevel(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT buffs = entityData.getCompound(BUFF_TAG);
            return buffs.getInt(COMBO_TAG);
        }
        return 0;
    }
    private static void updateComboDisplay(LivingEntity entity, int comboLevel, int currentAttackCount, int attacksPerLevel) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entity;
        TranslationTextComponent message;
        if (comboLevel == 0) {
            message = new TranslationTextComponent(COMBO_DISPLAY_KEY + ".basic", currentAttackCount, attacksPerLevel);
            message.mergeStyle(TextFormatting.GRAY);
        } else {
            TextFormatting color = getComboColor(comboLevel);
            message = new TranslationTextComponent(COMBO_DISPLAY_KEY + ".advanced", comboLevel, currentAttackCount, attacksPerLevel);
            message.mergeStyle(color);
            if (comboLevel >= 8) {
                message.mergeStyle(TextFormatting.BOLD);
            }
        }
        player.sendStatusMessage(message, true);
    }
    private static void sendComboResetMessage(LivingEntity entity, int lostLevel) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entity;
        TranslationTextComponent message = new TranslationTextComponent(COMBO_RESET_KEY, lostLevel);
        message.mergeStyle(TextFormatting.RED);
        player.sendMessage(message, player.getUniqueID());
    }
    private static TextFormatting getComboColor(int comboLevel) {
        switch (comboLevel) {
            case 1: return TextFormatting.WHITE;
            case 2: return TextFormatting.YELLOW;
            case 3: return TextFormatting.GOLD;
            case 4: return TextFormatting.GREEN;
            case 5: return TextFormatting.AQUA;
            case 6: return TextFormatting.BLUE;
            case 7: return TextFormatting.LIGHT_PURPLE;
            case 8: return TextFormatting.DARK_PURPLE;
            case 9: return TextFormatting.RED;
            case 10: return TextFormatting.DARK_RED;
            default: return TextFormatting.WHITE;
        }
    }
    private static void spawnComboParticles(LivingEntity entity, int comboLevel, ServerWorld world) {
        if (comboLevel <= 0 || comboLevel > COMBO_PARTICLES.length) {
            return;
        }
        RedstoneParticleData particle = COMBO_PARTICLES[comboLevel - 1];
        Vector3d pos = entity.getPositionVec();
        Random rand = world.getRandom();
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2 / 12;
            double radius = 1.0 + comboLevel * 0.1;
            double x = pos.x + Math.cos(angle) * radius;
            double y = pos.y + entity.getHeight() * 0.8;
            double z = pos.z + Math.sin(angle) * radius;
            world.spawnParticle(
                    particle,
                    x, y, z,
                    1,
                    0.1, 0.1, 0.1,
                    0.05
            );
        }
        for (int i = 0; i < comboLevel * 2; i++) {
            double x = pos.x + (rand.nextDouble() - 0.5) * 0.5;
            double y = pos.y + entity.getHeight() + 0.5 + rand.nextDouble() * 0.5;
            double z = pos.z + (rand.nextDouble() - 0.5) * 0.5;
            world.spawnParticle(
                    particle,
                    x, y, z,
                    1,
                    0, 0.1, 0,
                    0.1
            );
        }
    }
    private static void spawnHitParticles(double x, double y, double z, ServerWorld world, int comboLevel) {
        if (comboLevel <= 0 || comboLevel > COMBO_PARTICLES.length) {
            return;
        }
        RedstoneParticleData particle = COMBO_PARTICLES[comboLevel - 1];
        Random rand = world.getRandom();
        for (int i = 0; i < 8 + comboLevel * 2; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 1.5;
            double offsetY = (rand.nextDouble() - 0.5) * 1.5;
            double offsetZ = (rand.nextDouble() - 0.5) * 1.5;
            world.spawnParticle(
                    particle,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1,
                    0, 0, 0,
                    0.1
            );
        }
    }
    public static int getCurrentComboLevel(LivingEntity entity) {
        CompoundNBT comboData = getComboData(entity);
        return comboData.getInt(COMBO_LEVEL_TAG);
    }
}