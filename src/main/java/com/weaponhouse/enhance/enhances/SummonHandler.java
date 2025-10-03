package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid = "enhance")
public class SummonHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String SUMMON_TAG = "summon";
    private static final float BASE_TRIGGER_CHANCE = 0.03F;
    private static final float SUMMON_RANGE = 2.0F;
    private static final int MAX_SUMMON_ATTEMPTS = 5;
    private static final Map<String, Integer> MARK_TO_AMPLIFY_LEVEL = new HashMap<String, Integer>() {{
        put("one_enhance", 1);
        put("two_enhance", 2);
        put("three_enhance", 3);
    }};
    private static final Map<Integer, Integer> LEVEL_TO_NORMAL_COUNT = new HashMap<Integer, Integer>() {{
        put(1, 2);
        put(2, 3);
        put(3, 5);
    }};
    private static final Set<String> ALL_MARK_TAGS = MARK_TO_AMPLIFY_LEVEL.keySet();
    private static final String SUMMONED_MOB_TAG = "summoned_by_buff";
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity hurtEntity = event.getEntityLiving();
        World world = hurtEntity.world;
        if (world.isRemote || !(hurtEntity instanceof MobEntity) || !hasSummonBuff(hurtEntity) || event.getSource().getTrueSource() == null) {
            return;
        }
        if (hurtEntity instanceof PlayerEntity) {
            removeSummonBuff(hurtEntity);
            return;
        }
        MobEntity hurtMob = (MobEntity) hurtEntity;
        int summonLevel = getSummonLevel(hurtMob);
        float triggerChance = BASE_TRIGGER_CHANCE * summonLevel;
        Random random = world.rand;
        if (random.nextFloat() < triggerChance) {
            attemptSummonSameMob(hurtMob);
        }
    }
    private static void attemptSummonSameMob(MobEntity parentMob) {
        World world = parentMob.world;
        if (world.isRemote) return;
        EntityType<? extends MobEntity> mobType = (EntityType<? extends MobEntity>) parentMob.getType();
        if (isUnsummonableType(mobType)) {
            return;
        }
        BlockPos summonPos = findValidSummonPos(parentMob);
        if (summonPos == null) {
            spawnSummonFailParticles(parentMob);
            return;
        }
        try {
            MobEntity summonedMob = mobType.create(world);
            if (summonedMob == null) return;
            summonedMob.setPosition(
                    summonPos.getX() + 0.5D,
                    summonPos.getY() + 0.5D,
                    summonPos.getZ() + 0.5D
            );
            summonedMob.setHealth(parentMob.getHealth() * 0.8F);
            copyMobBuffs(parentMob, summonedMob);
            summonedMob.addTag(SUMMONED_MOB_TAG);
            if (parentMob.getAttackTarget() != null) {
                summonedMob.setAttackTarget(parentMob.getAttackTarget());
            }
            world.addEntity(summonedMob);
            spawnSummonSuccessParticles(summonedMob);
        } catch (Exception e) {
        }
    }
    private static BlockPos findValidSummonPos(MobEntity parentMob) {
        World world = parentMob.world;
        BlockPos parentPos = parentMob.getPosition();
        Random random = world.rand;
        for (int i = 0; i < MAX_SUMMON_ATTEMPTS; i++) {
            int offsetX = random.nextInt((int) (SUMMON_RANGE * 2)) - (int) SUMMON_RANGE;
            int offsetY = random.nextInt(3) - 1;
            int offsetZ = random.nextInt((int) (SUMMON_RANGE * 2)) - (int) SUMMON_RANGE;
            BlockPos candidatePos = parentPos.add(offsetX, offsetY, offsetZ);
            Vector3d candidateVec = new Vector3d(
                    candidatePos.getX() + 0.5D,
                    candidatePos.getY(),
                    candidatePos.getZ() + 0.5D
            );
            boolean isGroundSolid = world.getBlockState(candidatePos.down()).isSolid();
            boolean isPosAir = world.getBlockState(candidatePos).isAir();
            boolean isAboveAir = world.getBlockState(candidatePos.up()).isAir();
            boolean noEntities = world.getEntitiesWithinAABB(
                    LivingEntity.class,
                    new net.minecraft.util.math.AxisAlignedBB(candidateVec, candidateVec).grow(0.8D)
            ).isEmpty();
            if (isGroundSolid && isPosAir && isAboveAir && noEntities) {
                return candidatePos;
            }
        }
        return null;
    }
    private static boolean isUnsummonableType(EntityType<? extends MobEntity> mobType) {
        String mobId = mobType.getRegistryName().toString();
        return mobId.contains("ender_dragon") ||
                mobId.contains("wither") ||
                mobId.contains("villager") ||
                mobId.contains("wandering_trader");
    }
    private static void copyMobBuffs(MobEntity parent, MobEntity child) {
        CompoundNBT parentData = parent.getPersistentData();
        CompoundNBT childData = child.getPersistentData();
        Random random = new Random();
        if (!parentData.contains(BUFF_TAG, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        CompoundNBT parentBuffs = parentData.getCompound(BUFF_TAG);
        CompoundNBT childBuffs = new CompoundNBT();
        String parentMarkTag = null;
        int parentAmplifyLevel = 0;
        for (Map.Entry<String, Integer> entry : MARK_TO_AMPLIFY_LEVEL.entrySet()) {
            String markTag = entry.getKey();
            if (parentBuffs.contains(markTag)) {
                parentMarkTag = markTag;
                parentAmplifyLevel = entry.getValue();
                break;
            }
        }
        if (parentMarkTag != null) {
            if (parentBuffs.contains(parentMarkTag, Constants.NBT.TAG_INT)) {
                childBuffs.putInt(parentMarkTag, parentBuffs.getInt(parentMarkTag));
            } else {
                childBuffs.putString(parentMarkTag, parentBuffs.getString(parentMarkTag));
            }
            for (String tag : ALL_MARK_TAGS) {
                child.removeTag(tag);
            }
            child.addTag(parentMarkTag);
        }
        List<String> normalTags = new ArrayList<>();
        for (String tagKey : parentBuffs.keySet()) {
            if (!ALL_MARK_TAGS.contains(tagKey) && !tagKey.equals(SUMMON_TAG)) {
                normalTags.add(tagKey);
            }
        }
        int targetCount = LEVEL_TO_NORMAL_COUNT.getOrDefault(parentAmplifyLevel, 0);
        int actualCount = Math.min(targetCount, normalTags.size());
        if (actualCount > 0) {
            Collections.shuffle(normalTags, random);
            for (int i = 0; i < actualCount; i++) {
                String normalTag = normalTags.get(i);
                if (parentBuffs.contains(normalTag, Constants.NBT.TAG_INT)) {
                    childBuffs.putInt(normalTag, parentBuffs.getInt(normalTag));
                } else if (parentBuffs.contains(normalTag, Constants.NBT.TAG_STRING)) {
                    childBuffs.putString(normalTag, parentBuffs.getString(normalTag));
                } else if (parentBuffs.contains(normalTag, Constants.NBT.TAG_FLOAT)) {
                    childBuffs.putFloat(normalTag, parentBuffs.getFloat(normalTag));
                } else if (parentBuffs.contains(normalTag, Constants.NBT.TAG_DOUBLE)) {
                    childBuffs.putDouble(normalTag, parentBuffs.getDouble(normalTag));
                }
            }
        }
        if (!childBuffs.isEmpty()) {
            childData.put(BUFF_TAG, childBuffs);
        }
    }
    private static void spawnSummonSuccessParticles(MobEntity mob) {
        World world = mob.world;
        if (world.isRemote) {
            Random random = world.rand;
            for (int i = 0; i < 12 + random.nextInt(4); i++) {
                world.addParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        mob.getPosX() + (random.nextDouble() - 0.5) * mob.getWidth(),
                        mob.getPosY() + random.nextDouble() * mob.getHeight(),
                        mob.getPosZ() + (random.nextDouble() - 0.5) * mob.getWidth(),
                        0.0D, 0.1D, 0.0D
                );
            }
        }
    }
    private static void spawnSummonFailParticles(MobEntity mob) {
        World world = mob.world;
        if (world.isRemote) {
            Random random = world.rand;
            for (int i = 0; i < 8 + random.nextInt(3); i++) {
                world.addParticle(
                        ParticleTypes.ANGRY_VILLAGER,
                        mob.getPosX() + (random.nextDouble() - 0.5) * mob.getWidth(),
                        mob.getPosY() + mob.getHeight() / 2,
                        mob.getPosZ() + (random.nextDouble() - 0.5) * mob.getWidth(),
                        0.0D, 0.05D, 0.0D
                );
            }
        }
    }
    public static boolean hasSummonBuff(LivingEntity entity) {
        if (!(entity instanceof MobEntity)) {
            return false;
        }
        CompoundNBT entityData = entity.getPersistentData();
        return entityData.contains(BUFF_TAG) &&
                entityData.getCompound(BUFF_TAG).contains(SUMMON_TAG) &&
                entityData.getCompound(BUFF_TAG).getInt(SUMMON_TAG) > 0;
    }
    public static int getSummonLevel(LivingEntity entity) {
        if (!hasSummonBuff(entity)) {
            return 0;
        }
        CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
        return buffs.getInt(SUMMON_TAG);
    }
    public static void removeSummonBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT buffs = entityData.getCompound(BUFF_TAG);
            buffs.putInt(SUMMON_TAG, 0);
            entityData.put(BUFF_TAG, buffs);
        }
    }
}
