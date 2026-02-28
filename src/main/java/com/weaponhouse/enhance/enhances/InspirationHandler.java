package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.*;
import java.util.stream.Collectors;
public class InspirationHandler {
    private static final Random RANDOM = new Random();
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String INSPIRATION_MARKER_TAG = "InspirationMarker";
    private static final int INSPIRATION_RANGE = 16;
    private static final int INSPIRATION_DURATION = 1200;
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!hasInspirationBuff(entity)) {
            return;
        }
        float currentHealth = entity.getHealth() - event.getAmount();
        float maxHealth = entity.getMaxHealth();

        if (currentHealth > maxHealth * 0.5f) {
            return;
        }
        triggerInspirationWave(entity);
        removeInspirationBuff(entity);
    }
    private static boolean hasInspirationBuff(LivingEntity entity) {
        CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
        return buffs.contains("inspiration") && buffs.getInt("inspiration") > 0;
    }
    private static void removeInspirationBuff(LivingEntity entity) {
        CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
        if (buffs.contains("inspiration")) {
            buffs.remove("inspiration");
            entity.getPersistentData().put(BUFF_TAG, buffs);
            BossBarHandler.createOrUpdateBossBar(entity);
        }
    }
    private static void triggerInspirationWave(LivingEntity sourceEntity) {
        World world = sourceEntity.world;
        int inspirationLevel = getInspirationLevel(sourceEntity);
        spawnInspirationParticles(world, sourceEntity.getPositionVec());
        playInspirationSound(world, sourceEntity.getPositionVec());
        List<LivingEntity> sameTypeEntities = findSameTypeEntities(sourceEntity);
        for (LivingEntity target : sameTypeEntities) {
            if (target == sourceEntity) continue;
            applyRandomBuffToTarget(target, inspirationLevel);
        }
        removeInspirationBuff(sourceEntity);
        if (sourceEntity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) sourceEntity;
            player.sendMessage(new TranslationTextComponent(
                    "message.enhance_inspiration.triggered",
                    sameTypeEntities.size() - 1
            ).mergeStyle(TextFormatting.LIGHT_PURPLE), player.getUniqueID());
        }
    }
    private static List<LivingEntity> findSameTypeEntities(LivingEntity source) {
        World world = source.world;
        AxisAlignedBB area = new AxisAlignedBB(
                source.getPosX() - (double) InspirationHandler.INSPIRATION_RANGE, source.getPosY() - (double) InspirationHandler.INSPIRATION_RANGE, source.getPosZ() - (double) InspirationHandler.INSPIRATION_RANGE,
                source.getPosX() + (double) InspirationHandler.INSPIRATION_RANGE, source.getPosY() + (double) InspirationHandler.INSPIRATION_RANGE, source.getPosZ() + (double) InspirationHandler.INSPIRATION_RANGE
        );
        List<LivingEntity> nearbyEntities = world.getEntitiesWithinAABB(LivingEntity.class, area);
        return nearbyEntities.stream()
                .filter(entity -> isSameEntityType(source, entity))
                .collect(Collectors.toList());
    }
    private static boolean isSameEntityType(LivingEntity entity1, LivingEntity entity2) {
        if (entity1 instanceof PlayerEntity) {
            return entity2 instanceof PlayerEntity;
        }
        return entity1.getType() == entity2.getType();
    }
    private static int getInspirationLevel(LivingEntity entity) {
        CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
        return buffs.getInt("inspiration");
    }
    private static void applyRandomBuffToTarget(LivingEntity target, int inspirationLevel) {
        if (hasInspirationMarker(target)) {
            return;
        }
        List<String> availableBuffs = getAvailableBuffsForLevel(inspirationLevel, target.world);
        if (availableBuffs.isEmpty()) {
            return;
        }
        List<String> filteredBuffs = filterExistingBuffs(availableBuffs, target);
        if (filteredBuffs.isEmpty()) {
            return;
        }
        String selectedBuff = filteredBuffs.get(RANDOM.nextInt(filteredBuffs.size()));
        int buffLevel = generateBuffLevel(inspirationLevel, selectedBuff);
        applyBuffToEntity(target, selectedBuff, buffLevel);
        setInspirationMarker(target, selectedBuff, buffLevel);
        BossBarHandler.createOrUpdateBossBar(target);
        spawnBuffParticles(target.world, target.getPositionVec());
        if (target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) target;
            player.sendMessage(new TranslationTextComponent(
                    "message.enhance_inspiration.buff_received",
                    getBuffDisplayName(selectedBuff),
                    buffLevel
            ).mergeStyle(TextFormatting.AQUA), player.getUniqueID());
        }
    }
    private static void applyBuffToEntity(LivingEntity entity, String buffType, int level) {
        CompoundNBT data = entity.getPersistentData();
        CompoundNBT buffs = data.contains(BUFF_TAG) ? data.getCompound(BUFF_TAG) : new CompoundNBT();
        buffs.putInt(buffType, level);
        data.put(BUFF_TAG, buffs);
        if ("life".equals(buffType)) {
            LifeHandler.applyLifeBuff(entity);
        }
        if ("attack".equals(buffType)) {
            AttackHandler.applyAttackBuff(entity);
        }
        if ("spirit_shield".equals(buffType)) {
            SpiritShieldHandler.initializeSpiritShield(entity);
        }
    }
    private static boolean hasInspirationMarker(LivingEntity entity) {
        CompoundNBT marker = entity.getPersistentData().getCompound(INSPIRATION_MARKER_TAG);
        return marker.contains("active") && marker.getBoolean("active");
    }
    private static void setInspirationMarker(LivingEntity entity, String buffName, int level) {
        CompoundNBT marker = new CompoundNBT();
        marker.putBoolean("active", true);
        marker.putString("buffName", buffName);
        marker.putInt("buffLevel", level);
        marker.putLong("expireTime", entity.world.getGameTime() + INSPIRATION_DURATION);
        entity.getPersistentData().put(INSPIRATION_MARKER_TAG, marker);
    }
    private static List<String> getAvailableBuffsForLevel(int inspirationLevel, World world) {
        String difficulty = getDifficultyString(world.getDifficulty());
        List<String> availableBuffs;
        switch (inspirationLevel) {
            case 1:
                availableBuffs = getTierOneBuffs(difficulty);
                break;
            case 2:
                availableBuffs = getTierOneAndTwoBuffs(difficulty);
                break;
            case 3:
                availableBuffs = getAllTierBuffs(difficulty);
                break;
            default:
                availableBuffs = new ArrayList<>();
        }
        return filterOutUnwantedBuffs(availableBuffs);
    }
    private static List<String> filterOutUnwantedBuffs(List<String> buffs) {
        List<String> filtered = new ArrayList<>();
        Set<String> excludedBuffs = new HashSet<>(Arrays.asList("attack", "life", "inspiration", "summon","enhance_level","one_enhance","two_enhance","three_enhance"));

        for (String buff : buffs) {
            if (!excludedBuffs.contains(buff)) {
                filtered.add(buff);
            }
        }
        return filtered;
    }
    private static String getDifficultyString(Difficulty difficulty) {
        switch (difficulty) {
            case PEACEFUL:
            case EASY:
                return "easy";
            case HARD:
                return "hard";
            case NORMAL:
            default:
                return "normal";
        }
    }
    private static List<String> getTierOneBuffs(String difficulty) {
        List<String> buffs = new ArrayList<>(com.weaponhouse.enhance.util.ConfigLoader.TIER_ONE_BUFFS_BY_DIFFICULTY
                .getOrDefault(difficulty, new ArrayList<>()));
        return filterOutUnwantedBuffs(buffs);
    }
    private static List<String> getTierOneAndTwoBuffs(String difficulty) {
        List<String> allBuffs = new ArrayList<>();
        allBuffs.addAll(getTierOneBuffs(difficulty));
        allBuffs.addAll(com.weaponhouse.enhance.util.ConfigLoader.TIER_TWO_BUFFS_BY_DIFFICULTY
                .getOrDefault(difficulty, new ArrayList<>()));
        return filterOutUnwantedBuffs(allBuffs);
    }
    private static List<String> getAllTierBuffs(String difficulty) {
        List<String> allBuffs = new ArrayList<>();
        allBuffs.addAll(getTierOneAndTwoBuffs(difficulty));
        allBuffs.addAll(com.weaponhouse.enhance.util.ConfigLoader.TIER_THREE_BUFFS_BY_DIFFICULTY
                .getOrDefault(difficulty, new ArrayList<>()));
        return filterOutUnwantedBuffs(allBuffs);
    }
    private static List<String> filterExistingBuffs(List<String> availableBuffs, LivingEntity target) {
        CompoundNBT permanentBuffs = target.getPersistentData().getCompound(BUFF_TAG);
        return availableBuffs.stream()
                .filter(buff -> !permanentBuffs.contains(buff))
                .collect(Collectors.toList());
    }
    private static int generateBuffLevel(int inspirationLevel, String buffName) {
        int baseLevel = 1;
        switch (inspirationLevel) {
            case 1:
                baseLevel = RANDOM.nextInt(3) + 1;
                break;
            case 2:
                baseLevel = RANDOM.nextInt(5) + 1;
                break;
            case 3:
                baseLevel = RANDOM.nextInt(8) + 1;
                break;
        }
        return Math.min(baseLevel, getMaxLevelForBuff(buffName));
    }
    private static int getMaxLevelForBuff(String buffName) {
        switch (buffName) {
            case "ricochet":
                return 5;
            case "harmony":
                return 20;
            case "enhance_level":
                return com.weaponhouse.enhance.commands.EnhanceCommand.MAX_ENHANCE_LEVEL;
            default:
                return Integer.MAX_VALUE;
        }
    }
    private static String getBuffDisplayName(String buffKey) {
        return new TranslationTextComponent("buff.enhance." + buffKey).getString();
    }
    private static void spawnInspirationParticles(World world, net.minecraft.util.math.vector.Vector3d pos) {
        if (world.isRemote) return;
        for (int i = 0; i < 50; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 2;
            double offsetY = (RANDOM.nextDouble() - 0.5) * 2;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 2;

            world.addParticle(ParticleTypes.END_ROD,
                    pos.x, pos.y + 1, pos.z,
                    offsetX, offsetY, offsetZ);
        }
    }
    private static void spawnBuffParticles(World world, net.minecraft.util.math.vector.Vector3d pos) {
        if (world.isRemote) return;
        for (int i = 0; i < 10; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 1;
            double offsetY = (RANDOM.nextDouble() - 0.5) * 1;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 1;

            world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    pos.x, pos.y + 1, pos.z,
                    offsetX, offsetY, offsetZ);
        }
    }
    private static void playInspirationSound(World world, net.minecraft.util.math.vector.Vector3d pos) {
        if (world.isRemote) return;

        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                1.0F, 0.8F + RANDOM.nextFloat() * 0.4F);
    }
}