package com.weaponhouse.enhance.enhances;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.network.BossDataPacket;
import com.weaponhouse.enhance.network.RemoveBossDataPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
    private static final Map<String, Integer> MOD_PRIORITY = new HashMap<>();
    private static boolean hasRestoredExistingMobs = false;
    static {
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
    }
    public static void createBossBar(LivingEntity entity, int tier) {
        if (!(entity.world instanceof ServerWorld)) return;
        UUID entityId = entity.getUniqueID();
        removeBossBar(entityId);
        List<String> modNames = getModNames(entity);
        String entityType = getEntityType(entity);
        BossInfoData data = new BossInfoData(modNames, tier, entityType, entity.getHealth(), entity.getMaxHealth(), 0.0);
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
        for (ServerPlayerEntity player : bossInfo.getPlayers()) {
            double distance = getPlayerBossDistance(player.getUniqueID(), entityId);
            Enhance.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new BossDataPacket(entityId, data.modNames, data.tier, data.entityType, data.currentHealth, data.maxHealth, distance)
            );
        }
    }
    private static String getEntityType(LivingEntity entity) {
        ResourceLocation key = entity.getType().getRegistryName();
        if (key != null) {
            return "entity." + key.getNamespace() + "." + key.getPath();
        }
        return entity.getType().getName().getString();
    }
    private static List<String> getModNames(LivingEntity entity) {
        List<String> modNames = new ArrayList<>();
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains("WeaponHouseBuffs")) {
            CompoundNBT buffsTag = entityData.getCompound("WeaponHouseBuffs");
            List<Map.Entry<String, Integer>> sortedMods = new ArrayList<>();
            for (String key : buffsTag.keySet()) {
                if (isEnhancementKey(key)) continue;
                sortedMods.add(new AbstractMap.SimpleEntry<>(key, buffsTag.getInt(key)));
            }
            sortedMods.sort((a, b) -> {
                int priorityA = MOD_PRIORITY.getOrDefault(a.getKey(), 0);
                int priorityB = MOD_PRIORITY.getOrDefault(b.getKey(), 0);
                return Integer.compare(priorityB, priorityA);
            });
            for (Map.Entry<String, Integer> entry : sortedMods) {
                String key = entry.getKey();
                int level = entry.getValue();
                modNames.add(key + ":" + level);
            }
        }
        return modNames;
    }
    private static void sendBossDataToPlayer(UUID entityId, ServerPlayerEntity player) {
        ServerBossInfo bossInfo = bossBars.get(entityId);
        BossInfoData data = bossData.get(entityId);
        if (bossInfo == null || data == null) return;
        double distance = getPlayerBossDistance(player.getUniqueID(), entityId);
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
        ServerBossInfo bossInfo = bossBars.remove(entityId);
        if (bossInfo != null) {
            bossInfo.setVisible(false);
            bossInfo.removeAllPlayers();
        }
        bossData.remove(entityId);
        for (Map<UUID, Double> playerDistances : playerBossDistances.values()) {
            playerDistances.remove(entityId);
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
        if (player.ticksExisted % 10 != 0) return;
        Map<UUID, ServerBossInfo> barsToShow = new HashMap<>(3);
        List<Map.Entry<LivingEntity, Double>> nearbyBosses = new ArrayList<>();
        for (Map.Entry<UUID, ServerBossInfo> entry : bossBars.entrySet()) {
            Entity entity = ((ServerWorld) player.world).getEntityByUuid(entry.getKey());
            if (entity instanceof LivingEntity && entity.isAlive()) {
                LivingEntity livingEntity = (LivingEntity) entity;
                UUID bossId = entity.getUniqueID();
                double dx = Math.abs(player.getPosX() - livingEntity.getPosX());
                double dy = Math.abs(player.getPosY() - livingEntity.getPosY());
                double dz = Math.abs(player.getPosZ() - livingEntity.getPosZ());
                if (dx <= RANGE_XZ && dy <= RANGE_Y && dz <= RANGE_XZ) {
                    double distance = player.getDistance(livingEntity);
                    nearbyBosses.add(new AbstractMap.SimpleEntry<>(livingEntity, distance));
                    setPlayerBossDistance(playerId, bossId, distance);
                } else {
                    playerBossDistances.getOrDefault(playerId, Collections.emptyMap()).remove(bossId);
                }
            }
        }
        nearbyBosses.sort(Comparator.comparingDouble(Map.Entry::getValue));
        int count = Math.min(nearbyBosses.size(), 3);
        for (int i = 0; i < count; i++) {
            LivingEntity entity = nearbyBosses.get(i).getKey();
            UUID bossId = entity.getUniqueID();
            barsToShow.put(bossId, bossBars.get(bossId));
            if (!bossBars.get(bossId).getPlayers().contains(player)) {
                bossBars.get(bossId).addPlayer(player);
                sendBossDataToPlayer(bossId, player);
            }
        }
        for (Map.Entry<UUID, ServerBossInfo> entry : bossBars.entrySet()) {
            ServerBossInfo bossInfo = entry.getValue();
            UUID bossId = entry.getKey();
            boolean shouldShow = barsToShow.containsKey(bossId);
            boolean isShowing = bossInfo.getPlayers().contains(player);
            if (!shouldShow && isShowing) {
                bossInfo.removePlayer(player);
                sendRemoveBossDataToPlayer(bossId, player);
            }
        }
    }
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = event.getEntityLiving();
            UUID entityId = entity.getUniqueID();
            if (bossBars.containsKey(entityId)) {
                removeBossBar(entityId);
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
        public BossInfoData(List<String> modNames, int tier, String entityType, float currentHealth, float maxHealth, double distance) {
            this.modNames = modNames;
            this.tier = tier;
            this.entityType = entityType;
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.distance = distance;
        }
    }
}
