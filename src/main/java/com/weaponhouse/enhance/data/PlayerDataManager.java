package com.weaponhouse.enhance.data;

import com.weaponhouse.enhance.Enhance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
@Mod.EventBusSubscriber(modid = "enhance")
public class PlayerDataManager {
    private static final String DATA_NAME = "enhance_player_data";
    private static final Map<UUID, CompoundNBT> deathBackup = new HashMap<>();
    public static class EnhancePlayerData extends WorldSavedData {
        private final Map<UUID, CompoundNBT> playerData = new HashMap<>();
        public EnhancePlayerData() {
            super(DATA_NAME);
        }
        @Override
        public void read(CompoundNBT nbt) {
            playerData.clear();
            CompoundNBT playersNbt = nbt.getCompound("players");
            for (String key : playersNbt.keySet()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    playerData.put(uuid, playersNbt.getCompound(key));
                } catch (IllegalArgumentException e) {
                    Enhance.LOGGER.error("无效的UUID: {}", key);
                }
            }
        }
        @Override
        public CompoundNBT write(CompoundNBT nbt) {
            CompoundNBT playersNbt = new CompoundNBT();
            for (Map.Entry<UUID, CompoundNBT> entry : playerData.entrySet()) {
                playersNbt.put(entry.getKey().toString(), entry.getValue());
            }
            nbt.put("players", playersNbt);
            return nbt;
        }
        public CompoundNBT getPlayerData(UUID playerId) {
            return playerData.getOrDefault(playerId, new CompoundNBT());
        }
        public void setPlayerData(UUID playerId, CompoundNBT data) {
            playerData.put(playerId, data);
            this.markDirty();
        }
    }
    public static CompoundNBT getPermanentPlayerData(ServerPlayerEntity player) {
        EnhancePlayerData data = getPlayerDataStorage(player);
        if (data != null) {
            return data.getPlayerData(player.getUniqueID());
        }
        return new CompoundNBT();
    }
    public static void savePermanentPlayerData(ServerPlayerEntity player, CompoundNBT data) {
        EnhancePlayerData storage = getPlayerDataStorage(player);
        if (storage != null) {
            storage.setPlayerData(player.getUniqueID(), data);
        }
    }
    private static EnhancePlayerData getPlayerDataStorage(ServerPlayerEntity player) {
        if (player == null || player.world == null || player.world.getServer() == null) {
            return null;
        }
        return Objects.requireNonNull(player.world.getServer()
                        .getWorld(player.world.getDimensionKey()))
                .getSavedData()
                .getOrCreate(EnhancePlayerData::new, DATA_NAME);
    }
    @SubscribeEvent
    public static void onPlayerDeath(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            UUID playerId = player.getUniqueID();
            if (!deathBackup.containsKey(playerId)) {
                CompoundNBT backupData = getPermanentPlayerData(player);
                deathBackup.put(playerId, backupData.copy());
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            UUID playerId = player.getUniqueID();
            if (deathBackup.containsKey(playerId)) {
                CompoundNBT backupData = deathBackup.get(playerId);
                savePermanentPlayerData(player, backupData);
                deathBackup.remove(playerId);
                syncToPlayerRuntimeData(player, backupData);
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            CompoundNBT permanentData = getPermanentPlayerData(player);
            syncToPlayerRuntimeData(player, permanentData);
            verifyAndFixPlayerData(player);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            CompoundNBT runtimeData = getRuntimePlayerData(player);
            savePermanentPlayerData(player, runtimeData);
        }
    }
    private static CompoundNBT getRuntimePlayerData(ServerPlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        CompoundNBT enhanceData = new CompoundNBT();
        if (playerData.contains("EnhancePermanentData")) {
            enhanceData.put("PermanentData", playerData.getCompound("EnhancePermanentData"));
        }
        if (playerData.contains("WeaponHouseBuffs")) {
            enhanceData.put("Buffs", playerData.getCompound("WeaponHouseBuffs"));
        }
        return enhanceData;
    }
    private static void syncToPlayerRuntimeData(ServerPlayerEntity player, CompoundNBT permanentData) {
        if (permanentData.contains("PermanentData")) {
            player.getPersistentData().put("EnhancePermanentData", permanentData.getCompound("PermanentData"));
        }
        if (permanentData.contains("Buffs")) {
            player.getPersistentData().put("WeaponHouseBuffs", permanentData.getCompound("Buffs"));
        }
    }
    private static void verifyAndFixPlayerData(ServerPlayerEntity player) {
        CompoundNBT permanentData = getPermanentPlayerData(player);
        CompoundNBT buffs = permanentData.contains("Buffs")
                ? permanentData.getCompound("Buffs")
                : new CompoundNBT();
        if (permanentData.contains("PermanentData")) {
            CompoundNBT playerPermData = permanentData.getCompound("PermanentData");
            if (playerPermData.getBoolean("enhance_stone_triggered") &&
                    !playerPermData.getBoolean("enhance_stone_level_increased")) {
                int level = buffs.getInt("enhance_level");
                if (level < 1) {
                    buffs.putInt("enhance_level", 1);
                    permanentData.put("Buffs", buffs);
                    playerPermData.putBoolean("enhance_stone_level_increased", true);
                    permanentData.put("PermanentData", playerPermData);
                    savePermanentPlayerData(player, permanentData);
                    syncToPlayerRuntimeData(player, permanentData);
                }
            }
        }
    }
}