package com.weaponhouse.enhance.invasion;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.enhances.MobEnhancementHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "enhance")
public class InvasionHandler {

    public enum InvasionState {
        INACTIVE,
        WAITING_FIRST,
        ACTIVE,
        COOLDOWN
    }

    private static final Map<UUID, InvasionData> playerInvasionData = new ConcurrentHashMap<>();
    private static final Map<UUID, List<MobEntity>> activeInvasions = new ConcurrentHashMap<>();

    private static InvasionState globalState = InvasionState.INACTIVE;
    private static long firstUnlockTime = 0;
    private static long lastInvasionTime = 0;
    private static int invasionCycle = 0;

    private static final long INVASION_START_PROTECTION_TICKS = 20L;
    private static long invasionStartTick = 0;

    private static final long DAY_TICKS = 24000L;
    private static long INVASION_INTERVAL;
    private static long INVASION_DURATION;
    private static long NIGHT_START_TICKS;

    private static final String MONSTERS_TAG = "InvasionMonsters";
    private static final String MONSTER_TYPE_TAG = "MonsterType";
    private static final String MONSTER_POS_X_TAG = "PosX";
    private static final String MONSTER_POS_Y_TAG = "PosY";
    private static final String MONSTER_POS_Z_TAG = "PosZ";
    private static final String MONSTER_ROT_Y_TAG = "RotY";
    private static final String MONSTER_NBT_TAG = "MonsterNBT";
    private static final String TARGET_PLAYER_UUID_TAG = "TargetPlayerUUID";

    private static final String PLAYER_INVASION_DATA_TAG = "PlayerInvasionData";
    private static final String PLAYER_UUID_TAG = "PlayerUUID";
    private static final String PLAYER_ENHANCE_LEVEL_TAG = "EnhanceLevel";
    private static final String PLAYER_LAST_INVASION_TIME_TAG = "LastInvasionTime";
    private static final String PLAYER_IS_IN_INVASION_TAG = "IsInInvasion";
    private static final String PLAYER_MONSTER_COUNT_TAG = "MonsterCount";
    private static final String PLAYER_LAST_RESPAWN_TIME_TAG = "LastRespawnTime";

    static {
        loadConfig();
    }

    private static class InvasionData {
        public int playerEnhanceLevel = 0;
        public long lastInvasionTime = 0;
        public boolean isInInvasion = false;
        public int invasionMonsterCount = 0;
        public long lastRespawnTime = 0;

        public CompoundNBT toNBT() {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt(PLAYER_ENHANCE_LEVEL_TAG, playerEnhanceLevel);
            nbt.putLong(PLAYER_LAST_INVASION_TIME_TAG, lastInvasionTime);
            nbt.putBoolean(PLAYER_IS_IN_INVASION_TAG, isInInvasion);
            nbt.putInt(PLAYER_MONSTER_COUNT_TAG, invasionMonsterCount);
            nbt.putLong(PLAYER_LAST_RESPAWN_TIME_TAG, lastRespawnTime);
            return nbt;
        }

        public static InvasionData fromNBT(CompoundNBT nbt) {
            InvasionData data = new InvasionData();
            data.playerEnhanceLevel = nbt.getInt(PLAYER_ENHANCE_LEVEL_TAG);
            data.lastInvasionTime = nbt.getLong(PLAYER_LAST_INVASION_TIME_TAG);
            data.isInInvasion = nbt.getBoolean(PLAYER_IS_IN_INVASION_TAG);
            data.invasionMonsterCount = nbt.getInt(PLAYER_MONSTER_COUNT_TAG);
            data.lastRespawnTime = nbt.getLong(PLAYER_LAST_RESPAWN_TIME_TAG);
            return data;
        }
    }

    public static class InvasionWorldSavedData extends WorldSavedData {
        private static final String DATA_NAME = "enhance_invasion_data";
        private CompoundNBT invasionData = new CompoundNBT();

        public InvasionWorldSavedData() {
            super(DATA_NAME);
        }

        @Override
        public void read(CompoundNBT nbt) {
            this.invasionData = nbt.getCompound("InvasionData");
        }

        @Override
        public CompoundNBT write(CompoundNBT compound) {
            compound.put("InvasionData", this.invasionData);
            return compound;
        }

        public static InvasionWorldSavedData get(ServerWorld world) {
            return world.getSavedData().getOrCreate(InvasionWorldSavedData::new, DATA_NAME);
        }

        public void setInvasionData(CompoundNBT data) {
            this.invasionData = data;
            this.markDirty();
        }

        public CompoundNBT getInvasionData() {
            return this.invasionData;
        }
    }

    private static void loadConfig() {
        INVASION_INTERVAL = InvasionConfig.getInvasionIntervalDays() * DAY_TICKS;
        INVASION_DURATION = InvasionConfig.getInvasionDurationDays() * DAY_TICKS;
        NIGHT_START_TICKS = InvasionConfig.getNightStartTicks();
    }

    public static void checkEnhancementUnlock(ServerPlayerEntity player) {
        if (!InvasionConfig.isEnabled()) return;

        if (globalState == InvasionState.INACTIVE) {
            globalState = InvasionState.WAITING_FIRST;
            firstUnlockTime = player.world.getGameTime();

            TranslationTextComponent message = new TranslationTextComponent(
                    "enhance.invasion.unlock_message",
                    InvasionConfig.getInvasionIntervalDays()
            );
            player.world.getPlayers().forEach(p -> p.sendMessage(message, p.getUniqueID()));

            ServerWorld world = getServerWorld();
            if (world != null) {
                long dayTime = world.getDayTime() % DAY_TICKS;
                long daysUntilInvasion = InvasionConfig.getInvasionIntervalDays();
                long timeUntilNight;
                if (dayTime < NIGHT_START_TICKS) {
                    timeUntilNight = NIGHT_START_TICKS - dayTime;
                } else {
                    timeUntilNight = DAY_TICKS - dayTime + NIGHT_START_TICKS;
                }
                long totalTicksUntilInvasion = timeUntilNight + (daysUntilInvasion - 1) * DAY_TICKS;
            }
        }

        updatePlayerInvasionData(player);
    }

    private static void updatePlayerInvasionData(ServerPlayerEntity player) {
        UUID playerId = player.getUniqueID();
        InvasionData data = playerInvasionData.getOrDefault(playerId, new InvasionData());

        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(EnhanceCommand.BUFF_TAG)) {
            CompoundNBT buffs = playerData.getCompound(EnhanceCommand.BUFF_TAG);
            data.playerEnhanceLevel = buffs.contains(EnhanceCommand.ENHANCE_LEVEL_TAG)
                    ? buffs.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG) : 0;
        }

        playerInvasionData.put(playerId, data);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!InvasionConfig.isEnabled()) return;

        checkAndStartInvasion();
        processActiveInvasions();
    }

    private static void checkAndStartInvasion() {
        if (globalState == InvasionState.WAITING_FIRST || globalState == InvasionState.COOLDOWN) {
            ServerWorld world = getServerWorld();
            if (world == null) return;

            long currentTime = world.getGameTime();
            long timeSinceLast = currentTime - (globalState == InvasionState.WAITING_FIRST ? firstUnlockTime : lastInvasionTime);

            if (timeSinceLast >= INVASION_INTERVAL) {
                long dayTime = world.getDayTime() % DAY_TICKS;
                if (dayTime >= NIGHT_START_TICKS) {
                    startInvasion(world);
                }
            }
        }
    }

    private static void startInvasion(ServerWorld world) {
        globalState = InvasionState.ACTIVE;
        lastInvasionTime = world.getGameTime();
        invasionCycle++;
        invasionStartTick = world.getGameTime();

        TranslationTextComponent message = new TranslationTextComponent(
                "enhance.invasion.start_message",
                invasionCycle
        );

        world.getPlayers().forEach(player -> {
            player.sendMessage(message, player.getUniqueID());

            if (player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = player;
                updatePlayerInvasionData(serverPlayer);

                InvasionData data = playerInvasionData.get(serverPlayer.getUniqueID());
                if (data != null && data.playerEnhanceLevel > 0) {
                    data.isInInvasion = true;
                    int spawnedCount = spawnInvasionMonstersForPlayer(serverPlayer, world);
                    data.invasionMonsterCount = spawnedCount;
                }
            }
        });
    }

    private static int spawnInvasionMonstersForPlayer(ServerPlayerEntity player, ServerWorld world) {
        InvasionData data = playerInvasionData.get(player.getUniqueID());
        if (data == null || data.playerEnhanceLevel == 0) return 0;

        int monsterCount = InvasionConfig.getMonsterCountByLevel(data.playerEnhanceLevel);
        List<MobEntity> monsters = new ArrayList<>();

        List<String> monsterTypeNames = InvasionConfig.getInvasionMonsterTypes();
        if (monsterTypeNames.isEmpty()) {
            monsterTypeNames.add("minecraft:zombie");
            monsterTypeNames.add("minecraft:skeleton");
        }

        String playerTag = "invasion_" + player.getUniqueID().toString().replace("-", "");
        String playerUUID = player.getUniqueID().toString();

        int spawned = 0;
        for (int i = 0; i < monsterCount; i++) {
            String monsterTypeName = monsterTypeNames.get(world.rand.nextInt(monsterTypeNames.size()));
            EntityType<?> monsterType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(monsterTypeName));
            if (monsterType == null) continue;

            BlockPos playerPos = player.getPosition();
            int radius = 5 + world.rand.nextInt(10);

            int x = playerPos.getX() + (world.rand.nextInt(radius * 2 + 1) - radius);
            int z = playerPos.getZ() + (world.rand.nextInt(radius * 2 + 1) - radius);
            int y = world.getHeight(Heightmap.Type.MOTION_BLOCKING, x, z);
            if (y <= 0) y = playerPos.getY() + 2;

            BlockPos spawnPos = new BlockPos(x, y, z);

            MobEntity monster = (MobEntity) monsterType.create(world);
            if (monster != null) {
                monster.setUniqueId(UUID.randomUUID());

                monster.setLocationAndAngles(
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        world.rand.nextFloat() * 360.0F,
                        0.0F
                );

                applyEnhancementToMonster(monster, player, data.playerEnhanceLevel);
                applyTrackingToMonster(monster, player);

                monster.getPersistentData().putBoolean("enhance_invasion", true);
                monster.getPersistentData().putUniqueId("invasion_target", player.getUniqueID());
                monster.addTag(playerTag);
                monster.addTag("invasion_monster");

                CompoundNBT monsterData = monster.getPersistentData();
                monsterData.putString("invasion_player_tag", playerTag);
                monsterData.putString("invasion_player_uuid", playerUUID);

                world.addEntity(monster);
                monsters.add(monster);
                spawned++;
            }
        }

        activeInvasions.put(player.getUniqueID(), monsters);

        TranslationTextComponent playerMessage = new TranslationTextComponent(
                "enhance.invasion.player_alert",
                spawned
        );
        player.sendMessage(playerMessage, player.getUniqueID());

        return spawned;
    }

    private static BlockPos findSpawnPositionNearPlayer(ServerPlayerEntity player, ServerWorld world) {
        BlockPos playerPos = player.getPosition();
        int minRadius = InvasionConfig.getSpawnRadiusMin();
        int maxRadius = InvasionConfig.getSpawnRadiusMax();

        for (int attempt = 0; attempt < 20; attempt++) {
            int radius = world.rand.nextInt(maxRadius - minRadius + 1) + minRadius;
            int x = playerPos.getX() + (world.rand.nextInt(radius * 2 + 1) - radius);
            int z = playerPos.getZ() + (world.rand.nextInt(radius * 2 + 1) - radius);
            int y = world.getHeight(Heightmap.Type.MOTION_BLOCKING, x, z);

            if (y > 0) {
                BlockPos testPos = new BlockPos(x, y, z);
                if (isSimpleSafeSpawnPosition(world, testPos)) {
                    return testPos;
                }
            }
        }

        return playerPos.up(5);
    }

    private static boolean isSimpleSafeSpawnPosition(ServerWorld world, BlockPos pos) {
        if (!world.isAirBlock(pos) || !world.isAirBlock(pos.up()) || !world.isAirBlock(pos.up(2))) {
            return false;
        }
        if (!world.getBlockState(pos.down()).isSolid()) {
            return false;
        }
        return !world.getBlockState(pos).getMaterial().isLiquid()
                && !world.getBlockState(pos.up()).getMaterial().isLiquid();
    }

    private static void spawnReplacementMonster(ServerPlayerEntity player, ServerWorld world, List<MobEntity> existingMonsters) {
        List<String> monsterTypeNames = InvasionConfig.getInvasionMonsterTypes();
        if (monsterTypeNames.isEmpty()) {
            monsterTypeNames.add("minecraft:zombie");
        }

        String monsterTypeName = monsterTypeNames.get(world.rand.nextInt(monsterTypeNames.size()));
        EntityType<?> monsterType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(monsterTypeName));
        if (monsterType == null) return;

        BlockPos spawnPos = findSpawnPositionNearPlayer(player, world);

        String playerTag = "invasion_" + player.getUniqueID().toString().replace("-", "");
        String playerUUID = player.getUniqueID().toString();

        MobEntity monster = (MobEntity) monsterType.create(world);
        if (monster != null) {
            monster.setLocationAndAngles(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    world.rand.nextFloat() * 360.0F,
                    0.0F
            );

            InvasionData data = playerInvasionData.get(player.getUniqueID());
            if (data != null) {
                applyEnhancementToMonster(monster, player, data.playerEnhanceLevel);
                applyTrackingToMonster(monster, player);

                monster.getPersistentData().putBoolean("enhance_invasion", true);
                monster.getPersistentData().putUniqueId("invasion_target", player.getUniqueID());
                monster.addTag(playerTag);
                monster.addTag("invasion_monster");

                CompoundNBT monsterData = monster.getPersistentData();
                monsterData.putString("invasion_player_tag", playerTag);
                monsterData.putString("invasion_player_uuid", playerUUID);

                world.addEntity(monster);
                existingMonsters.add(monster);
            }
        }
    }

    private static void applyEnhancementToMonster(MobEntity monster, ServerPlayerEntity target, int playerLevel) {
        List<Integer> possibleLevels = InvasionConfig.getMonsterEnhanceLevels(playerLevel);
        if (possibleLevels.isEmpty()) {
            possibleLevels = Collections.singletonList(1);
        }

        int monsterEnhanceLevel = possibleLevels.get(target.world.rand.nextInt(possibleLevels.size()));
        MobEnhancementHandler.applyEnhancement(monster, monsterEnhanceLevel, target.world.getDifficulty(), true);

        CompoundNBT monsterData = monster.getPersistentData();
        if (!monsterData.contains(EnhanceCommand.BUFF_TAG)) {
            monsterData.put(EnhanceCommand.BUFF_TAG, new CompoundNBT());
        }

        CompoundNBT buffs = monsterData.getCompound(EnhanceCommand.BUFF_TAG);
        buffs.putInt("tracking", 3);
        monsterData.put(EnhanceCommand.BUFF_TAG, buffs);

        monsterData.putUniqueId("tracking_target", target.getUniqueID());
    }

    private static void applyTrackingToMonster(MobEntity monster, ServerPlayerEntity target) {
        monster.setAttackTarget(target);

        CompoundNBT data = monster.getPersistentData();
        data.putBoolean("enhance_tracking", true);
        data.putUniqueId("tracking_target", target.getUniqueID());
        data.putLong("tracking_end_time", monster.world.getGameTime() + INVASION_DURATION);
    }

    private static void processActiveInvasions() {
        if (globalState != InvasionState.ACTIVE) return;

        ServerWorld world = getServerWorld();
        if (world == null) return;

        long currentTime = world.getGameTime();

        if (currentTime % 40 == 0) {
            checkAndRestoreTracking(world);
        }

        if (currentTime - lastInvasionTime >= INVASION_DURATION) {
            endInvasion(world, false);
            return;
        }

        boolean infiniteMode = InvasionConfig.isInfiniteMode();

        if (!infiniteMode) {
            if (currentTime - invasionStartTick < INVASION_START_PROTECTION_TICKS) {
            } else {
                boolean hasActiveMonsters = checkGlobalInvasionMonstersAlive();
                if (!hasActiveMonsters) {
                    endInvasion(world, true);
                    return;
                }
            }
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUniqueID();
            List<MobEntity> monsters = activeInvasions.get(playerId);

            if (monsters != null) {
                monsters.removeIf(monster -> monster == null || !monster.isAlive() || monster.removed);

                if (infiniteMode) {
                    InvasionData data = playerInvasionData.get(playerId);
                    if (data != null && data.isInInvasion) {
                        int aliveCount = monsters.size();
                        int maxMonsters = InvasionConfig.getInfiniteMaxMonstersPerPlayer();

                        if (aliveCount < maxMonsters) {
                            int targetCount = Math.min(data.invasionMonsterCount, maxMonsters);

                            if (currentTime - data.lastRespawnTime >= InvasionConfig.getMonsterRespawnInterval()) {
                                int toSpawn = (int) Math.ceil((targetCount - aliveCount) * InvasionConfig.getInfiniteRespawnRate());
                                if (toSpawn > 0) {
                                    for (int i = 0; i < toSpawn; i++) {
                                        spawnReplacementMonster(player, world, monsters);
                                    }
                                    data.lastRespawnTime = currentTime;
                                }
                            }
                        }
                    }
                }

                updateMonsterTracking(player, monsters);
            }
        }
    }

    private static boolean checkGlobalInvasionMonstersAlive() {
        int aliveCount = 0;

        for (List<MobEntity> monsters : activeInvasions.values()) {
            if (monsters != null) {
                for (MobEntity monster : monsters) {
                    if (monster != null && monster.isAlive() && !monster.removed) {
                        aliveCount++;
                    }
                }
            }
        }

        if (aliveCount == 0) {
            boolean hasEmptyData = activeInvasions.isEmpty()
                    || activeInvasions.values().stream().allMatch(list -> list == null || list.isEmpty());
            if (hasEmptyData) {
                return false;
            }
        }

        return aliveCount > 0;
    }

    private static void updateMonsterTracking(ServerPlayerEntity player, List<MobEntity> monsters) {
        for (MobEntity monster : monsters) {
            if (monster.isAlive() && !monster.removed) {
                if (monster.getAttackTarget() == null || monster.getAttackTarget() != player) {
                    monster.setAttackTarget(player);
                }
                if (monster.getDistanceSq(player) < 1024) {
                    monster.getNavigator().tryMoveToEntityLiving(player, 1.2);
                }
            }
        }
    }

    private static void endInvasion(ServerWorld world, boolean isAllMonstersDead) {
        globalState = InvasionState.COOLDOWN;

        int removedCount = 0;
        try {
            String command = "forcekill @e[tag=invasion_monster]";
            world.getServer().getCommandManager().handleCommand(
                    world.getServer().getCommandSource().withFeedbackDisabled().withPermissionLevel(4),
                    command
            );
            removedCount = countInvasionMonstersRemoved(world);
        } catch (Exception e) {
            removedCount = clearAllInvasionMonsters(world);
        }

        for (List<MobEntity> monsters : activeInvasions.values()) {
            if (monsters != null) {
                monsters.clear();
            }
        }
        activeInvasions.clear();

        for (InvasionData data : playerInvasionData.values()) {
            data.isInInvasion = false;
            data.invasionMonsterCount = 0;
            data.lastRespawnTime = 0;
        }

        TranslationTextComponent message;
        if (isAllMonstersDead) {
            message = new TranslationTextComponent(
                    "enhance.invasion.end_defeated",
                    InvasionConfig.getInvasionIntervalDays(),
                    removedCount
            );
        } else {
            message = new TranslationTextComponent(
                    "enhance.invasion.end_timeout",
                    InvasionConfig.getInvasionIntervalDays(),
                    removedCount
            );
        }

        TranslationTextComponent finalMessage = message;
        world.getPlayers().forEach(player -> player.sendMessage(finalMessage, player.getUniqueID()));
    }

    private static int countInvasionMonstersRemoved(ServerWorld world) {
        int count = 0;

        List<LivingEntity> entities = world.getEntitiesWithinAABB(
                LivingEntity.class,
                new AxisAlignedBB(
                        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
                )
        );

        for (LivingEntity entity : entities) {
            if (entity.getTags().contains("invasion_monster")
                    || entity.getPersistentData().getBoolean("enhance_invasion")) {
                count++;
            }
        }

        return count;
    }

    private static int clearAllInvasionMonsters(ServerWorld world) {
        int removedCount = 0;

        List<LivingEntity> entities = world.getEntitiesWithinAABB(
                LivingEntity.class,
                new AxisAlignedBB(
                        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
                )
        );

        for (LivingEntity entity : entities) {
            if (entity.getPersistentData().getBoolean("enhance_invasion")
                    && !entity.getPersistentData().getBoolean("invasion_removed")) {
                try {
                    entity.getPersistentData().putBoolean("invasion_removed", true);
                    InvasionDropHandler.safelyRemoveInvasionMonster(entity);
                    removedCount++;

                    if (InvasionConfig.isDebugMode()) {
                        world.playEvent(2001, entity.getPosition(),
                                net.minecraft.block.Block.getStateId(net.minecraft.block.Blocks.REDSTONE_BLOCK.getDefaultState()));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return removedCount;
    }

    public static void onPlayerLogout(ServerPlayerEntity player) {
        UUID playerId = player.getUniqueID();
        InvasionData data = playerInvasionData.get(playerId);
        if (data != null) data.isInInvasion = false;

        if (globalState == InvasionState.ACTIVE) {
            boolean hasOnlinePlayers = player.world.getPlayers().stream()
                    .anyMatch(p -> p instanceof ServerPlayerEntity && !p.getUniqueID().equals(playerId));

            if (!hasOnlinePlayers && !checkGlobalInvasionMonstersAlive()) {
                endInvasion((ServerWorld) player.world, true);
            }
        }
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        UUID playerId = player.getUniqueID();
        List<MobEntity> monsters = activeInvasions.get(playerId);

        if (monsters != null) {
            for (MobEntity monster : monsters) {
                if (monster != null && monster.isAlive() && !monster.removed) {
                    monster.remove();
                }
            }
            activeInvasions.remove(playerId);
        }

        InvasionData data = playerInvasionData.get(playerId);
        if (data != null) {
            data.isInInvasion = false;
            data.invasionMonsterCount = 0;
            data.lastRespawnTime = 0;
        }

        if (globalState == InvasionState.ACTIVE && !checkGlobalInvasionMonstersAlive()) {
            endInvasion((ServerWorld) player.world, true);
        }
    }

    private static ServerWorld getServerWorld() {
        return net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
    }

    public static void stopInvasionForTesting() {
        ServerWorld world = getServerWorld();
        if (world != null) {
            endInvasion(world, false);
        }
    }
    public static ITextComponent getInvasionStatusComponent() {
        switch (globalState) {
            case INACTIVE:
                return new TranslationTextComponent("enhance.invasion.status.inactive");

            case WAITING_FIRST: {
                ServerWorld world = getServerWorld();
                if (world != null) {
                    long remaining = INVASION_INTERVAL - (world.getGameTime() - firstUnlockTime);
                    if (remaining < 0) remaining = 0;

                    long days = remaining / DAY_TICKS;
                    long hours = (remaining % DAY_TICKS) / 1000;

                    return new TranslationTextComponent(
                            "enhance.invasion.status.waiting_first",
                            days, hours
                    );
                }
                return new TranslationTextComponent("enhance.invasion.status.waiting_first", 0, 0);
            }

            case ACTIVE: {
                ServerWorld activeWorld = getServerWorld();
                if (activeWorld != null) {
                    long remaining = INVASION_DURATION - (activeWorld.getGameTime() - lastInvasionTime);
                    if (remaining < 0) remaining = 0;

                    long seconds = remaining / 20;

                    int aliveCount = 0;
                    for (List<MobEntity> monsters : activeInvasions.values()) {
                        if (monsters != null) {
                            for (MobEntity monster : monsters) {
                                if (monster != null && monster.isAlive() && !monster.removed) {
                                    aliveCount++;
                                }
                            }
                        }
                    }

                    return new TranslationTextComponent(
                            "enhance.invasion.status.active",
                            seconds / 60, seconds % 60, aliveCount
                    );
                }
                return new TranslationTextComponent("enhance.invasion.status.active", 0, 0, 0);
            }

            case COOLDOWN: {
                ServerWorld cooldownWorld = getServerWorld();
                if (cooldownWorld != null) {
                    long remaining = INVASION_INTERVAL - (cooldownWorld.getGameTime() - lastInvasionTime);
                    if (remaining < 0) remaining = 0;

                    long days = remaining / DAY_TICKS;
                    long hours = (remaining % DAY_TICKS) / 1000;

                    return new TranslationTextComponent(
                            "enhance.invasion.status.cooldown",
                            days, hours
                    );
                }
                return new TranslationTextComponent("enhance.invasion.status.cooldown", 0, 0);
            }

            default:
                return new TranslationTextComponent("enhance.invasion.status.unknown");
        }
    }

    public static void resetInvasionCycle() {
        globalState = InvasionState.INACTIVE;
        firstUnlockTime = 0;
        lastInvasionTime = 0;
        invasionCycle = 0;
        playerInvasionData.clear();
        activeInvasions.clear();
    }

    public static void spawnTestMonsters(ServerPlayerEntity player, int count) {
        ServerWorld world = (ServerWorld) player.world;

        List<String> monsterTypes = InvasionConfig.getInvasionMonsterTypes();
        if (monsterTypes.isEmpty()) {
            monsterTypes.add("minecraft:zombie");
            monsterTypes.add("minecraft:skeleton");
            TranslationTextComponent message = new TranslationTextComponent("enhance.invasion.test_default_monsters");
            player.sendMessage(message, player.getUniqueID());
        }

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            String monsterTypeName = monsterTypes.get(world.rand.nextInt(monsterTypes.size()));
            EntityType<?> monsterType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(monsterTypeName));
            if (monsterType == null) continue;

            BlockPos playerPos = player.getPosition();
            int x = playerPos.getX() + (world.rand.nextInt(21) - 10);
            int z = playerPos.getZ() + (world.rand.nextInt(21) - 10);
            int y = world.getHeight(Heightmap.Type.WORLD_SURFACE, x, z);
            BlockPos spawnPos = new BlockPos(x, y, z);

            MobEntity monster = (MobEntity) monsterType.create(world);
            if (monster != null) {
                monster.setLocationAndAngles(
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        world.rand.nextFloat() * 360.0F,
                        0.0F
                );
                monster.getPersistentData().putBoolean("test_monster", true);
                world.addEntity(monster);
                spawned++;
                monster.setAttackTarget(player);
            }
        }

        TranslationTextComponent message = new TranslationTextComponent("enhance.invasion.test_spawned", spawned);
        player.sendMessage(message, player.getUniqueID());
    }

    public static void forceStartInvasion(ServerWorld world) {
        if (!InvasionConfig.isEnabled()) {
            InvasionConfig.setEnabled(true);
        }

        startInvasion(world);

        TranslationTextComponent message = new TranslationTextComponent("enhance.invasion.admin_force_start");
        world.getPlayers().forEach(player -> player.sendMessage(message, player.getUniqueID()));
    }

    private static void reapplyMonsterTracking(MobEntity monster, ServerPlayerEntity targetPlayer) {
        monster.setAttackTarget(targetPlayer);

        CompoundNBT data = monster.getPersistentData();
        data.putBoolean("enhance_tracking", true);
        data.putUniqueId("tracking_target", targetPlayer.getUniqueID());

        ServerWorld world = (ServerWorld) monster.world;
        long remainingTime = calculateRemainingInvasionTime(world);
        if (remainingTime > 0) {
            data.putLong("tracking_end_time", world.getGameTime() + remainingTime);
        } else {
            data.remove("enhance_tracking");
            data.remove("tracking_target");
            data.remove("tracking_end_time");
        }

        if (monster instanceof net.minecraft.entity.monster.ZombieEntity
                || monster instanceof net.minecraft.entity.monster.SkeletonEntity) {
            monster.setRevengeTarget(targetPlayer);
            monster.setAttackTarget(targetPlayer);
        }
    }

    private static long calculateRemainingInvasionTime(ServerWorld world) {
        if (globalState != InvasionState.ACTIVE) return 0;

        long currentTime = world.getGameTime();
        long elapsed = currentTime - lastInvasionTime;
        return Math.max(0, INVASION_DURATION - elapsed);
    }

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            UUID playerId = player.getUniqueID();

            List<MobEntity> monsters = activeInvasions.get(playerId);
            if (monsters != null && !monsters.isEmpty()) {
                monsters.removeIf(monster -> monster == null || !monster.isAlive() || monster.removed);

                ServerWorld world = (ServerWorld) player.world;
                for (MobEntity monster : monsters) {
                    if (monster != null && monster.isAlive()) {
                        reapplyMonsterTracking(monster, player);
                    }
                }
            }

            updatePlayerInvasionData(player);

            Objects.requireNonNull(player.world.getServer()).deferTask(() -> {
                ServerWorld world = (ServerWorld) player.world;
                restoreTrackingForLoadedMonsters(world);
            });
        }
    }

    private static void checkAndRestoreTracking(ServerWorld world) {
        for (Map.Entry<UUID, List<MobEntity>> entry : activeInvasions.entrySet()) {
            UUID playerId = entry.getKey();
            List<MobEntity> monsters = entry.getValue();
            if (monsters == null || monsters.isEmpty()) continue;

            ServerPlayerEntity targetPlayer = (ServerPlayerEntity) world.getPlayerByUuid(playerId);
            if (targetPlayer == null) continue;

            for (MobEntity monster : monsters) {
                if (monster != null && monster.isAlive() && !monster.removed) {
                    CompoundNBT data = monster.getPersistentData();

                    if (data.getBoolean("needs_tracking_restore")) {
                        reapplyMonsterTracking(monster, targetPlayer);
                        data.remove("needs_tracking_restore");
                    } else if (data.getBoolean("enhance_tracking")) {
                        UUID storedTargetId = data.getUniqueId("tracking_target");
                        if (!targetPlayer.getUniqueID().equals(storedTargetId)) {
                            reapplyMonsterTracking(monster, targetPlayer);
                        }
                    } else if (data.getBoolean("enhance_invasion")) {
                        reapplyMonsterTracking(monster, targetPlayer);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        if (event.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) event.getWorld();
            if (serverWorld.getDimensionKey() == World.OVERWORLD) {
                saveInvasionData(serverWorld);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) event.getWorld();
            if (serverWorld.getDimensionKey() == World.OVERWORLD) {
                serverWorld.getServer().deferTask(() -> loadInvasionData(serverWorld));
            }
        }
    }

    private static void saveInvasionData(ServerWorld world) {
        if (globalState == InvasionState.INACTIVE && playerInvasionData.isEmpty()) return;

        InvasionWorldSavedData savedData = InvasionWorldSavedData.get(world);
        CompoundNBT invasionData = new CompoundNBT();

        try {
            invasionData.putString("GlobalState", globalState.name());
            invasionData.putLong("FirstUnlockTime", firstUnlockTime);
            invasionData.putLong("LastInvasionTime", lastInvasionTime);
            invasionData.putInt("InvasionCycle", invasionCycle);
            invasionData.putLong("InvasionStartTick", invasionStartTick);
            invasionData.putLong("CurrentTimeAtSave", world.getGameTime());

            ListNBT playerDataList = new ListNBT();
            for (Map.Entry<UUID, InvasionData> entry : playerInvasionData.entrySet()) {
                UUID playerId = entry.getKey();
                InvasionData data = entry.getValue();

                CompoundNBT playerTag = new CompoundNBT();
                playerTag.putUniqueId(PLAYER_UUID_TAG, playerId);
                playerTag.put(PLAYER_INVASION_DATA_TAG, data.toNBT());
                playerDataList.add(playerTag);
            }
            invasionData.put(PLAYER_INVASION_DATA_TAG, playerDataList);

            ListNBT monstersList = new ListNBT();
            if (globalState == InvasionState.ACTIVE) {
                for (Map.Entry<UUID, List<MobEntity>> entry : activeInvasions.entrySet()) {
                    UUID targetPlayerId = entry.getKey();
                    List<MobEntity> monsters = entry.getValue();
                    if (monsters == null) continue;

                    for (MobEntity monster : monsters) {
                        if (monster != null && monster.isAlive() && !monster.removed) {
                            CompoundNBT monsterTag = new CompoundNBT();

                            ResourceLocation monsterType = ForgeRegistries.ENTITIES.getKey(monster.getType());
                            if (monsterType != null) {
                                monsterTag.putString(MONSTER_TYPE_TAG, monsterType.toString());
                            }

                            monsterTag.putDouble(MONSTER_POS_X_TAG, monster.getPosX());
                            monsterTag.putDouble(MONSTER_POS_Y_TAG, monster.getPosY());
                            monsterTag.putDouble(MONSTER_POS_Z_TAG, monster.getPosZ());
                            monsterTag.putFloat(MONSTER_ROT_Y_TAG, monster.rotationYaw);

                            CompoundNBT monsterNBT = new CompoundNBT();
                            monster.writeWithoutTypeId(monsterNBT);
                            monsterTag.put(MONSTER_NBT_TAG, monsterNBT);

                            monsterTag.putUniqueId(TARGET_PLAYER_UUID_TAG, targetPlayerId);
                            monstersList.add(monsterTag);
                        }
                    }
                }
            }

            invasionData.put(MONSTERS_TAG, monstersList);

            savedData.setInvasionData(invasionData);
            savedData.markDirty();
        } catch (Exception ignored) {
        }
    }

    private static void loadInvasionData(ServerWorld world) {
        try {
            InvasionWorldSavedData savedData = InvasionWorldSavedData.get(world);
            CompoundNBT invasionData = savedData.getInvasionData();

            if (invasionData.isEmpty()) {
                activeInvasions.clear();
                return;
            }

            String stateStr = invasionData.getString("GlobalState");
            if (stateStr.isEmpty()) {
                globalState = InvasionState.INACTIVE;
            } else {
                try {
                    globalState = InvasionState.valueOf(stateStr);
                } catch (IllegalArgumentException e) {
                    globalState = InvasionState.INACTIVE;
                }
            }

            firstUnlockTime = invasionData.getLong("FirstUnlockTime");
            lastInvasionTime = invasionData.getLong("LastInvasionTime");
            invasionCycle = invasionData.getInt("InvasionCycle");
            invasionStartTick = invasionData.getLong("InvasionStartTick");

            playerInvasionData.clear();
            if (invasionData.contains(PLAYER_INVASION_DATA_TAG)) {
                ListNBT playerDataList = invasionData.getList(PLAYER_INVASION_DATA_TAG, Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < playerDataList.size(); i++) {
                    CompoundNBT playerTag = playerDataList.getCompound(i);
                    UUID playerId = playerTag.getUniqueId(PLAYER_UUID_TAG);
                    CompoundNBT dataNBT = playerTag.getCompound(PLAYER_INVASION_DATA_TAG);
                    InvasionData data = InvasionData.fromNBT(dataNBT);
                    playerInvasionData.put(playerId, data);
                }
            }

            activeInvasions.clear();
            if (globalState == InvasionState.ACTIVE && invasionData.contains(MONSTERS_TAG)) {
                Map<UUID, List<MobEntity>> loadedMonsters = new ConcurrentHashMap<>();

                ListNBT monstersList = invasionData.getList(MONSTERS_TAG, Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < monstersList.size(); i++) {
                    try {
                        CompoundNBT monsterTag = monstersList.getCompound(i);

                        UUID targetPlayerId = monsterTag.getUniqueId(TARGET_PLAYER_UUID_TAG);
                        String monsterTypeName = monsterTag.getString(MONSTER_TYPE_TAG);

                        EntityType<?> monsterType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(monsterTypeName));
                        if (monsterType == null) continue;

                        double posX = monsterTag.getDouble(MONSTER_POS_X_TAG);
                        double posY = monsterTag.getDouble(MONSTER_POS_Y_TAG);
                        double posZ = monsterTag.getDouble(MONSTER_POS_Z_TAG);
                        float rotY = monsterTag.getFloat(MONSTER_ROT_Y_TAG);

                        CompoundNBT monsterNBT = monsterTag.getCompound(MONSTER_NBT_TAG);

                        MobEntity monster = (MobEntity) monsterType.create(world);
                        if (monster != null) {
                            monster.setLocationAndAngles(posX, posY, posZ, rotY, 0.0F);

                            UUID newUUID = UUID.randomUUID();
                            monster.setUniqueId(newUUID);

                            if (monsterNBT.contains("UUIDMost") && monsterNBT.contains("UUIDLeast")) {
                                monsterNBT.putLong("UUIDMost", newUUID.getMostSignificantBits());
                                monsterNBT.putLong("UUIDLeast", newUUID.getLeastSignificantBits());
                            }

                            monster.read(monsterNBT);
                            world.addEntity(monster);

                            loadedMonsters.computeIfAbsent(targetPlayerId, k -> new ArrayList<>()).add(monster);
                        }
                    } catch (Exception ignored) {
                    }
                }

                activeInvasions.putAll(loadedMonsters);
            }

            if (globalState == InvasionState.ACTIVE) {
                long currentTime = world.getGameTime();
                long elapsed = currentTime - lastInvasionTime;
                if (elapsed >= INVASION_DURATION) {
                    endInvasion(world, false);
                }
            }

        } catch (Exception e) {
            globalState = InvasionState.INACTIVE;
            activeInvasions.clear();
            playerInvasionData.clear();
        }
    }

    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.fml.event.server.FMLServerStartingEvent event) {
        ServerWorld world = event.getServer().getWorld(World.OVERWORLD);
        if (world != null) {
            event.getServer().deferTask(() -> loadInvasionData(world));
        }
    }

    private static void restoreTrackingForLoadedMonsters(ServerWorld world) {
        for (Map.Entry<UUID, List<MobEntity>> entry : activeInvasions.entrySet()) {
            UUID playerId = entry.getKey();
            List<MobEntity> monsters = entry.getValue();
            if (monsters == null || monsters.isEmpty()) continue;

            ServerPlayerEntity targetPlayer = world.getServer().getPlayerList().getPlayerByUUID(playerId);

            for (MobEntity monster : monsters) {
                if (monster == null || !monster.isAlive() || monster.removed) continue;

                try {
                    if (targetPlayer != null && targetPlayer.isAlive()) {
                        reapplyMonsterTracking(monster, targetPlayer);
                        monster.getPersistentData().remove("needs_tracking_restore");
                    } else {
                        monster.getPersistentData().putBoolean("needs_tracking_restore", true);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static int clearPlayerInvasionMonsters(ServerPlayerEntity player) {
        if (player == null) return 0;

        int removedCount = 0;
        List<MobEntity> monsters = activeInvasions.get(player.getUniqueID());

        if (monsters != null) {
            for (MobEntity monster : monsters) {
                if (monster != null && monster.isAlive() && !monster.removed) {
                    InvasionDropHandler.safelyRemoveInvasionMonster(monster);
                    removedCount++;
                }
            }
            activeInvasions.remove(player.getUniqueID());
        }

        InvasionData data = playerInvasionData.get(player.getUniqueID());
        if (data != null) {
            data.isInInvasion = false;
            data.invasionMonsterCount = 0;
        }

        return removedCount;
    }
}
