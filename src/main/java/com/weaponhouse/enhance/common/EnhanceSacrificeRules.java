package com.weaponhouse.enhance.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
public class EnhanceSacrificeRules {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("enhance/sacrifice_rules.json");
    private static Map<String, Float> sacrificeHealthBonus = new HashMap<>();
    private static Map<String, Float> sacrificeAttackBonus = new HashMap<>();
    private static Map<String, Float> sacrificeDefenseBonus = new HashMap<>();
    private static Map<String, Float> sacrificeSpeedBonus = new HashMap<>();
    private static Map<String, float[]> sacrificeHarmonyBonus = new HashMap<>();
    static {
        loadConfig();
    }
    public static void loadConfig() {
        try {
            File configDir = CONFIG_PATH.getParent().toFile();
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            if (!CONFIG_PATH.toFile().exists()) {
                createDefaultConfig();
            }
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                Type type = new TypeToken<SacrificeConfig>() {}.getType();
                SacrificeConfig config = gson.fromJson(reader, type);
                sacrificeHealthBonus = config.healthBonus;
                sacrificeAttackBonus = config.attackBonus;
                sacrificeDefenseBonus = config.defenseBonus;
                sacrificeSpeedBonus = config.speedBonus;
                sacrificeHarmonyBonus = config.harmonyBonus;
            }
        } catch (Exception e) {
            setDefaultValues();
        }
    }
    private static void createDefaultConfig() throws IOException {
        setDefaultValues();
        SacrificeConfig defaultConfig = new SacrificeConfig();
        defaultConfig.healthBonus = sacrificeHealthBonus;
        defaultConfig.attackBonus = sacrificeAttackBonus;
        defaultConfig.defenseBonus = sacrificeDefenseBonus;
        defaultConfig.speedBonus = sacrificeSpeedBonus;
        defaultConfig.harmonyBonus = sacrificeHarmonyBonus;
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            gson.toJson(defaultConfig, writer);
        }
    }
    private static void setDefaultValues() {
        sacrificeHealthBonus.clear();
        sacrificeHealthBonus.put("life", 1.0f);
        sacrificeHealthBonus.put("fasting", 1.0f);
        sacrificeHealthBonus.put("photosynthesis", 0.5f);
        sacrificeHealthBonus.put("vampire", 2.0f);
        sacrificeHealthBonus.put("curse", 2.0f);
        sacrificeHealthBonus.put("unyielding", 5.0f);
        sacrificeHealthBonus.put("chaos", 0.5f);
        sacrificeHealthBonus.put("inspiration", 5.0f);
        sacrificeAttackBonus.clear();
        sacrificeAttackBonus.put("attack", 0.25f);
        sacrificeAttackBonus.put("megaforce", 0.20f);
        sacrificeAttackBonus.put("rob", 0.5f);
        sacrificeAttackBonus.put("displacement", 0.5f);
        sacrificeAttackBonus.put("thunder", 0.20f);
        sacrificeAttackBonus.put("ricochet", 1.0f);
        sacrificeAttackBonus.put("annihilation", 0.5f);
        sacrificeAttackBonus.put("corrosion", 0.5f);
        sacrificeAttackBonus.put("combo", 0.25f);
        sacrificeDefenseBonus.clear();
        sacrificeDefenseBonus.put("thorns", 0.25f);
        sacrificeDefenseBonus.put("death_bomb", 0.25f);
        sacrificeDefenseBonus.put("spirit_shield", 0.125f);
        sacrificeSpeedBonus.clear();
        sacrificeSpeedBonus.put("frost", 0.0025f);
        sacrificeSpeedBonus.put("aura", 0.005f);
        sacrificeSpeedBonus.put("hunger", 0.0025f);
        sacrificeSpeedBonus.put("phantom", 0.005f);
        sacrificeHarmonyBonus.clear();
        sacrificeHarmonyBonus.put("harmony", new float[]{2.0f, 0.5f, 0.25f});
    }
    public static Map<String, Float> getSacrificeHealthBonus() {
        return new HashMap<>(sacrificeHealthBonus);
    }
    public static Map<String, Float> getSacrificeAttackBonus() {
        return new HashMap<>(sacrificeAttackBonus);
    }
    public static Map<String, Float> getSacrificeDefenseBonus() {
        return new HashMap<>(sacrificeDefenseBonus);
    }
    public static Map<String, Float> getSacrificeSpeedBonus() {
        return new HashMap<>(sacrificeSpeedBonus);
    }
    public static Map<String, float[]> getSacrificeHarmonyBonus() {
        Map<String, float[]> copy = new HashMap<>();
        sacrificeHarmonyBonus.forEach((k, v) -> copy.put(k, v.clone()));
        return copy;
    }
    public static boolean isSacrificeableBuff(String buffId) {
        return sacrificeHealthBonus.containsKey(buffId)
                || sacrificeAttackBonus.containsKey(buffId)
                || sacrificeDefenseBonus.containsKey(buffId)
                || sacrificeSpeedBonus.containsKey(buffId)
                || sacrificeHarmonyBonus.containsKey(buffId);
    }
    private static class SacrificeConfig {
        Map<String, Float> healthBonus = new HashMap<>();
        Map<String, Float> attackBonus = new HashMap<>();
        Map<String, Float> defenseBonus = new HashMap<>();
        Map<String, Float> speedBonus = new HashMap<>();
        Map<String, float[]> harmonyBonus = new HashMap<>();
    }
}
