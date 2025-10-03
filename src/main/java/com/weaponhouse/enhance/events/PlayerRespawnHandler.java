package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import com.weaponhouse.enhance.util.ConfigLoader;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import java.lang.reflect.Field;
import java.util.*;
@Mod.EventBusSubscriber
public class PlayerRespawnHandler {
    private static Field flySpeedField = null;
    public static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final List<String> EXCLUDE_DEFAULT_BUFFS = Arrays.asList(
            "one_enhance", "two_enhance", "three_enhance", "enhance_level"
    );
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayerEntity deadPlayer = (ServerPlayerEntity) event.getOriginal();
            ServerPlayerEntity newPlayer = (ServerPlayerEntity) event.getPlayer();
            CompoundNBT oldData = deadPlayer.getPersistentData();
            CompoundNBT oldBuffs = oldData.contains(BUFF_TAG)
                    ? oldData.getCompound(BUFF_TAG)
                    : new CompoundNBT();
            CompoundNBT modifiedBuffs = new CompoundNBT();
            for (String key : oldBuffs.keySet()) {
                modifiedBuffs.putInt(key, oldBuffs.getInt(key));
            }
            List<String> validBuffs = new ArrayList<>();
            for (String key : modifiedBuffs.keySet()) {
                if (!EXCLUDE_DEFAULT_BUFFS.contains(key) && modifiedBuffs.getInt(key) > 0) {
                    validBuffs.add(key);
                }
            }
            if (ConfigLoader.DEATH_PENALTY_ENABLED) {
                if (validBuffs.size() > ConfigLoader.DEATH_PENALTY_THRESHOLD) {
                    String removedKey = validBuffs.get(new Random().nextInt(validBuffs.size()));
                    modifiedBuffs.remove(removedKey);
                    newPlayer.sendMessage(
                            new TranslationTextComponent(
                                    "message.player_respawn.death_penalty.lose_buff",
                                    getBuffName(newPlayer, removedKey),
                                    oldBuffs.getInt(removedKey)
                            ).mergeStyle(TextFormatting.RED),
                            newPlayer.getUniqueID()
                    );
                } else if (!validBuffs.isEmpty()) {
                    newPlayer.sendMessage(
                            new TranslationTextComponent(
                                    "message.player_respawn.death_penalty.exempt",
                                    ConfigLoader.DEATH_PENALTY_THRESHOLD
                            ).mergeStyle(TextFormatting.GREEN),
                            newPlayer.getUniqueID()
                    );
                }
            } else {
                if (!validBuffs.isEmpty()) {
                    newPlayer.sendMessage(
                            new TranslationTextComponent("message.player_respawn.death_penalty.disabled")
                                    .mergeStyle(TextFormatting.GREEN),
                            newPlayer.getUniqueID()
                    );
                }
            }
            CompoundNBT newData = newPlayer.getPersistentData();
            newData.put(BUFF_TAG, modifiedBuffs);
            if (oldData.contains("PlayerSpawn")) newData.put("PlayerSpawn", oldData.getCompound("PlayerSpawn"));
            if (oldData.contains("BaseMaxHealth")) newData.putFloat("BaseMaxHealth", oldData.getFloat("BaseMaxHealth"));
            if (oldData.contains("BaseAttackDamage")) newData.putFloat("BaseAttackDamage", oldData.getFloat("BaseAttackDamage"));
            if (oldData.contains("naturalArmor")) newData.putFloat("naturalArmor", oldData.getFloat("naturalArmor"));
            if (oldData.contains("BaseMovementSpeed")) newData.putFloat("BaseMovementSpeed", oldData.getFloat("BaseMovementSpeed"));
            try {
                float oldFlySpeed = getPlayerFlySpeed(deadPlayer);
                newData.putFloat("CustomFlySpeed", oldFlySpeed);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains("PlayerSpawn")) {
            CompoundNBT spawnData = playerData.getCompound("PlayerSpawn");
            int spawnX = spawnData.getInt("SpawnX");
            int spawnY = spawnData.getInt("SpawnY");
            int spawnZ = spawnData.getInt("SpawnZ");
            String spawnDimension = spawnData.getString("SpawnDimension");
            try {
                List<ServerWorld> worlds = getLoadedWorlds();
                ServerWorld targetWorld = null;
                for (ServerWorld world : worlds) {
                    RegistryKey<World> dimensionKey = world.getDimensionKey();
                    String dimensionId = dimensionKey.getLocation().toString();
                    if (dimensionId.equals(spawnDimension)) {
                        targetWorld = world;
                        break;
                    }
                }
                if (targetWorld != null) {
                    BlockPos targetPos = new BlockPos(spawnX, spawnY, spawnZ);
                    if (!targetWorld.equals(player.getServerWorld())) {
                        player.teleport(targetWorld, targetPos.getX(), targetPos.getY(), targetPos.getZ(), player.rotationYaw, player.rotationPitch);
                    }
                    player.sendMessage(
                            new TranslationTextComponent(
                                    "message.player_respawn.respawn_at_location",
                                    spawnDimension, spawnX, spawnY, spawnZ
                            ),
                            player.getUniqueID()
                    );
                } else {
                    player.sendMessage(
                            new TranslationTextComponent(
                                    "message.player_respawn.dimension_not_found",
                                    spawnDimension
                            ).mergeStyle(TextFormatting.RED),
                            player.getUniqueID()
                    );
                }
            } catch (Exception e) {
                player.sendMessage(
                        new TranslationTextComponent("message.player_respawn.teleport_failed")
                                .mergeStyle(TextFormatting.RED),
                        player.getUniqueID()
                );
            }
        } else {
            player.sendMessage(
                    new TranslationTextComponent("message.player_respawn.no_spawn_data")
                            .mergeStyle(TextFormatting.YELLOW),
                    player.getUniqueID()
            );
        }
        if (playerData.contains(BUFF_TAG)) {
            CompoundNBT buffs = playerData.getCompound(BUFF_TAG);
            if (buffs.contains("life") && buffs.getInt("life") > 0) LifeHandler.applyLifeBuff(player);
            if (buffs.contains("attack") && buffs.getInt("attack") > 0) AttackHandler.applyAttackBuff(player);
        }
        if (playerData.contains("BaseMaxHealth")) {
            float baseMaxHealth = playerData.getFloat("BaseMaxHealth");
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseMaxHealth);
            player.setHealth(baseMaxHealth);
        }
        if (playerData.contains("BaseAttackDamage")) {
            float baseAttackDamage = playerData.getFloat("BaseAttackDamage");
            player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttackDamage);
        }
        if (playerData.contains("BaseMovementSpeed")) {
            float baseMovementSpeed = playerData.getFloat("BaseMovementSpeed");
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseMovementSpeed);
        }
        if (playerData.contains("CustomFlySpeed")) {
            try {
                float savedFlySpeed = playerData.getFloat("CustomFlySpeed");
                setPlayerFlySpeed(player, savedFlySpeed);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static String getBuffName(ServerPlayerEntity player, String buffKey) {
        String langKey = "buff.enhance." + buffKey;
        return new TranslationTextComponent(langKey).getString();
    }
    private static float getPlayerFlySpeed(ServerPlayerEntity player) throws Exception {
        PlayerAbilities abilities = player.abilities;
        if (flySpeedField == null) findFlySpeedField(abilities);
        if (flySpeedField != null) {
            flySpeedField.setAccessible(true);
            return flySpeedField.getFloat(abilities);
        } else {
            return abilities.getFlySpeed();
        }
    }
    private static void setPlayerFlySpeed(ServerPlayerEntity player, float speed) throws Exception {
        PlayerAbilities abilities = player.abilities;
        if (flySpeedField == null) findFlySpeedField(abilities);
        if (flySpeedField != null) {
            flySpeedField.setAccessible(true);
            flySpeedField.setFloat(abilities, speed);
        } else {
            callSetFlySpeedMethod(abilities, speed);
        }
        player.sendPlayerAbilities();
    }
    private static void findFlySpeedField(PlayerAbilities abilities) {
        try {
            flySpeedField = findFieldByDefaultValue(abilities, 0.05f);
            if (flySpeedField == null) {
                String[] possibleFieldNames = {"flySpeed", "field_75097_g", "field_75096_f", "d", "flySpeedMultiplier"};
                for (String fieldName : possibleFieldNames) {
                    try {
                        flySpeedField = abilities.getClass().getDeclaredField(fieldName);
                        break;
                    } catch (NoSuchFieldException e) { /* 忽略 */ }
                }
            }
            if (flySpeedField == null) {
                for (Field field : abilities.getClass().getDeclaredFields()) {
                    if (field.getType() == float.class) {
                        flySpeedField = field;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static Field findFieldByDefaultValue(PlayerAbilities abilities, float defaultValue) {
        try {
            for (Field field : abilities.getClass().getDeclaredFields()) {
                if (field.getType() == float.class) {
                    field.setAccessible(true);
                    float value = field.getFloat(abilities);
                    if (Math.abs(value - defaultValue) < 0.001f) return field;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private static void callSetFlySpeedMethod(PlayerAbilities abilities, float speed) {
        try {
            java.lang.reflect.Method setFlySpeed = abilities.getClass().getDeclaredMethod("setFlySpeed", float.class);
            setFlySpeed.setAccessible(true);
            setFlySpeed.invoke(abilities, speed);
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Method setFlySpeed = abilities.getClass().getDeclaredMethod("func_75092_a", float.class);
                setFlySpeed.setAccessible(true);
                setFlySpeed.invoke(abilities, speed);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static List<ServerWorld> getLoadedWorlds() {
        Collection<ServerWorld> worlds = (Collection<ServerWorld>) ServerLifecycleHooks.getCurrentServer().getWorlds();
        return new ArrayList<>(worlds);
    }
}