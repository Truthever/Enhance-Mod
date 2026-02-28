package com.weaponhouse.enhance.invasion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
public class InvasionConfig {
    private static InvasionSettings settings = new InvasionSettings();
    private static final Path CONFIG_PATH = FMLLoader.getGamePath()
            .resolve("config")
            .resolve("enhance")
            .resolve("invasion_settings.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static {
        ensureConfigFileExists();
        loadConfigFromFile();
    }
    public static class InvasionSettings {
        public int invasionIntervalDays = 5;
        public int invasionDurationDays = 1;
        public long nightStartTicks = 13000L;
        public Map<String, Integer> monsterCountByLevel = new HashMap<>();
        public Map<String, List<Integer>> monsterEnhanceLevelByLevel = new HashMap<>();
        public List<String> invasionMonsterTypes = new ArrayList<>();
        public int spawnRadiusMin = 10;
        public int spawnRadiusMax = 30;
        public int monsterRespawnInterval = 200;
        //暂时禁用增幅入侵
        public boolean enabled = false;
        public boolean debugMode = true;
        public boolean infiniteMode = false;
        public int infiniteMaxMonstersPerPlayer = 50;
        public double infiniteRespawnRate = 0.5;
        public InvasionSettings() {
            monsterCountByLevel.put("1", 5);
            monsterCountByLevel.put("2", 5);
            monsterCountByLevel.put("3", 10);
            monsterCountByLevel.put("4", 10);
            monsterCountByLevel.put("5", 15);
            monsterCountByLevel.put("6", 20);
            monsterEnhanceLevelByLevel.put("1", Collections.singletonList(1));
            monsterEnhanceLevelByLevel.put("2", Collections.singletonList(1));
            monsterEnhanceLevelByLevel.put("3", Arrays.asList(1, 2));
            monsterEnhanceLevelByLevel.put("4", Arrays.asList(1, 2));
            monsterEnhanceLevelByLevel.put("5", Arrays.asList(1, 2, 3));
            monsterEnhanceLevelByLevel.put("6", Arrays.asList(1, 2, 3));
            invasionMonsterTypes.addAll(Arrays.asList(
                    "minecraft:zombie",
                    "minecraft:skeleton",
                    "minecraft:enderman",
                    "minecraft:wither_skeleton",
                    "minecraft:blaze",
                    "minecraft:pillager",
                    "minecraft:vindicator"
            ));
        }
    }
    private static void ensureConfigFileExists() {
        File configFile = CONFIG_PATH.toFile();
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                createDefaultConfig(configFile);
            } catch (IOException ignored) {
            }
        }
    }
    private static void createDefaultConfig(File configFile) throws IOException {
        InvasionSettings defaultSettings = new InvasionSettings();
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(defaultSettings, writer);
        }
    }
    private static void loadConfigFromFile() {
        File configFile = CONFIG_PATH.toFile();
        if (!configFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            InvasionSettings loadedSettings = GSON.fromJson(reader, InvasionSettings.class);
            if (loadedSettings != null) {
                settings = loadedSettings;
            }
        } catch (IOException ignored) {
        }
    }
    public static void saveConfigToFile() {
        try {
            File configFile = CONFIG_PATH.toFile();
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(settings, writer);
            }
        } catch (IOException ignored) {
        }
    }
    public static void reloadConfig() {
        loadConfigFromFile();
    }
    public static int getInvasionIntervalDays() {
        return settings.invasionIntervalDays;
    }
    public static int getInvasionDurationDays() {
        return settings.invasionDurationDays;
    }
    public static long getNightStartTicks() {
        return settings.nightStartTicks;
    }
    public static int getMonsterCountByLevel(int playerLevel) {
        String levelKey = String.valueOf(playerLevel);
        return settings.monsterCountByLevel.getOrDefault(levelKey, 5);
    }
    public static List<Integer> getMonsterEnhanceLevels(int playerLevel) {
        String levelKey = String.valueOf(playerLevel);
        return settings.monsterEnhanceLevelByLevel.getOrDefault(levelKey, Collections.singletonList(1));
    }
    public static List<String> getInvasionMonsterTypes() {
        return settings.invasionMonsterTypes;
    }
    public static int getSpawnRadiusMin() {
        return settings.spawnRadiusMin;
    }
    public static int getSpawnRadiusMax() {
        return settings.spawnRadiusMax;
    }
    public static int getMonsterRespawnInterval() {
        return settings.monsterRespawnInterval;
    }
    public static boolean isEnabled() {
        return settings.enabled;
    }
    public static boolean isDebugMode() {
        return settings.debugMode;
    }
    public static boolean isInfiniteMode() {
        return settings.infiniteMode;
    }
    public static int getInfiniteMaxMonstersPerPlayer() {
        return settings.infiniteMaxMonstersPerPlayer;
    }
    public static double getInfiniteRespawnRate() {
        return settings.infiniteRespawnRate;
    }
    public static void setInvasionIntervalDays(int days) {
        settings.invasionIntervalDays = days;
        saveConfigToFile();
    }
    public static void setInvasionDurationDays(int days) {
        settings.invasionDurationDays = days;
        saveConfigToFile();
    }
    public static void setMonsterCountByLevel(int playerLevel, int monsterCount) {
        settings.monsterCountByLevel.put(String.valueOf(playerLevel), monsterCount);
        saveConfigToFile();
    }
    public static void addInvasionMonsterType(String monsterType) {
        if (!settings.invasionMonsterTypes.contains(monsterType)) {
            settings.invasionMonsterTypes.add(monsterType);
            saveConfigToFile();
        }
    }
    public static void removeInvasionMonsterType(String monsterType) {
        settings.invasionMonsterTypes.remove(monsterType);
        saveConfigToFile();
    }
    public static void setEnabled(boolean enabled) {
        settings.enabled = enabled;
        saveConfigToFile();
    }
}