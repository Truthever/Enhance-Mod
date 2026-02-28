package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.network.BossDataPacket;
import com.weaponhouse.enhance.network.RemoveBossDataPacket;
import com.weaponhouse.enhance.network.SpiritShieldPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import java.util.*;
@Mod.EventBusSubscriber(modid = "enhance")
public class BossBarHandler {
    private static final Map<UUID, ServerBossInfo> bossBars = new HashMap<>();
    private static final Map<UUID, BossInfoData> bossData = new HashMap<>();
    private static final Map<UUID, Map<UUID, Double>> playerBossDistances = new HashMap<>();
    private static final double RANGE_XZ = 16.0;
    private static final double RANGE_Y = 5.0;
    private static final double SUMMONED_RANGE_XZ = 8.0;
    private static final double SUMMONED_RANGE_Y = 3.0;
    private static final Map<String, Integer> MOD_PRIORITY = new HashMap<>();
    private static boolean hasRestoredExistingMobs = false;
    static {
        MOD_PRIORITY.put("combo", 25);
        MOD_PRIORITY.put("corrosion", 24);
        MOD_PRIORITY.put("annihilation", 23);
        MOD_PRIORITY.put("inspiration", 22);
        MOD_PRIORITY.put("chaos", 21);
        MOD_PRIORITY.put("megaforce", 20);
        MOD_PRIORITY.put("unyielding", 19);
        MOD_PRIORITY.put("summon", 18);
        MOD_PRIORITY.put("death_bomb", 17);
        MOD_PRIORITY.put("vampire", 16);
        MOD_PRIORITY.put("thorns", 15);
        MOD_PRIORITY.put("thunder", 14);
        MOD_PRIORITY.put("frost", 13);
        MOD_PRIORITY.put("curse", 12);
        MOD_PRIORITY.put("aura", 11);
        MOD_PRIORITY.put("tracking", 10);
        MOD_PRIORITY.put("ricochet", 9);
        MOD_PRIORITY.put("displacement", 8);
        MOD_PRIORITY.put("phantom", 7);
        MOD_PRIORITY.put("rob", 6);
        MOD_PRIORITY.put("harmony", 5);
        MOD_PRIORITY.put("attack", 4);
        MOD_PRIORITY.put("life", 3);
        MOD_PRIORITY.put("hunger", 2);
        MOD_PRIORITY.put("photosynthesis", 1);
        MOD_PRIORITY.put("fasting", 0);
        MOD_PRIORITY.put("spirit_shield", 0);
    }
    private static final double DRAGON_WITHER_RANGE_XZ = 64.0;
    private static final double DRAGON_WITHER_RANGE_Y  = 64.0;
    private static boolean isDragonOrWither(LivingEntity entity) {
        return entity.getType() == net.minecraft.entity.EntityType.ENDER_DRAGON
                || entity.getType() == net.minecraft.entity.EntityType.WITHER;
    }
    private static double getRangeXZ(LivingEntity entity) {
        if (isDragonOrWither(entity)) return DRAGON_WITHER_RANGE_XZ;
        return isSummonedMob(entity) ? SUMMONED_RANGE_XZ : RANGE_XZ;
    }
    private static double getRangeY(LivingEntity entity) {
        if (isDragonOrWither(entity)) return DRAGON_WITHER_RANGE_Y;
        return isSummonedMob(entity) ? SUMMONED_RANGE_Y : RANGE_Y;
    }
    private static boolean hasAnyRealBuff(LivingEntity entity) {
        CompoundNBT data = entity.getPersistentData();
        if (!data.contains("WeaponHouseBuffs")) return false;

        CompoundNBT buffs = data.getCompound("WeaponHouseBuffs");
        for (String key : buffs.keySet()) {
            if (isEnhancementKey(key)) continue;
            if (buffs.getInt(key) > 0) return true;
        }

        if (data.contains("InspirationMarker")) {
            CompoundNBT marker = data.getCompound("InspirationMarker");
            return marker.getBoolean("active");
        }
        return false;
    }
    private static boolean isSummonedMob(LivingEntity entity) {
        return entity.getTags().contains("summoned_by_buff");
    }
    private static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity;
    }
    private static boolean isInPlayerDimension(Entity entity, ServerPlayerEntity player) {
        if (entity == null || player == null) return false;
        return entity.world.getDimensionKey().equals(player.world.getDimensionKey());
    }
    @SubscribeEvent
    public static void onWorldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
        if (!(event.getWorld() instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) event.getWorld();
        if (world.getDimensionKey() != World.OVERWORLD) return;
        if (hasRestoredExistingMobs) return;
        hasRestoredExistingMobs = true;
        world.getServer().deferTask(() -> restoreBossBarsForExistingMobs(world));
    }
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.fml.event.server.FMLServerStartedEvent event) {
        for (ServerPlayerEntity player : event.getServer().getPlayerList().getPlayers()) {
            loadSpiritShieldData(player);
        }
    }
    public static void loadSpiritShieldData(ServerPlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains("SpiritShieldData")) {
            CompoundNBT spiritShieldData = playerData.getCompound("SpiritShieldData");
            float current = spiritShieldData.getFloat("currentShield");
            float max = spiritShieldData.getFloat("maxShield");
            Enhance.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SpiritShieldPacket(current, max)
            );
        } else {
            if (hasSpiritShieldBuff(player)) {
                SpiritShieldHandler.initializeSpiritShield(player);
            }
        }
    }
    private static boolean hasSpiritShieldBuff(ServerPlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        return playerData.contains("WeaponHouseBuffs") &&
                playerData.getCompound("WeaponHouseBuffs").contains("spirit_shield") &&
                playerData.getCompound("WeaponHouseBuffs").getInt("spirit_shield") > 0;
    }
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            loadSpiritShieldData(player);
        }
    }
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            player.getServerWorld().getServer().deferTask(() -> loadSpiritShieldData(player));
        }
    }
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            UUID playerId = player.getUniqueID();
            clearPlayerBossData(player);
            loadSpiritShieldData(player);
            player.getServerWorld().getServer().deferTask(() -> {
                playerBossDistances.remove(playerId);
                for (UUID bossId : new ArrayList<>(bossBars.keySet())) {
                    Entity entity = findEntityAcrossDimensions(bossId, player.server);
                    if (isInPlayerDimension(entity, player) && !isPlayer(entity)) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        double rangeXZ = getRangeXZ(livingEntity);
                        double rangeY = getRangeY(livingEntity);
                        double dx = Math.abs(player.getPosX() - livingEntity.getPosX());
                        double dy = Math.abs(player.getPosY() - livingEntity.getPosY());
                        double dz = Math.abs(player.getPosZ() - livingEntity.getPosZ());
                        if (dx <= rangeXZ && dy <= rangeY && dz <= rangeXZ) {
                            double distance = player.getDistance(livingEntity);
                            if (distance > 0.0) {
                                sendBossDataToPlayer(bossId, player);
                            }
                        }
                    }
                }
            });
        }
    }
    private static void clearPlayerBossData(ServerPlayerEntity player) {
        UUID playerId = player.getUniqueID();
        for (UUID bossId : new ArrayList<>(bossBars.keySet())) {
            ServerBossInfo bossInfo = bossBars.get(bossId);
            if (bossInfo != null && bossInfo.getPlayers().contains(player)) {
                bossInfo.removePlayer(player);
            }
            sendRemoveBossDataToPlayer(bossId, player);
        }
        playerBossDistances.remove(playerId);
    }
    @SubscribeEvent
    public static void onServerAboutToStart(net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent event) {
        hasRestoredExistingMobs = false;
        bossBars.clear();
        bossData.clear();
        playerBossDistances.clear();
    }
    private static void restoreBossBarsForExistingMobs(ServerWorld world) {
        for (LivingEntity entity : world.getEntitiesWithinAABB(LivingEntity.class,
                new AxisAlignedBB(-1000, -1000, -1000, 1000, 1000, 1000))) {
            if (!entity.isAlive()) continue;
            if (isSummonedMob(entity)) continue;
            if (isPlayer(entity)) continue;
            boolean isEnhanced = entity.getPersistentData().getBoolean("WH_Enhanced") ||
                    entity.getTags().contains("one_enhance") ||
                    entity.getTags().contains("two_enhance") ||
                    entity.getTags().contains("three_enhance");
            if (isEnhanced) {
                int tier = getEnhancementTier(entity);
                if (tier > 0) {
                    createBossBar(entity, tier);
                }
            }
        }
    }
    private static int getEnhancementTier(LivingEntity entity) {
        if (entity.getTags().contains("three_enhance")) return 3;
        if (entity.getTags().contains("two_enhance")) return 2;
        if (entity.getTags().contains("one_enhance")) return 1;
        return 0;
    }
    public static void createBossBar(LivingEntity entity, int tier) {
        if (!(entity.world instanceof ServerWorld)) return;
        if (isPlayer(entity)) return;

        if (isSummonedMob(entity)) {
            createSummonedMobBossBar(entity, tier);
        } else {
            createNormalBossBar(entity, tier);
        }
    }
    private static void createSummonedMobBossBar(LivingEntity entity, int tier) {
        UUID entityId = entity.getUniqueID();
        removeBossBar(entityId);
        List<String> modNames = getModNames(entity);
        String entityType = getEntityType(entity);
        BossInfoData data = new BossInfoData(modNames, tier, entityType, entity.getHealth(), entity.getMaxHealth(), 0.0, true);
        bossData.put(entityId, data);
        ServerBossInfo bossInfo = new ServerBossInfo(
                new StringTextComponent(""),
                getBossBarColor(tier),
                BossInfo.Overlay.PROGRESS
        );
        bossInfo.setVisible(false);
        bossInfo.setPercent(entity.getHealth() / entity.getMaxHealth());
        bossBars.put(entityId, bossInfo);
        sendBossDataToAllVisiblePlayers(entityId);
    }
    private static void createNormalBossBar(LivingEntity entity, int tier) {
        UUID entityId = entity.getUniqueID();
        removeBossBar(entityId);
        List<String> modNames = getModNames(entity);
        String entityType = getEntityType(entity);
        BossInfoData data = new BossInfoData(modNames, tier, entityType, entity.getHealth(), entity.getMaxHealth(), 0.0, false);
        bossData.put(entityId, data);
        ServerBossInfo bossInfo = new ServerBossInfo(
                new StringTextComponent(""),
                getBossBarColor(tier),
                BossInfo.Overlay.PROGRESS
        );
        bossInfo.setVisible(false);
        bossInfo.setPercent(entity.getHealth() / entity.getMaxHealth());
        bossBars.put(entityId, bossInfo);
        sendBossDataToAllVisiblePlayers(entityId);
    }
    private static void sendBossDataToAllVisiblePlayers(UUID entityId) {
        ServerBossInfo bossInfo = bossBars.get(entityId);
        BossInfoData data = bossData.get(entityId);
        if (bossInfo == null || data == null) return;
        Entity entity = findEntityAcrossDimensions(entityId, ServerLifecycleHooks.getCurrentServer());
        if (entity == null) return;
        if (isPlayer(entity)) return;
        for (ServerPlayerEntity player : new ArrayList<>(bossInfo.getPlayers())) {
            if (isInPlayerDimension(entity, player)) {
                double distance = getPlayerBossDistance(player.getUniqueID(), entityId);
                if (distance == 0.0) {
                    bossInfo.removePlayer(player);
                    sendRemoveBossDataToPlayer(entityId, player);
                    continue;
                }
                Enhance.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new BossDataPacket(entityId, data.modNames, data.tier, data.entityType, data.currentHealth, data.maxHealth, distance)
                );
            } else {
                bossInfo.removePlayer(player);
                sendRemoveBossDataToPlayer(entityId, player);
            }
        }
    }
    private static String getEntityType(LivingEntity entity) {
        ResourceLocation key = entity.getType().getRegistryName();
        if (key != null) {
            return "entity." + key.getNamespace() + "." + key.getPath();
        }
        return entity.getType().getName().getString();
    }
    public static void createOrUpdateBossBar(LivingEntity entity) {
        if (!(entity.world instanceof ServerWorld)) return;
        if (isPlayer(entity)) return;
        int tier = getEnhancementTier(entity);
        List<String> modNames = getModNames(entity);
        if (modNames.isEmpty() && !hasAnyRealBuff(entity)) {
            removeBossBar(entity.getUniqueID());
            return;
        }
        if (modNames.isEmpty()) {
            modNames = Collections.singletonList("enhanced:0");
        }
        UUID entityId = entity.getUniqueID();
        boolean isSummoned = isSummonedMob(entity);
        String entityType = getEntityType(entity);
        BossInfoData data = new BossInfoData(modNames, tier, entityType,
                entity.getHealth(), entity.getMaxHealth(),
                0.0, isSummoned);
        bossData.put(entityId, data);
        ServerBossInfo bossInfo = bossBars.get(entityId);
        if (bossInfo == null) {
            bossInfo = new ServerBossInfo(
                    new StringTextComponent(""),
                    getBossBarColor(tier),
                    BossInfo.Overlay.PROGRESS
            );
            bossInfo.setVisible(false);
            bossBars.put(entityId, bossInfo);
        }
        bossInfo.setPercent(entity.getHealth() / entity.getMaxHealth());
        sendBossDataToAllVisiblePlayers(entityId);
    }
    private static List<String> getModNames(LivingEntity entity) {
        List<String> modNames = new ArrayList<>();
        CompoundNBT entityData = entity.getPersistentData();
        boolean hasActiveInspiration = false;
        String inspirationBuffName = null;
        int inspirationBuffLevel = 0;
        if (entityData.contains("InspirationMarker")) {
            CompoundNBT marker = entityData.getCompound("InspirationMarker");
            if (marker.getBoolean("active")) {
                hasActiveInspiration = true;
                inspirationBuffName = marker.getString("buffName");
                inspirationBuffLevel = marker.getInt("buffLevel");
            }
        }
        if (entityData.contains("WeaponHouseBuffs")) {
            CompoundNBT buffsTag = entityData.getCompound("WeaponHouseBuffs");
            List<Map.Entry<String, Integer>> sortedMods = new ArrayList<>();
            for (String key : buffsTag.keySet()) {
                if (isEnhancementKey(key)) continue;
                int level = buffsTag.getInt(key);
                if (level > 0) {
                    if (hasActiveInspiration && key.equals(inspirationBuffName)) {
                        continue;
                    }
                    sortedMods.add(new AbstractMap.SimpleEntry<>(key, level));
                }
            }
            sortedMods.sort((a, b) -> {
                int priorityA = MOD_PRIORITY.getOrDefault(a.getKey(), 0);
                int priorityB = MOD_PRIORITY.getOrDefault(b.getKey(), 0);
                return Integer.compare(priorityB, priorityA);
            });
            for (Map.Entry<String, Integer> entry : sortedMods) {
                String key = entry.getKey();
                int level = entry.getValue();
                if ("inspiration".equals(key)) {
                    continue;
                }
                modNames.add(key + ":" + level);
            }
        }
        if (hasActiveInspiration) {
            modNames.add(inspirationBuffName + ":" + inspirationBuffLevel + "(灵感)");
        }
        if (modNames.isEmpty()) {
            if (entity.getTags().contains("three_enhance")) {
                modNames.add("enhanced:3");
            } else if (entity.getTags().contains("two_enhance")) {
                modNames.add("enhanced:2");
            } else if (entity.getTags().contains("one_enhance")) {
                modNames.add("enhanced:1");
            }
        }
        return modNames;
    }
    public static void sendBossDataToPlayer(UUID entityId, ServerPlayerEntity player) {
        ServerBossInfo bossInfo = bossBars.get(entityId);
        BossInfoData data = bossData.get(entityId);
        if (bossInfo == null || data == null) return;
        Entity entity = findEntityAcrossDimensions(entityId, player.server);
        if (isPlayer(entity)) return;
        double distance = getPlayerBossDistance(player.getUniqueID(), entityId);
        if (distance == 0.0) {
            bossInfo.removePlayer(player);
            sendRemoveBossDataToPlayer(entityId, player);
            return;
        }
        Enhance.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BossDataPacket(entityId, data.modNames, data.tier, data.entityType, data.currentHealth, data.maxHealth, distance)
        );
    }
    private static double getPlayerBossDistance(UUID playerId, UUID bossId) {
        return playerBossDistances.getOrDefault(playerId, Collections.emptyMap()).getOrDefault(bossId, 0.0);
    }
    private static void setPlayerBossDistance(UUID playerId, UUID bossId, double distance) {
        playerBossDistances.computeIfAbsent(playerId, k -> new HashMap<>()).put(bossId, distance);
    }
    private static boolean isEnhancementKey(String key) {
        return key.equals("one_enhance") || key.equals("two_enhance") || key.equals("three_enhance");
    }
    private static BossInfo.Color getBossBarColor(int tier) {
        switch (tier) {
            case 1: return BossInfo.Color.GREEN;
            case 2: return BossInfo.Color.BLUE;
            case 3: return BossInfo.Color.RED;
            default: return BossInfo.Color.PURPLE;
        }
    }
    public static void removeBossBar(UUID entityId) {
        synchronized (bossBars) {
            ServerBossInfo bossInfo = bossBars.remove(entityId);
            if (bossInfo != null) {
                bossInfo.setVisible(false);
                bossInfo.removeAllPlayers();
            }
        }
        synchronized (bossData) {
            bossData.remove(entityId);
        }
        synchronized (playerBossDistances) {
            for (Map<UUID, Double> playerDistances : playerBossDistances.values()) {
                playerDistances.remove(entityId);
            }
        }
        sendRemoveBossDataToPlayers(entityId);
    }
    private static void sendRemoveBossDataToPlayers(UUID entityId) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                Enhance.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new RemoveBossDataPacket(entityId)
                );
            }
        }
    }
    private static void sendRemoveBossDataToPlayer(UUID entityId, ServerPlayerEntity player) {
        Enhance.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RemoveBossDataPacket(entityId)
        );
    }
    public static void updateBossBarHealth(LivingEntity entity) {
        if (isPlayer(entity)) return;

        UUID entityId = entity.getUniqueID();
        ServerBossInfo bossInfo = bossBars.get(entityId);
        if (bossInfo != null) {
            float healthPercent = MathHelper.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
            bossInfo.setPercent(healthPercent);
            BossInfoData data = bossData.get(entityId);
            if (data != null) {
                data.currentHealth = entity.getHealth();
                data.maxHealth = entity.getMaxHealth();
                sendBossDataToAllVisiblePlayers(entityId);
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        UUID playerId = player.getUniqueID();
        if (player.ticksExisted % 5 != 0) return;
        try {
            List<UUID> bossIdsToProcess = new ArrayList<>(bossBars.keySet());
            Map<UUID, ServerBossInfo> barsToShow = new HashMap<>(3);
            List<Map.Entry<LivingEntity, Double>> nearbyBosses = new ArrayList<>();
            for (UUID bossId : bossIdsToProcess) {
                Entity entity = findEntityAcrossDimensions(bossId, player.server);
                if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
                    removeBossBarFromPlayer(bossId, player);
                    continue;
                }
                LivingEntity livingEntity = (LivingEntity) entity;
                if (isPlayer(livingEntity)) continue;
                if (!isInPlayerDimension(livingEntity, player)) {
                    removeBossBarFromPlayer(bossId, player);
                    continue;
                }
                double rangeXZ = getRangeXZ(livingEntity);
                double rangeY = getRangeY(livingEntity);
                double dx = Math.abs(player.getPosX() - livingEntity.getPosX());
                double dy = Math.abs(player.getPosY() - livingEntity.getPosY());
                double dz = Math.abs(player.getPosZ() - livingEntity.getPosZ());
                if (dx <= rangeXZ && dy <= rangeY && dz <= rangeXZ) {
                    double distance = player.getDistance(livingEntity);
                    if (distance == 0.0) {
                        removeBossBarFromPlayer(bossId, player);
                        continue;
                    }
                    nearbyBosses.add(new AbstractMap.SimpleEntry<>(livingEntity, distance));
                    setPlayerBossDistance(playerId, bossId, distance);
                    BossInfoData data = bossData.get(bossId);
                    if (data != null) {
                        data.currentHealth = livingEntity.getHealth();
                        data.maxHealth = livingEntity.getMaxHealth();
                        data.distance = distance;
                        sendBossDataToPlayer(bossId, player);
                    }
                } else {
                    removeBossBarFromPlayer(bossId, player);
                }
            }
            List<UUID> entitiesToRemove = new ArrayList<>();
            for (UUID bossId : bossIdsToProcess) {
                Entity entity = findEntityAcrossDimensions(bossId, player.server);
                if (entity == null || !entity.isAlive()) {
                    entitiesToRemove.add(bossId);
                }
            }
            for (UUID entityId : entitiesToRemove) {
                removeBossBar(entityId);
            }
            nearbyBosses.sort(Comparator.comparingDouble(Map.Entry::getValue));
            int count = Math.min(nearbyBosses.size(), 3);
            for (int i = 0; i < count; i++) {
                LivingEntity entity = nearbyBosses.get(i).getKey();
                UUID bossId = entity.getUniqueID();
                ServerBossInfo bossInfo = bossBars.get(bossId);
                if (bossInfo != null) {
                    barsToShow.put(bossId, bossInfo);
                    if (!bossInfo.getPlayers().contains(player)) {
                        bossInfo.addPlayer(player);
                        sendBossDataToPlayer(bossId, player);
                    }
                }
            }
            for (UUID bossId : bossIdsToProcess) {
                ServerBossInfo bossInfo = bossBars.get(bossId);
                if (bossInfo != null) {
                    boolean shouldShow = barsToShow.containsKey(bossId);
                    boolean isShowing = bossInfo.getPlayers().contains(player);

                    if (!shouldShow && isShowing) {
                        removeBossBarFromPlayer(bossId, player);
                    }
                }
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }
    private static void removeBossBarFromPlayer(UUID bossId, ServerPlayerEntity player) {
        ServerBossInfo bossInfo = bossBars.get(bossId);
        if (bossInfo != null && bossInfo.getPlayers().contains(player)) {
            bossInfo.removePlayer(player);
        }
        sendRemoveBossDataToPlayer(bossId, player);
        UUID playerId = player.getUniqueID();
        if (playerBossDistances.containsKey(playerId)) {
            playerBossDistances.get(playerId).remove(bossId);
        }
    }
    private static Entity findEntityAcrossDimensions(UUID entityId, net.minecraft.server.MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntityByUuid(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
    public static boolean hasBossBar(UUID entityId) {
        return bossBars.containsKey(entityId) && bossData.containsKey(entityId);
    }
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = event.getEntityLiving();
            UUID entityId = entity.getUniqueID();
            SpiritShieldHandler.resetSpiritShield(entity);
            removeBossBar(entityId);
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                    sendRemoveBossDataToPlayer(entityId, player);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = event.getEntityLiving();
            updateBossBarHealth(entity);
        }
    }
    public static class BossInfoData {
        public List<String> modNames;
        public int tier;
        public String entityType;
        public float currentHealth;
        public float maxHealth;
        public double distance;
        public boolean isSummoned;
        public BossInfoData(List<String> modNames, int tier, String entityType, float currentHealth, float maxHealth, double distance, boolean isSummoned) {
            this.modNames = modNames;
            this.tier = tier;
            this.entityType = entityType;
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.distance = distance;
            this.isSummoned = isSummoned;
        }
    }
}