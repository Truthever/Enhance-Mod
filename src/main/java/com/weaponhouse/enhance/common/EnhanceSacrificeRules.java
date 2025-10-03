package com.weaponhouse.enhance.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weaponhouse.enhance.Enhance;
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
    // 配置文件路径
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("enhance/sacrifice_rules.json");

    // 献祭加成映射表（从配置文件加载）
    private static Map<String, Float> sacrificeHealthBonus = new HashMap<>();
    private static Map<String, Float> sacrificeAttackBonus = new HashMap<>();
    private static Map<String, Float> sacrificeDefenseBonus = new HashMap<>();
    private static Map<String, Float> sacrificeSpeedBonus = new HashMap<>();
    private static Map<String, float[]> sacrificeHarmonyBonus = new HashMap<>();

    // 静态初始化：加载配置文件
    static {
        loadConfig();
    }

    /**
     * 加载配置文件，如果文件不存在则创建默认配置
     */
    public static void loadConfig() {
        try {
            // 确保配置目录存在
            File configDir = CONFIG_PATH.getParent().toFile();
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            // 如果配置文件不存在，创建默认配置
            if (!CONFIG_PATH.toFile().exists()) {
                createDefaultConfig();
            }

            // 读取配置文件
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                Type type = new TypeToken<SacrificeConfig>() {}.getType();
                SacrificeConfig config = gson.fromJson(reader, type);

                // 初始化映射表
                sacrificeHealthBonus = config.healthBonus;
                sacrificeAttackBonus = config.attackBonus;
                sacrificeDefenseBonus = config.defenseBonus;
                sacrificeSpeedBonus = config.speedBonus;
                sacrificeHarmonyBonus = config.harmonyBonus;

                Enhance.LOGGER.info("Successfully loaded sacrifice rules config");
            }
        } catch (Exception e) {
            Enhance.LOGGER.error("Failed to load sacrifice rules config, using defaults", e);
            // 加载失败时使用默认配置
            setDefaultValues();
        }
    }

    /**
     * 创建默认配置文件
     */
    private static void createDefaultConfig() throws IOException {
        // 设置默认值
        setDefaultValues();

        // 创建配置对象
        SacrificeConfig defaultConfig = new SacrificeConfig();
        defaultConfig.healthBonus = sacrificeHealthBonus;
        defaultConfig.attackBonus = sacrificeAttackBonus;
        defaultConfig.defenseBonus = sacrificeDefenseBonus;
        defaultConfig.speedBonus = sacrificeSpeedBonus;
        defaultConfig.harmonyBonus = sacrificeHarmonyBonus;

        // 写入文件
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            gson.toJson(defaultConfig, writer);
        }
    }

    /**
     * 设置默认数值（与原硬编码值保持一致）
     */
    private static void setDefaultValues() {
        sacrificeHealthBonus.clear();
        sacrificeHealthBonus.put("life", 1.0f);
        sacrificeHealthBonus.put("fasting", 1.0f);
        sacrificeHealthBonus.put("photosynthesis", 0.5f);
        sacrificeHealthBonus.put("vampire", 2.0f);
        sacrificeHealthBonus.put("curse", 2.0f);
        sacrificeHealthBonus.put("unyielding", 5.0f);

        sacrificeAttackBonus.clear();
        sacrificeAttackBonus.put("attack", 0.25f);
        sacrificeAttackBonus.put("megaforce", 0.20f);
        sacrificeAttackBonus.put("rob", 0.5f);
        sacrificeAttackBonus.put("displacement", 0.5f);
        sacrificeAttackBonus.put("thunder", 0.20f);
        sacrificeAttackBonus.put("ricochet", 1.0f);

        sacrificeDefenseBonus.clear();
        sacrificeDefenseBonus.put("thorns", 0.25f);
        sacrificeDefenseBonus.put("death_bomb", 0.25f);

        sacrificeSpeedBonus.clear();
        sacrificeSpeedBonus.put("frost", 0.005f);
        sacrificeSpeedBonus.put("aura", 0.01f);
        sacrificeSpeedBonus.put("hunger", 0.005f);
        sacrificeSpeedBonus.put("phantom", 0.01f);

        sacrificeHarmonyBonus.clear();
        sacrificeHarmonyBonus.put("harmony", new float[]{2.0f, 0.5f, 0.25f});
    }

    // Getter方法
    public static Map<String, Float> getSacrificeHealthBonus() {
        return new HashMap<>(sacrificeHealthBonus); // 返回副本防止外部修改
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
        // 深拷贝数组防止外部修改
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

    /**
     * 配置文件数据结构
     */
    private static class SacrificeConfig {
        Map<String, Float> healthBonus = new HashMap<>();
        Map<String, Float> attackBonus = new HashMap<>();
        Map<String, Float> defenseBonus = new HashMap<>();
        Map<String, Float> speedBonus = new HashMap<>();
        Map<String, float[]> harmonyBonus = new HashMap<>();
    }
}
