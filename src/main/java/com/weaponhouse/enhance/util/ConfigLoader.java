package com.weaponhouse.enhance.util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean BLIND_MODE = false;
    public static boolean PORTABLE_UI_ENABLED = false;
    public static boolean ENHANCE_BOSSBAR_ENABLED = true;
    public static class FireResistanceSetting {
        private final boolean enabled;
        private final int durationTicks;
        private final boolean persistent;
        public FireResistanceSetting(boolean enabled, int durationSeconds, boolean persistent) {
            this.enabled = enabled;
            this.durationTicks = durationSeconds * 20;
            this.persistent = persistent;
        }
        public boolean isEnabled() { return enabled; }
        public int getDurationTicks() { return durationTicks; }
        public boolean isPersistent() { return persistent; }
    }
    public static Map<String, List<String>> TIER_ONE_BAN_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, List<String>> TIER_TWO_BAN_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, List<String>> TIER_THREE_BAN_BY_DIFFICULTY = new HashMap<>();
    public static float ENHANCE_STONE_DROP_CHANCE_TIER1 = 0.02f;
    public static float ENHANCE_STONE_DROP_CHANCE_TIER2 = 0.05f;
    public static float ENHANCE_STONE_DROP_CHANCE_TIER3 = 0.1f;
    public static boolean DEATH_PENALTY_ENABLED = true;
    public static int DEATH_PENALTY_THRESHOLD = 3;
    public static Map<String, FireResistanceSetting> FIRE_RESISTANCE_SETTINGS = new HashMap<>();
    private static final Path GAME_ROOT_DIR = FMLLoader.getGamePath();
    public static final Path CONFIG_DIR = GAME_ROOT_DIR.resolve("config").resolve("enhance");
    public static Map<String, Float> ENHANCER_CHANCES = new HashMap<>();
    public static float LEVEL_ONE_CHANCE = 0.8f;
    public static float LEVEL_TWO_CHANCE = 0.15f;
    public static float GREEN_GIFT_DROP_CHANCE = 0.3f;
    public static float BLUE_GIFT_DROP_CHANCE = 0.25f;
    public static float RED_GIFT_DROP_CHANCE = 0.2f;
    public static int GREEN_GIFT_BUFF_COUNT = 1;
    public static int BLUE_GIFT_BUFF_COUNT = 1;
    public static int RED_GIFT_BUFF_COUNT = 1;
    public static Map<String, List<String>> TIER_ONE_BUFFS_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, List<String>> TIER_TWO_BUFFS_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, List<String>> TIER_THREE_BUFFS_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, Map<String, int[]>> TIER_ONE_RANGES_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, Map<String, int[]>> TIER_TWO_RANGES_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, Map<String, int[]>> TIER_THREE_RANGES_BY_DIFFICULTY = new HashMap<>();
    public static Map<String, int[]> GREEN_GIFT_BUFF_RANGES = new HashMap<>();
    public static Map<String, int[]> BLUE_GIFT_BUFF_RANGES = new HashMap<>();
    public static List<String> GREEN_GIFT_AVAILABLE_BUFFS = new ArrayList<>();
    public static List<String> BLUE_GIFT_AVAILABLE_BUFFS = new ArrayList<>();
    public static List<String> RED_GIFT_AVAILABLE_BUFFS = new ArrayList<>();
    public static Map<String, int[]> RED_GIFT_BUFF_RANGES = new HashMap<>();
    public static Map<String, int[]> RED_GIFT_HARD_BUFF_RANGES = new HashMap<>();
    public static Map<String, Map<String, Object>> TIER_BUFF_GUARANTEE_RULES = new HashMap<>();
    public static void loadConfigs() {
        File configDir = CONFIG_DIR.toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        loadProbabilityConfig();
        loadBuffPoolConfig();
        loadGiftBuffConfig();
        loadBuffGuaranteeConfig();
        loadFireResistanceSettings();
    }
    private static void loadFireResistanceSettings() {
        File configFile = CONFIG_DIR.resolve("settings.json").toFile();
        if (!configFile.exists()) {
            createDefaultFireResistanceSettings(configFile);
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            Type rootType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> root = GSON.fromJson(reader, rootType);
            Map<String, Object> fireResNode = (Map<String, Object>) root.getOrDefault(
                    "fire_resistance_settings",
                    new HashMap<>()
            );
            loadSingleTierFireResSetting(fireResNode, "tier1", false, 300, true);
            loadSingleTierFireResSetting(fireResNode, "tier2", false, 600, true);
            PORTABLE_UI_ENABLED = (boolean) root.getOrDefault("portable_ui_enabled", false);
            loadSingleTierFireResSetting(fireResNode, "tier3", true, 10000, true);
            DEATH_PENALTY_ENABLED = (boolean) root.getOrDefault("death_penalty_enabled", true);
            DEATH_PENALTY_THRESHOLD = getIntFromConfig(
                    root,
                    "death_penalty_threshold",
                    "death_penalty_threshold",
                    3
            );
            DEATH_PENALTY_THRESHOLD = Math.max(0, DEATH_PENALTY_THRESHOLD);
            ENHANCE_BOSSBAR_ENABLED = (boolean) root.getOrDefault("enhance_bossbar", true);
            BLIND_MODE = (boolean) root.getOrDefault("blind_mode", false);
        } catch (IOException e) {
            setDefaultFireResistanceSettings();
        }
    }
    @SuppressWarnings("unchecked")
    private static void loadSingleTierFireResSetting(
            Map<String, Object> fireResNode,
            String tierKey,
            boolean defaultEnabled,
            int defaultDurationSec,
            boolean defaultPersistent
    ) {
        if (!fireResNode.containsKey(tierKey)) {
            FIRE_RESISTANCE_SETTINGS.put(tierKey, new FireResistanceSetting(
                    defaultEnabled, defaultDurationSec, defaultPersistent
            ));
            return;
        }
        Map<String, Object> tierConfig = (Map<String, Object>) fireResNode.get(tierKey);
        boolean enabled = (boolean) tierConfig.getOrDefault("enabled", defaultEnabled);
        int durationSec = getIntFromConfig(
                tierConfig,
                "duration_seconds",
                "fire_resistance_settings." + tierKey + ".duration_seconds",
                defaultDurationSec
        );
        boolean persistent = (boolean) tierConfig.getOrDefault("persistent", defaultPersistent);
        FIRE_RESISTANCE_SETTINGS.put(tierKey, new FireResistanceSetting(
                enabled,
                Math.max(1, durationSec),
                persistent
        ));
    }
    private static void createDefaultFireResistanceSettings(File configFile) {
        Map<String, Object> defaultRoot = new HashMap<>();
        Map<String, Object> fireResNode = new HashMap<>();
        Map<String, Object> tier1 = new HashMap<>();
        tier1.put("enabled", false);
        tier1.put("duration_seconds", 300);
        tier1.put("persistent", true);
        fireResNode.put("tier1", tier1);
        Map<String, Object> tier2 = new HashMap<>();
        tier2.put("enabled", false);
        tier2.put("duration_seconds", 600);
        tier2.put("persistent", true);
        fireResNode.put("tier2", tier2);
        Map<String, Object> tier3 = new HashMap<>();
        tier3.put("enabled", true);
        tier3.put("duration_seconds", 10000);
        tier3.put("persistent", true);
        fireResNode.put("tier3", tier3);
        defaultRoot.put("fire_resistance_settings", fireResNode);
        defaultRoot.put("portable_ui_enabled", false);
        defaultRoot.put("death_penalty_enabled", true);
        defaultRoot.put("death_penalty_threshold", 3);
        defaultRoot.put("enhance_bossbar", true);
        defaultRoot.put("blind_mode", false);
        try (FileWriter writer = new FileWriter(configFile)) {
            String json = GSON.toJson(defaultRoot);
            writer.write(json);
        } catch (IOException e) {
        }
    }
    private static void setDefaultFireResistanceSettings() {
        FIRE_RESISTANCE_SETTINGS.clear();
        FIRE_RESISTANCE_SETTINGS.put("tier1", new FireResistanceSetting(false, 300, true));
        FIRE_RESISTANCE_SETTINGS.put("tier2", new FireResistanceSetting(false, 600, true));
        FIRE_RESISTANCE_SETTINGS.put("tier3", new FireResistanceSetting(true, 10000, true));
    }
    private static void loadBuffGuaranteeConfig() {
        File configFile = CONFIG_DIR.resolve("buff_guarantee.json").toFile();
        if (!configFile.exists()) {
            createDefaultBuffGuaranteeConfig(configFile);
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            Type configType = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
            Map<String, Map<String, Object>> loadedRules = GSON.fromJson(reader, configType);
            TIER_BUFF_GUARANTEE_RULES.clear();
            initGuaranteeRule(loadedRules, "tier1");
            initGuaranteeRule(loadedRules, "tier2");
            initGuaranteeRule(loadedRules, "tier3");
        } catch (IOException e) {
            setDefaultGuaranteeRules();
        }
    }
    private static void initGuaranteeRule(Map<String, Map<String, Object>> loadedRules, String tierKey) {
        Map<String, Object> defaultRule = getDefaultGuaranteeRule(tierKey);
        Map<String, Object> loadedRule = loadedRules.getOrDefault(tierKey, new HashMap<>());
        Map<String, Object> finalRule = new HashMap<>();
        finalRule.put("guaranteed_min", loadedRule.getOrDefault("guaranteed_min", defaultRule.get("guaranteed_min")));
        finalRule.put("chain_init_prob", loadedRule.getOrDefault("chain_init_prob", defaultRule.get("chain_init_prob")));
        finalRule.put("chain_next_prob", loadedRule.getOrDefault("chain_next_prob", defaultRule.get("chain_next_prob")));
        Map<String, String> defaultForceBuffs = (Map<String, String>) defaultRule.get("force_buffs");
        Map<String, String> loadedForceBuffs = new HashMap<>();
        if (loadedRule.containsKey("force_buffs") && loadedRule.get("force_buffs") instanceof Map<?, ?>) {
            Map<?, ?> rawLoaded = (Map<?, ?>) loadedRule.get("force_buffs");
            for (Map.Entry<?, ?> entry : rawLoaded.entrySet()) {
                String buffName = entry.getKey() instanceof String ? (String) entry.getKey() : "";
                String levelRule = entry.getValue() instanceof String ? (String) entry.getValue() : "random";
                if (!buffName.isEmpty()) {
                    loadedForceBuffs.put(buffName, levelRule);
                }
            }
        }
        Map<String, String> finalForceBuffs = new HashMap<>(defaultForceBuffs);
        finalForceBuffs.putAll(loadedForceBuffs);
        finalRule.put("force_buffs", finalForceBuffs);
        TIER_BUFF_GUARANTEE_RULES.put(tierKey, finalRule);
    }
    private static Map<String, Object> getDefaultGuaranteeRule(String tierKey) {
        Map<String, Object> defaultRule = new HashMap<>();
        switch (tierKey) {
            case "tier1":
                Map<String, String> tier1ForceBuffs = new HashMap<>();
                tier1ForceBuffs.put("life", "random");
                defaultRule.put("force_buffs", tier1ForceBuffs);
                defaultRule.put("guaranteed_min", 2);
                defaultRule.put("chain_init_prob", 0.5f);
                defaultRule.put("chain_next_prob", 0.35f);
                break;
            case "tier2":
                defaultRule.put("force_buffs", new HashMap<String, String>());
                defaultRule.put("guaranteed_min", 3);
                defaultRule.put("chain_init_prob", 0.5f);
                defaultRule.put("chain_next_prob", 0.35f);
                break;
            case "tier3":
                defaultRule.put("force_buffs", new HashMap<String, String>());
                defaultRule.put("guaranteed_min", 5);
                defaultRule.put("chain_init_prob", 0.5f);
                defaultRule.put("chain_next_prob", 0.35f);
                break;
            default:
                defaultRule.put("force_buffs", new HashMap<String, String>());
                defaultRule.put("guaranteed_min", 1);
                defaultRule.put("chain_init_prob", 0.5f);
                defaultRule.put("chain_next_prob", 0.35f);
        }
        return defaultRule;
    }
    private static void createDefaultBuffGuaranteeConfig(File configFile) {
        Map<String, Map<String, Object>> defaultConfig = new HashMap<>();
        Map<String, Object> tier1Rule = new HashMap<>();
        Map<String, String> tier1ForceBuffs = new HashMap<>();
        tier1ForceBuffs.put("life", "random");
        tier1Rule.put("force_buffs", tier1ForceBuffs);
        tier1Rule.put("guaranteed_min", 2);
        tier1Rule.put("chain_init_prob", 0.5f);
        tier1Rule.put("chain_next_prob", 0.35f);
        defaultConfig.put("tier1", tier1Rule);
        Map<String, Object> tier2Rule = new HashMap<>();
        tier2Rule.put("force_buffs", new HashMap<String, String>());
        tier2Rule.put("guaranteed_min", 3);
        tier2Rule.put("chain_init_prob", 0.5f);
        tier2Rule.put("chain_next_prob", 0.35f);
        defaultConfig.put("tier2", tier2Rule);
        Map<String, Object> tier3Rule = new HashMap<>();
        tier3Rule.put("force_buffs", new HashMap<String, String>());
        tier3Rule.put("guaranteed_min", 6);
        tier3Rule.put("chain_init_prob", 0.6f);
        tier3Rule.put("chain_next_prob", 0.40f);
        defaultConfig.put("tier3", tier3Rule);
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gsonWithComment = new GsonBuilder().setPrettyPrinting().create();
            String jsonWithComment = gsonWithComment.toJson(defaultConfig);
            writer.write(jsonWithComment);
        } catch (IOException e) {
        }
    }
    private static void setDefaultGuaranteeRules() {
        TIER_BUFF_GUARANTEE_RULES.clear();
        TIER_BUFF_GUARANTEE_RULES.put("tier1", getDefaultGuaranteeRule("tier1"));
        TIER_BUFF_GUARANTEE_RULES.put("tier2", getDefaultGuaranteeRule("tier2"));
        TIER_BUFF_GUARANTEE_RULES.put("tier3", getDefaultGuaranteeRule("tier3"));
    }
    public static Map<String, Object> getGuaranteeRuleByTier(int tier) {
        String tierKey = "tier" + tier;
        return TIER_BUFF_GUARANTEE_RULES.getOrDefault(tierKey, getDefaultGuaranteeRule(tierKey));
    }
    private static void loadGiftBuffConfig() {
        File configFile = CONFIG_DIR.resolve("gift_buffs.json").toFile();
        if (!configFile.exists()) {
            createDefaultGiftBuffConfig(configFile);
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            Type rootType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> giftConfigRoot = GSON.fromJson(reader, rootType);
            Map<String, Object> greenGiftNode = (Map<String, Object>) giftConfigRoot.getOrDefault("green_gift", new HashMap<>());
            GREEN_GIFT_BUFF_COUNT = getIntFromConfig(
                    greenGiftNode,
                    "buff_count",
                    "green_gift.buff_count",
                    1
            );
            Map<String, Object> blueGiftNode = (Map<String, Object>) giftConfigRoot.getOrDefault("blue_gift", new HashMap<>());
            BLUE_GIFT_BUFF_COUNT = getIntFromConfig(
                    blueGiftNode,
                    "buff_count",
                    "blue_gift.buff_count",
                    1
            );
            Map<String, Object> redGiftNode = (Map<String, Object>) giftConfigRoot.getOrDefault("red_gift", new HashMap<>());
            RED_GIFT_BUFF_COUNT = getIntFromConfig(
                    redGiftNode,
                    "buff_count",
                    "red_gift.buff_count",
                    1
            );
            loadSingleGiftConfig(giftConfigRoot, "green_gift", new TypeToken<Map<String, List<Number>>>() {}.getType(), new TypeToken<List<String>>() {}.getType());
            loadSingleGiftConfig(giftConfigRoot, "blue_gift", new TypeToken<Map<String, List<Number>>>() {}.getType(), new TypeToken<List<String>>() {}.getType());
            loadSingleGiftConfig(
                    giftConfigRoot,
                    "red_gift",
                    new TypeToken<Map<String, List<Number>>>() {}.getType(),
                    new TypeToken<List<String>>() {}.getType()
            );
            loadRedGiftHardConfig(giftConfigRoot);
        } catch (IOException e) {
            resetToDefaultGiftConfig();
        }
    }
    private static int getIntFromConfig(Map<String, Object> configMap, String configKey, String logKey, int defaultValue) {
        try {
            if (configMap == null || !configMap.containsKey(configKey)) {
                return defaultValue;
            }
            Object valueObj = configMap.get(configKey);
            if (valueObj instanceof Number) {
                return ((Number) valueObj).intValue();
            } else if (valueObj instanceof String) {
                return Integer.parseInt((String) valueObj);
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }
    @SuppressWarnings("unchecked")
    private static void loadRedGiftHardConfig(Map<String, Object> giftConfigRoot) {
        if (giftConfigRoot.containsKey("red_gift")) {
            Map<String, Object> redGiftConfig = (Map<String, Object>) giftConfigRoot.get("red_gift");
            if (redGiftConfig.containsKey("hard_ranges")) {
                Type hardRangeType = new TypeToken<Map<String, List<Number>>>() {}.getType();
                Map<String, List<Number>> rawHardRanges = GSON.fromJson(
                        GSON.toJson(redGiftConfig.get("hard_ranges")),
                        hardRangeType
                );
                RED_GIFT_HARD_BUFF_RANGES.clear();
                RED_GIFT_HARD_BUFF_RANGES.putAll(convertNumberRangeToInt(rawHardRanges));
                return;
            }
        }
        loadDefaultRedGiftHardRanges();
    }
    private static void loadDefaultRedGiftHardRanges() {
        RED_GIFT_HARD_BUFF_RANGES.clear();
        RED_GIFT_HARD_BUFF_RANGES.put("harmony", new int[]{15, 45});
        RED_GIFT_HARD_BUFF_RANGES.put("unyielding", new int[]{5, 15});
        RED_GIFT_HARD_BUFF_RANGES.put("thorns", new int[]{10, 25});
        RED_GIFT_HARD_BUFF_RANGES.put("rob", new int[]{5, 15});
        RED_GIFT_HARD_BUFF_RANGES.put("megaforce", new int[]{15, 40});
        RED_GIFT_HARD_BUFF_RANGES.put("thunder", new int[]{20, 50});
        RED_GIFT_HARD_BUFF_RANGES.put("life", new int[]{30, 60});
        RED_GIFT_HARD_BUFF_RANGES.put("aura", new int[]{8,20});
        RED_GIFT_HARD_BUFF_RANGES.put("fasting", new int[]{8,20});
        RED_GIFT_HARD_BUFF_RANGES.put("hunger", new int[]{20, 50});
        RED_GIFT_HARD_BUFF_RANGES.put("phantom", new int[]{6, 8});
        RED_GIFT_HARD_BUFF_RANGES.put("photosynthesis", new int[]{15, 30});
        RED_GIFT_HARD_BUFF_RANGES.put("frost", new int[]{30, 60});
        RED_GIFT_HARD_BUFF_RANGES.put("attack", new int[]{30, 60});
        RED_GIFT_HARD_BUFF_RANGES.put("vampire", new int[]{12, 25});
        RED_GIFT_HARD_BUFF_RANGES.put("curse", new int[]{10, 25});
        RED_GIFT_HARD_BUFF_RANGES.put("death_bomb", new int[]{15, 40});
        RED_GIFT_HARD_BUFF_RANGES.put("ricochet", new int[]{5, 5});
        RED_GIFT_HARD_BUFF_RANGES.put("displacement", new int[]{5, 15});
    }
    @SuppressWarnings("unchecked")
    private static void loadSingleGiftConfig(Map<String, Object> root, String giftKey, Type rangeType, Type buffListType) {
        if (!root.containsKey(giftKey)) {
            return;
        }
        Map<String, Object> singleGiftConfig = (Map<String, Object>) root.get(giftKey);
        List<String> rawBuffList = new ArrayList<>();
        if (singleGiftConfig.containsKey("available_buffs")) {
            Object buffsObj = singleGiftConfig.get("available_buffs");
            if (buffsObj instanceof List<?>) {
                for (Object item : (List<?>) buffsObj) {
                    if (item instanceof String) {
                        rawBuffList.add((String) item);
                    }
                }
            }
        }
        Map<String, List<Number>> rawRanges = GSON.fromJson(
                GSON.toJson(singleGiftConfig.getOrDefault("ranges", new HashMap<>())),
                rangeType
        );
        if ("green_gift".equals(giftKey)) {
            GREEN_GIFT_AVAILABLE_BUFFS.clear();
            GREEN_GIFT_AVAILABLE_BUFFS.addAll(rawBuffList);
            GREEN_GIFT_BUFF_RANGES.clear();
            GREEN_GIFT_BUFF_RANGES.putAll(convertNumberRangeToInt(rawRanges));
        } else if ("blue_gift".equals(giftKey)) {
            BLUE_GIFT_AVAILABLE_BUFFS.clear();
            BLUE_GIFT_AVAILABLE_BUFFS.addAll(rawBuffList);
            BLUE_GIFT_BUFF_RANGES.clear();
            BLUE_GIFT_BUFF_RANGES.putAll(convertNumberRangeToInt(rawRanges));
        } else if ("red_gift".equals(giftKey)) {
            RED_GIFT_AVAILABLE_BUFFS.clear();
            RED_GIFT_AVAILABLE_BUFFS.addAll(rawBuffList);
            RED_GIFT_BUFF_RANGES.clear();
            RED_GIFT_BUFF_RANGES.putAll(convertNumberRangeToInt(rawRanges));
        }
    }
    private static Map<String, int[]> convertNumberRangeToInt(Map<String, List<Number>> rawRanges) {
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, List<Number>> entry : rawRanges.entrySet()) {
            String buffKey = entry.getKey();
            List<Number> rangeList = entry.getValue();
            if (rangeList == null || rangeList.size() < 2) {
                result.put(buffKey, new int[]{1, 1});
                continue;
            }
            int min = rangeList.get(0).intValue();
            int max = rangeList.get(1).intValue();
            result.put(buffKey, new int[]{Math.max(1, min), Math.max(min, max)});
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    public static Map<String, String> getForcedBuffLevelRules(int tier) {
        String tierKey = "tier" + tier;
        Map<String, Object> rule = TIER_BUFF_GUARANTEE_RULES.getOrDefault(tierKey, getDefaultGuaranteeRule(tierKey));
        return (Map<String, String>) rule.get("force_buffs");
    }
    private static void createDefaultGiftBuffConfig(File configFile) {
        Map<String, Object> defaultGiftConfig = new HashMap<>();
        Map<String, Object> greenGiftConfig = new HashMap<>();
        greenGiftConfig.put("buff_count", 1);
        List<String> greenGiftBuffs = Arrays.asList("frost", "life", "attack", "megaforce", "vampire", "hunger", "phantom", "photosynthesis");
        Map<String, List<Number>> greenGiftRanges = new HashMap<>();
        greenGiftRanges.put("frost", Arrays.asList(1, 10));
        greenGiftRanges.put("life", Arrays.asList(1, 10));
        greenGiftRanges.put("attack", Arrays.asList(1, 10));
        greenGiftRanges.put("megaforce", Arrays.asList(1, 5));
        greenGiftRanges.put("vampire", Arrays.asList(1, 3));
        greenGiftRanges.put("hunger", Arrays.asList(1, 5));
        greenGiftRanges.put("phantom", Arrays.asList(1, 1));
        greenGiftRanges.put("photosynthesis", Arrays.asList(1, 2));
        greenGiftConfig.put("available_buffs", greenGiftBuffs);
        greenGiftConfig.put("ranges", greenGiftRanges);
        Map<String, Object> blueGiftConfig = new HashMap<>();
        blueGiftConfig.put("buff_count", 1);
        List<String> blueGiftBuffs = Arrays.asList("frost", "life", "attack", "megaforce", "vampire", "rob", "thunder", "ricochet", "harmony", "curse", "thorns", "hunger", "displacement", "death_bomb", "unyielding", "phantom", "photosynthesis","aura","fasting");
        Map<String, List<Number>> blueGiftRanges = new HashMap<>();
        blueGiftRanges.put("frost", Arrays.asList(5, 20));
        blueGiftRanges.put("life", Arrays.asList(5, 20));
        blueGiftRanges.put("attack", Arrays.asList(5, 20));
        blueGiftRanges.put("megaforce", Arrays.asList(3, 10));
        blueGiftRanges.put("vampire", Arrays.asList(3, 7));
        blueGiftRanges.put("rob", Arrays.asList(1, 3));
        blueGiftRanges.put("displacement", Arrays.asList(1, 3));
        blueGiftRanges.put("thunder", Arrays.asList(3, 10));
        blueGiftRanges.put("ricochet", Arrays.asList(1, 1));
        blueGiftRanges.put("aura", Arrays.asList(1, 5));
        blueGiftRanges.put("fasting", Arrays.asList(1, 5));
        blueGiftRanges.put("harmony", Arrays.asList(1, 5));
        blueGiftRanges.put("curse", Arrays.asList(1, 5));
        blueGiftRanges.put("thorns", Arrays.asList(1, 5));
        blueGiftRanges.put("hunger", Arrays.asList(6, 10));
        blueGiftRanges.put("death_bomb", Arrays.asList(6, 10));
        blueGiftRanges.put("unyielding", Arrays.asList(1, 1));
        blueGiftRanges.put("phantom", Arrays.asList(3, 4));
        blueGiftRanges.put("photosynthesis", Arrays.asList(3, 7));
        blueGiftConfig.put("available_buffs", blueGiftBuffs);
        blueGiftConfig.put("ranges", blueGiftRanges);
        Map<String, Object> redGiftConfig = new HashMap<>();
        List<String> redGiftBuffs = Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "hunger", "displacement",
                "death_bomb", "unyielding", "phantom", "photosynthesis","aura","fasting"
        );
        Map<String, List<Number>> redGiftRanges = new HashMap<>();
        redGiftConfig.put("buff_count", 1);
        redGiftRanges.put("frost", Arrays.asList(20, 50));
        redGiftRanges.put("life", Arrays.asList(20, 50));
        redGiftRanges.put("attack", Arrays.asList(20, 50));
        redGiftRanges.put("megaforce", Arrays.asList(10, 30));
        redGiftRanges.put("vampire", Arrays.asList(8, 20));
        redGiftRanges.put("rob", Arrays.asList(3, 10));
        redGiftRanges.put("aura", Arrays.asList(5, 12));
        redGiftRanges.put("fasting", Arrays.asList(5, 12));
        redGiftRanges.put("displacement", Arrays.asList(3, 10));
        redGiftRanges.put("thunder", Arrays.asList(10, 40));
        redGiftRanges.put("ricochet", Arrays.asList(3, 3));
        redGiftRanges.put("harmony", Arrays.asList(10, 40));
        redGiftRanges.put("curse", Arrays.asList(6, 15));
        redGiftRanges.put("thorns", Arrays.asList(6, 50));
        redGiftRanges.put("hunger", Arrays.asList(15, 40));
        redGiftRanges.put("death_bomb", Arrays.asList(10, 30));
        redGiftRanges.put("unyielding", Arrays.asList(4, 7));
        redGiftRanges.put("phantom", Arrays.asList(5, 6));
        redGiftRanges.put("photosynthesis", Arrays.asList(8, 15));
        Map<String, List<Number>> hardRanges = new HashMap<>();
        hardRanges.put("frost", Arrays.asList(30, 60));
        hardRanges.put("life", Arrays.asList(30, 60));
        hardRanges.put("attack", Arrays.asList(30, 60));
        hardRanges.put("megaforce", Arrays.asList(15, 40));
        hardRanges.put("vampire", Arrays.asList(12, 25));
        hardRanges.put("rob", Arrays.asList(5, 15));
        hardRanges.put("displacement", Arrays.asList(5, 15));
        hardRanges.put("thunder", Arrays.asList(20, 50));
        hardRanges.put("aura", Arrays.asList(8,20));
        hardRanges.put("fasting", Arrays.asList(8,20));
        hardRanges.put("ricochet", Arrays.asList(5, 5));
        hardRanges.put("harmony", Arrays.asList(15, 45));
        hardRanges.put("curse", Arrays.asList(10, 25));
        hardRanges.put("thorns", Arrays.asList(10, 25));
        hardRanges.put("hunger", Arrays.asList(20, 50));
        hardRanges.put("death_bomb", Arrays.asList(15, 40));
        hardRanges.put("unyielding", Arrays.asList(5, 15));
        hardRanges.put("phantom", Arrays.asList(6, 8));
        hardRanges.put("photosynthesis", Arrays.asList(15, 30));
        redGiftConfig.put("available_buffs", redGiftBuffs);
        redGiftConfig.put("ranges", redGiftRanges);
        redGiftConfig.put("hard_ranges", hardRanges);
        defaultGiftConfig.put("green_gift", greenGiftConfig);
        defaultGiftConfig.put("blue_gift", blueGiftConfig);
        defaultGiftConfig.put("red_gift", redGiftConfig);
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(defaultGiftConfig, writer);
        } catch (IOException e) {}
    }
    private static void resetToDefaultGiftConfig() {
        GREEN_GIFT_AVAILABLE_BUFFS.clear();
        GREEN_GIFT_BUFF_RANGES.clear();
        GREEN_GIFT_AVAILABLE_BUFFS.addAll(Arrays.asList("frost", "life", "attack", "megaforce", "vampire", "hunger", "phantom", "photosynthesis"));
        GREEN_GIFT_BUFF_RANGES.put("frost", new int[]{1, 10});
        GREEN_GIFT_BUFF_RANGES.put("life", new int[]{1, 10});
        GREEN_GIFT_BUFF_RANGES.put("attack", new int[]{1, 10});
        GREEN_GIFT_BUFF_RANGES.put("megaforce", new int[]{1, 5});
        GREEN_GIFT_BUFF_RANGES.put("vampire", new int[]{1, 3});
        GREEN_GIFT_BUFF_RANGES.put("hunger", new int[]{1, 5});
        GREEN_GIFT_BUFF_RANGES.put("phantom", new int[]{1, 1});
        GREEN_GIFT_BUFF_RANGES.put("photosynthesis", new int[]{1, 2});
        BLUE_GIFT_AVAILABLE_BUFFS.clear();
        BLUE_GIFT_BUFF_RANGES.clear();
        BLUE_GIFT_AVAILABLE_BUFFS.addAll(Arrays.asList("frost", "life", "attack", "megaforce", "vampire", "rob", "thunder", "ricochet", "harmony", "curse", "thorns", "hunger", "displacement", "death_bomb", "unyielding", "phantom", "photosynthesis","aura","fasting"));
        BLUE_GIFT_BUFF_RANGES.put("frost", new int[]{5, 20});
        BLUE_GIFT_BUFF_RANGES.put("life", new int[]{5, 20});
        BLUE_GIFT_BUFF_RANGES.put("attack", new int[]{5, 20});
        BLUE_GIFT_BUFF_RANGES.put("megaforce", new int[]{3, 10});
        BLUE_GIFT_BUFF_RANGES.put("vampire", new int[]{3, 7});
        BLUE_GIFT_BUFF_RANGES.put("rob", new int[]{1, 3});
        BLUE_GIFT_BUFF_RANGES.put("aura", new int[]{1, 1});
        BLUE_GIFT_BUFF_RANGES.put("fasting", new int[]{1, 1});
        BLUE_GIFT_BUFF_RANGES.put("displacement", new int[]{1, 3});
        BLUE_GIFT_BUFF_RANGES.put("thunder", new int[]{3, 10});
        BLUE_GIFT_BUFF_RANGES.put("ricochet", new int[]{1, 1});
        BLUE_GIFT_BUFF_RANGES.put("harmony", new int[]{1, 5});
        BLUE_GIFT_BUFF_RANGES.put("curse", new int[]{1, 5});
        BLUE_GIFT_BUFF_RANGES.put("thorns", new int[]{1, 5});
        BLUE_GIFT_BUFF_RANGES.put("hunger", new int[]{6, 10});
        BLUE_GIFT_BUFF_RANGES.put("death_bomb", new int[]{6, 10});
        BLUE_GIFT_BUFF_RANGES.put("unyielding", new int[]{1, 1});
        BLUE_GIFT_BUFF_RANGES.put("phantom", new int[]{3, 4});
        BLUE_GIFT_BUFF_RANGES.put("photosynthesis", new int[]{3, 7});
        RED_GIFT_AVAILABLE_BUFFS.clear();
        RED_GIFT_BUFF_RANGES.clear();
        RED_GIFT_AVAILABLE_BUFFS.addAll(Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "hunger", "displacement",
                "death_bomb", "unyielding", "phantom", "photosynthesis","aura","fasting"
        ));
        RED_GIFT_BUFF_RANGES.put("frost", new int[]{20, 50});
        RED_GIFT_BUFF_RANGES.put("life", new int[]{20, 50});
        RED_GIFT_BUFF_RANGES.put("attack", new int[]{20, 50});
        RED_GIFT_BUFF_RANGES.put("megaforce", new int[]{10, 30});
        RED_GIFT_BUFF_RANGES.put("vampire", new int[]{8, 20});
        RED_GIFT_BUFF_RANGES.put("rob", new int[]{3, 10});
        RED_GIFT_BUFF_RANGES.put("displacement", new int[]{3, 10});
        RED_GIFT_BUFF_RANGES.put("thunder", new int[]{10, 40});
        RED_GIFT_BUFF_RANGES.put("ricochet", new int[]{3, 3});
        RED_GIFT_BUFF_RANGES.put("harmony", new int[]{10, 40});
        RED_GIFT_BUFF_RANGES.put("aura", new int[]{2,10});
        RED_GIFT_BUFF_RANGES.put("fasting", new int[]{2,10});
        RED_GIFT_BUFF_RANGES.put("curse", new int[]{6, 15});
        RED_GIFT_BUFF_RANGES.put("thorns", new int[]{6, 50});
        RED_GIFT_BUFF_RANGES.put("hunger", new int[]{15, 40});
        RED_GIFT_BUFF_RANGES.put("death_bomb", new int[]{10, 30});
        RED_GIFT_BUFF_RANGES.put("unyielding", new int[]{4, 7});
        RED_GIFT_BUFF_RANGES.put("phantom", new int[]{5, 6});
        RED_GIFT_BUFF_RANGES.put("photosynthesis", new int[]{8, 15});
        GREEN_GIFT_BUFF_COUNT = 1;
        BLUE_GIFT_BUFF_COUNT = 1;
        RED_GIFT_BUFF_COUNT = 1;
        loadDefaultRedGiftHardRanges();
    }
    private static void loadProbabilityConfig() {
        File configFile = CONFIG_DIR.resolve("probabilities.json").toFile();
        if (!configFile.exists()) {
            createDefaultProbabilityConfig(configFile);
        }
        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> probabilities = GSON.fromJson(reader, type);
            if (probabilities.containsKey("enhancer_chance")) {
                Object enhancerChanceObj = probabilities.get("enhancer_chance");
                if (enhancerChanceObj instanceof Map) {
                    Type chanceType = new TypeToken<Map<String, Float>>() {}.getType();
                    Map<String, Float> chanceMap = GSON.fromJson(GSON.toJson(enhancerChanceObj), chanceType);
                    ENHANCER_CHANCES.clear();
                    ENHANCER_CHANCES.putAll(chanceMap);
                } else if (enhancerChanceObj instanceof Number) {
                    float chance = ((Number) enhancerChanceObj).floatValue();
                    ENHANCER_CHANCES.clear();
                    ENHANCER_CHANCES.put("easy", chance);
                    ENHANCER_CHANCES.put("normal", chance);
                    ENHANCER_CHANCES.put("hard", chance);
                }
            }
            LEVEL_ONE_CHANCE = getFloat(probabilities, "level_one_chance", 0.8f);
            LEVEL_TWO_CHANCE = getFloat(probabilities, "level_two_chance", 0.15f);
            GREEN_GIFT_DROP_CHANCE = getFloat(probabilities, "green_gift_drop_chance", 0.3f);
            BLUE_GIFT_DROP_CHANCE = getFloat(probabilities, "blue_gift_drop_chance", 0.25f);
            RED_GIFT_DROP_CHANCE = getFloat(probabilities, "red_gift_drop_chance", 0.2f);
            ENHANCE_STONE_DROP_CHANCE_TIER1 = getFloat(probabilities, "enhance_stone_drop_chance_tier1", 0.02f);
            ENHANCE_STONE_DROP_CHANCE_TIER2 = getFloat(probabilities, "enhance_stone_drop_chance_tier2", 0.05f);
            ENHANCE_STONE_DROP_CHANCE_TIER3 = getFloat(probabilities, "enhance_stone_drop_chance_tier3", 0.1f);
        } catch (IOException e) {
            System.err.println("Failed to load probability config: " + e.getMessage());
            ENHANCER_CHANCES.clear();
            ENHANCER_CHANCES.put("easy", 0.25f);
            ENHANCER_CHANCES.put("normal", 0.3f);
            ENHANCER_CHANCES.put("hard", 0.5f);
        }
    }
    private static void loadBuffPoolConfig() {
        File configFile = CONFIG_DIR.resolve("buff_pools.json").toFile();
        if (!configFile.exists() || isOldBuffPoolConfig(configFile)) {
            createDefaultBuffPoolConfig(configFile);
        }
        try (FileReader reader = new FileReader(configFile)) {
            Type rootType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> buffPoolsRoot = GSON.fromJson(reader, rootType);
            TIER_ONE_BUFFS_BY_DIFFICULTY.clear();
            TIER_ONE_BAN_BY_DIFFICULTY.clear();
            TIER_TWO_BUFFS_BY_DIFFICULTY.clear();
            TIER_TWO_BAN_BY_DIFFICULTY.clear();
            TIER_THREE_BUFFS_BY_DIFFICULTY.clear();
            TIER_THREE_BAN_BY_DIFFICULTY.clear();
            parseTierBuffPool(buffPoolsRoot, "tier_one_buffs_by_difficulty", TIER_ONE_BUFFS_BY_DIFFICULTY, TIER_ONE_BAN_BY_DIFFICULTY);
            parseTierBuffPool(buffPoolsRoot, "tier_two_buffs_by_difficulty", TIER_TWO_BUFFS_BY_DIFFICULTY, TIER_TWO_BAN_BY_DIFFICULTY);
            parseTierBuffPool(buffPoolsRoot, "tier_three_buffs_by_difficulty", TIER_THREE_BUFFS_BY_DIFFICULTY, TIER_THREE_BAN_BY_DIFFICULTY);
            Type tierOneRangesType = new TypeToken<Map<String, Map<String, List<Number>>>>() {}.getType();
            Map<String, Map<String, List<Number>>> tierOneRangesRaw = GSON.fromJson(
                    GSON.toJson(buffPoolsRoot.get("tier_one_ranges_by_difficulty")),
                    tierOneRangesType
            );
            if (tierOneRangesRaw == null) tierOneRangesRaw = new HashMap<>();
            TIER_ONE_RANGES_BY_DIFFICULTY.putAll(parseDifficultyRanges(tierOneRangesRaw));
            Type tierTwoRangesType = new TypeToken<Map<String, Map<String, List<Number>>>>() {}.getType();
            Map<String, Map<String, List<Number>>> tierTwoRangesRaw = GSON.fromJson(
                    GSON.toJson(buffPoolsRoot.get("tier_two_ranges_by_difficulty")),
                    tierTwoRangesType
            );
            if (tierTwoRangesRaw == null) tierTwoRangesRaw = new HashMap<>();
            TIER_TWO_RANGES_BY_DIFFICULTY.putAll(parseDifficultyRanges(tierTwoRangesRaw));
            Type tierThreeRangesType = new TypeToken<Map<String, Map<String, List<Number>>>>() {}.getType();
            Map<String, Map<String, List<Number>>> tierThreeRangesRaw = GSON.fromJson(
                    GSON.toJson(buffPoolsRoot.get("tier_three_ranges_by_difficulty")),
                    tierThreeRangesType
            );
            if (tierThreeRangesRaw == null) tierThreeRangesRaw = new HashMap<>();
            TIER_THREE_RANGES_BY_DIFFICULTY.putAll(parseDifficultyRanges(tierThreeRangesRaw));
        } catch (IOException e) {
            createDefaultBuffPoolConfig(CONFIG_DIR.resolve("buff_pools.json").toFile());
        }
    }
    @SuppressWarnings("unchecked")
    private static void parseTierBuffPool(Map<String, Object> root, String configKey,
                                          Map<String, List<String>> buffsMap,
                                          Map<String, List<String>> banMap) {
        if (!root.containsKey(configKey)) {
            return;
        }
        Object configObj = root.get(configKey);
        if (!(configObj instanceof Map)) {
            return;
        }
        Map<String, Object> rawPool = (Map<String, Object>) configObj;
        for (Map.Entry<String, Object> entry : rawPool.entrySet()) {
            String difficulty = entry.getKey();
            Object value = entry.getValue();
            List<String> buffsList = new ArrayList<>();
            List<String> banList = new ArrayList<>();
            if (value instanceof Map) {
                Map<String, Object> configObj2 = (Map<String, Object>) value;
                if (configObj2.containsKey("buffs") && configObj2.get("buffs") instanceof List) {
                    List<?> rawBuffs = (List<?>) configObj2.get("buffs");
                    for (Object item : rawBuffs) {
                        if (item instanceof String) {
                            buffsList.add((String) item);
                        }
                    }
                }
                if (configObj2.containsKey("ban") && configObj2.get("ban") instanceof List) {
                    List<?> rawBan = (List<?>) configObj2.get("ban");
                    for (Object item : rawBan) {
                        if (item instanceof String) {
                            banList.add((String) item);
                        }
                    }
                }
            } else if (value instanceof List) {
                List<?> rawBuffs = (List<?>) value;
                for (Object item : rawBuffs) {
                    if (item instanceof String) {
                        buffsList.add((String) item);
                    }
                }
            }
            buffsMap.put(difficulty, buffsList);
            banMap.put(difficulty, banList);
        }
    }
    @SuppressWarnings("unchecked")
    private static void parseBuffPoolWithBan(
            Map<String, Object> root,
            String configKey,
            Map<String, List<String>> buffsMap,
            Map<String, List<String>> banMap) {
        if (!root.containsKey(configKey)) {
            return;
        }
        Object configObj = root.get(configKey);
        if (!(configObj instanceof Map)) {
            return;
        }
        Map<String, Object> rawPool = (Map<String, Object>) configObj;
        buffsMap.clear();
        banMap.clear();
        for (Map.Entry<String, Object> entry : rawPool.entrySet()) {
            String difficulty = entry.getKey();
            Object value = entry.getValue();
            List<String> buffsList = new ArrayList<>();
            List<String> banList = new ArrayList<>();
            if (value instanceof Map) {
                Map<String, Object> configObj2 = (Map<String, Object>) value;
                if (configObj2.containsKey("buffs") && configObj2.get("buffs") instanceof List) {
                    List<?> rawBuffs = (List<?>) configObj2.get("buffs");
                    for (Object item : rawBuffs) {
                        if (item instanceof String) {
                            buffsList.add((String) item);
                        }
                    }
                }
                if (configObj2.containsKey("ban") && configObj2.get("ban") instanceof List) {
                    List<?> rawBan = (List<?>) configObj2.get("ban");
                    for (Object item : rawBan) {
                        if (item instanceof String) {
                            banList.add((String) item);
                        }
                    }
                }
            } else if (value instanceof List) {
                List<?> rawBuffs = (List<?>) value;
                for (Object item : rawBuffs) {
                    if (item instanceof String) {
                        buffsList.add((String) item);
                    }
                }
            }
            buffsMap.put(difficulty, buffsList);
            banMap.put(difficulty, banList);
        }
    }
    private static boolean isOldBuffPoolConfig(File configFile) {
        try (FileReader reader = new FileReader(configFile)) {
            Type rootType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> buffPoolsRoot = GSON.fromJson(reader, rootType);
            return buffPoolsRoot.containsKey("tier_one_buffs")
                    && !buffPoolsRoot.containsKey("tier_one_buffs_by_difficulty");
        } catch (IOException e) {
            return true;
        }
    }
    private static Map<String, Map<String, int[]>> parseDifficultyRanges(Map<String, Map<String, List<Number>>> rawRanges) {
        Map<String, Map<String, int[]>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, List<Number>>> difficultyEntry : rawRanges.entrySet()) {
            String difficulty = difficultyEntry.getKey();
            Map<String, List<Number>> rawTierRanges = difficultyEntry.getValue();
            Map<String, int[]> tierRanges = new HashMap<>();
            for (Map.Entry<String, List<Number>> buffEntry : rawTierRanges.entrySet()) {
                String buffKey = buffEntry.getKey();
                List<Number> rangeList = buffEntry.getValue();
                if (rangeList.size() >= 2) {
                    int min = rangeList.get(0).intValue();
                    int max = rangeList.get(1).intValue();
                    tierRanges.put(buffKey, new int[]{min, max});
                }
            }
            result.put(difficulty, tierRanges);
        }
        return result;
    }
    private static float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
    private static void createDefaultProbabilityConfig(File configFile) {
        Map<String, Object> defaultProbabilities = new HashMap<>();
        Map<String, Float> enhancerChances = new HashMap<>();
        enhancerChances.put("easy", 0.25f);
        enhancerChances.put("normal", 0.3f);
        enhancerChances.put("hard", 0.5f);
        defaultProbabilities.put("enhancer_chance", enhancerChances);
        defaultProbabilities.put("level_one_chance", 0.8f);
        defaultProbabilities.put("level_two_chance", 0.15f);
        defaultProbabilities.put("green_gift_drop_chance", 0.35f);
        defaultProbabilities.put("blue_gift_drop_chance", 0.35f);
        defaultProbabilities.put("red_gift_drop_chance", 0.25f);
        defaultProbabilities.put("enhance_stone_drop_chance_tier1", 0.02f);
        defaultProbabilities.put("enhance_stone_drop_chance_tier2", 0.05f);
        defaultProbabilities.put("enhance_stone_drop_chance_tier3", 0.1f);
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(defaultProbabilities, writer);
        } catch (IOException e) {}
    }
    private static void createDefaultBuffPoolConfig(File configFile) {
        Map<String, Object> defaultBuffPools = new HashMap<>();
        Map<String, Map<String, Object>> tierOneBuffsByDiff = new HashMap<>();
        Map<String, Object> easyTierOne = new HashMap<>();
        easyTierOne.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "hunger",
                "death_bomb", "summon", "phantom", "photosynthesis"
        ));
        easyTierOne.put("ban", Arrays.asList());
        tierOneBuffsByDiff.put("easy", easyTierOne);
        Map<String, Object> normalTierOne = new HashMap<>();
        normalTierOne.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "hunger",
                "death_bomb", "summon", "phantom", "photosynthesis"
        ));
        normalTierOne.put("ban", Arrays.asList());
        tierOneBuffsByDiff.put("normal", normalTierOne);
        Map<String, Object> hardTierOne = new HashMap<>();
        hardTierOne.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "hunger",
                "death_bomb", "summon", "phantom", "photosynthesis"
        ));
        hardTierOne.put("ban", Arrays.asList());
        tierOneBuffsByDiff.put("hard", hardTierOne);
        defaultBuffPools.put("tier_one_buffs_by_difficulty", tierOneBuffsByDiff);
        Map<String, Map<String, Object>> tierTwoBuffsByDiff = new HashMap<>();
        Map<String, Object> easyTierTwo = new HashMap<>();
        easyTierTwo.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "hunger", "displacement",
                "death_bomb", "unyielding", "summon", "phantom", "photosynthesis","fasting","aura"
        ));
        easyTierTwo.put("ban", Arrays.asList());
        tierTwoBuffsByDiff.put("easy", easyTierTwo);
        Map<String, Object> normalTierTwo = new HashMap<>();
        normalTierTwo.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "hunger", "displacement",
                "death_bomb", "unyielding", "summon", "phantom", "photosynthesis","fasting","aura"
        ));
        normalTierTwo.put("ban", Arrays.asList());
        tierTwoBuffsByDiff.put("normal", normalTierTwo);
        Map<String, Object> hardTierTwo = new HashMap<>();
        hardTierTwo.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "hunger", "displacement",
                "death_bomb", "unyielding", "summon", "phantom", "photosynthesis","fasting","aura"
        ));
        hardTierTwo.put("ban", Arrays.asList());
        tierTwoBuffsByDiff.put("hard", hardTierTwo);
        defaultBuffPools.put("tier_two_buffs_by_difficulty", tierTwoBuffsByDiff);
        Map<String, Map<String, Object>> tierThreeBuffsByDiff = new HashMap<>();
        Map<String, Object> easyTierThree = new HashMap<>();
        easyTierThree.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "aura", "hunger",
                "displacement", "death_bomb", "tracking", "photosynthesis",
                "unyielding", "summon", "phantom","fasting"
        ));
        easyTierThree.put("ban", Arrays.asList());
        tierThreeBuffsByDiff.put("easy", easyTierThree);
        Map<String, Object> normalTierThree = new HashMap<>();
        normalTierThree.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "aura", "hunger",
                "displacement", "death_bomb", "tracking", "photosynthesis",
                "unyielding", "summon", "phantom","fasting"
        ));
        normalTierThree.put("ban", Arrays.asList());
        tierThreeBuffsByDiff.put("normal", normalTierThree);
        Map<String, Object> hardTierThree = new HashMap<>();
        hardTierThree.put("buffs", Arrays.asList(
                "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                "ricochet", "harmony", "curse", "thorns", "aura", "hunger",
                "displacement", "death_bomb", "tracking", "photosynthesis",
                "unyielding", "summon", "phantom","fasting"
        ));
        hardTierThree.put("ban", Arrays.asList());
        tierThreeBuffsByDiff.put("hard", hardTierThree);
        defaultBuffPools.put("tier_three_buffs_by_difficulty", tierThreeBuffsByDiff);
        Map<String, Map<String, List<Number>>> tierOneRangesByDiff = new HashMap<>();
        Map<String, List<Number>> easyTierOneRanges = new HashMap<>();
        easyTierOneRanges.put("frost", Arrays.asList(1, 7));
        easyTierOneRanges.put("life", Arrays.asList(1, 7));
        easyTierOneRanges.put("attack", Arrays.asList(1, 7));
        easyTierOneRanges.put("megaforce", Arrays.asList(1, 3));
        easyTierOneRanges.put("vampire", Arrays.asList(1, 2));
        easyTierOneRanges.put("hunger", Arrays.asList(1, 3));
        easyTierOneRanges.put("death_bomb", Arrays.asList(1, 3));
        easyTierOneRanges.put("summon", Arrays.asList(1, 7));
        easyTierOneRanges.put("phantom", Arrays.asList(1, 2));
        easyTierOneRanges.put("photosynthesis", Arrays.asList(1, 2));
        tierOneRangesByDiff.put("easy", easyTierOneRanges);
        Map<String, List<Number>> normalTierOneRanges = new HashMap<>();
        normalTierOneRanges.put("frost", Arrays.asList(1, 10));
        normalTierOneRanges.put("life", Arrays.asList(1, 10));
        normalTierOneRanges.put("attack", Arrays.asList(1, 10));
        normalTierOneRanges.put("megaforce", Arrays.asList(1, 5));
        normalTierOneRanges.put("vampire", Arrays.asList(1, 3));
        normalTierOneRanges.put("hunger", Arrays.asList(1, 5));
        normalTierOneRanges.put("death_bomb", Arrays.asList(1, 5));
        normalTierOneRanges.put("summon", Arrays.asList(5, 10));
        normalTierOneRanges.put("phantom", Arrays.asList(1, 2));
        normalTierOneRanges.put("photosynthesis", Arrays.asList(1, 2));
        tierOneRangesByDiff.put("normal", normalTierOneRanges);
        Map<String, List<Number>> hardTierOneRanges = new HashMap<>();
        hardTierOneRanges.put("frost", Arrays.asList(5, 12));
        hardTierOneRanges.put("life", Arrays.asList(5, 12));
        hardTierOneRanges.put("attack", Arrays.asList(5, 12));
        hardTierOneRanges.put("megaforce", Arrays.asList(3, 7));
        hardTierOneRanges.put("vampire", Arrays.asList(3, 8));
        hardTierOneRanges.put("hunger", Arrays.asList(5, 12));
        hardTierOneRanges.put("death_bomb", Arrays.asList(3, 12));
        hardTierOneRanges.put("summon", Arrays.asList(7, 15));
        hardTierOneRanges.put("phantom", Arrays.asList(3, 4));
        hardTierOneRanges.put("photosynthesis", Arrays.asList(3, 5));
        tierOneRangesByDiff.put("hard", hardTierOneRanges);
        defaultBuffPools.put("tier_one_ranges_by_difficulty", tierOneRangesByDiff);
        Map<String, Map<String, List<Number>>> tierTwoRangesByDiff = new HashMap<>();
        Map<String, List<Number>> easyTierTwoRanges = new HashMap<>();
        easyTierTwoRanges.put("frost", Arrays.asList(5, 15));
        easyTierTwoRanges.put("life", Arrays.asList(5, 15));
        easyTierTwoRanges.put("attack", Arrays.asList(5, 15));
        easyTierTwoRanges.put("megaforce", Arrays.asList(3, 7));
        easyTierTwoRanges.put("vampire", Arrays.asList(3, 5));
        easyTierTwoRanges.put("rob", Arrays.asList(1, 2));
        easyTierTwoRanges.put("aura", Arrays.asList(1, 3));
        easyTierTwoRanges.put("displacement", Arrays.asList(1, 1));
        easyTierTwoRanges.put("thunder", Arrays.asList(3, 7));
        easyTierTwoRanges.put("ricochet", Arrays.asList(1, 1));
        easyTierTwoRanges.put("harmony", Arrays.asList(1, 3));
        easyTierTwoRanges.put("curse", Arrays.asList(1, 3));
        easyTierTwoRanges.put("thorns", Arrays.asList(1, 3));
        easyTierTwoRanges.put("hunger", Arrays.asList(5, 7));
        easyTierTwoRanges.put("death_bomb", Arrays.asList(5, 8));
        easyTierTwoRanges.put("unyielding", Arrays.asList(1, 1));
        easyTierTwoRanges.put("summon", Arrays.asList(7, 15));
        easyTierTwoRanges.put("phantom", Arrays.asList(2, 4));
        easyTierTwoRanges.put("photosynthesis", Arrays.asList(2, 5));
        easyTierTwoRanges.put("fasting", Arrays.asList(1,3));
        tierTwoRangesByDiff.put("easy", easyTierTwoRanges);
        Map<String, List<Number>> normalTierTwoRanges = new HashMap<>();
        normalTierTwoRanges.put("frost", Arrays.asList(5, 20));
        normalTierTwoRanges.put("life", Arrays.asList(5, 20));
        normalTierTwoRanges.put("attack", Arrays.asList(5, 20));
        normalTierTwoRanges.put("megaforce", Arrays.asList(3, 10));
        normalTierTwoRanges.put("vampire", Arrays.asList(3, 7));
        normalTierTwoRanges.put("rob", Arrays.asList(1, 3));
        normalTierTwoRanges.put("aura", Arrays.asList(1, 5));
        normalTierTwoRanges.put("displacement", Arrays.asList(1, 3));
        normalTierTwoRanges.put("thunder", Arrays.asList(3, 10));
        normalTierTwoRanges.put("ricochet", Arrays.asList(1, 1));
        normalTierTwoRanges.put("harmony", Arrays.asList(1, 5));
        normalTierTwoRanges.put("curse", Arrays.asList(1, 5));
        normalTierTwoRanges.put("thorns", Arrays.asList(1, 5));
        normalTierTwoRanges.put("hunger", Arrays.asList(6, 10));
        normalTierTwoRanges.put("death_bomb", Arrays.asList(6, 10));
        normalTierTwoRanges.put("unyielding", Arrays.asList(1, 1));
        normalTierTwoRanges.put("summon", Arrays.asList(10, 18));
        normalTierTwoRanges.put("phantom", Arrays.asList(3, 4));
        normalTierTwoRanges.put("photosynthesis", Arrays.asList(3, 7));
        normalTierTwoRanges.put("fasting", Arrays.asList(1,5));
        tierTwoRangesByDiff.put("normal", normalTierTwoRanges);
        Map<String, List<Number>> hardTierTwoRanges = new HashMap<>();
        hardTierTwoRanges.put("frost", Arrays.asList(10, 25));
        hardTierTwoRanges.put("life", Arrays.asList(10, 25));
        hardTierTwoRanges.put("attack", Arrays.asList(10, 25));
        hardTierTwoRanges.put("megaforce", Arrays.asList(5, 15));
        hardTierTwoRanges.put("aura", Arrays.asList(3,8));
        hardTierTwoRanges.put("vampire", Arrays.asList(5, 10));
        hardTierTwoRanges.put("rob", Arrays.asList(3, 5));
        hardTierTwoRanges.put("displacement", Arrays.asList(3, 5));
        hardTierTwoRanges.put("thunder", Arrays.asList(5, 15));
        hardTierTwoRanges.put("ricochet", Arrays.asList(2, 2));
        hardTierTwoRanges.put("harmony", Arrays.asList(5, 10));
        hardTierTwoRanges.put("curse", Arrays.asList(5, 10));
        hardTierTwoRanges.put("thorns", Arrays.asList(5, 10));
        hardTierTwoRanges.put("hunger", Arrays.asList(10, 25));
        hardTierTwoRanges.put("death_bomb", Arrays.asList(10, 15));
        hardTierTwoRanges.put("unyielding", Arrays.asList(3, 5));
        hardTierTwoRanges.put("summon", Arrays.asList(15, 25));
        hardTierTwoRanges.put("phantom", Arrays.asList(4, 6));
        hardTierTwoRanges.put("photosynthesis", Arrays.asList(7, 15));
        hardTierTwoRanges.put("fasting", Arrays.asList(3,8));
        tierTwoRangesByDiff.put("hard", hardTierTwoRanges);
        defaultBuffPools.put("tier_two_ranges_by_difficulty", tierTwoRangesByDiff);
        Map<String, Map<String, List<Number>>> tierThreeRangesByDiff = new HashMap<>();
        Map<String, List<Number>> easyTierThreeRanges = new HashMap<>();
        easyTierThreeRanges.put("frost", Arrays.asList(16, 40));
        easyTierThreeRanges.put("life", Arrays.asList(16, 40));
        easyTierThreeRanges.put("attack", Arrays.asList(16, 40));
        easyTierThreeRanges.put("megaforce", Arrays.asList(10, 25));
        easyTierThreeRanges.put("vampire", Arrays.asList(8, 16));
        easyTierThreeRanges.put("rob", Arrays.asList(3, 7));
        easyTierThreeRanges.put("aura", Arrays.asList(3,8));
        easyTierThreeRanges.put("displacement", Arrays.asList(3, 7));
        easyTierThreeRanges.put("thunder", Arrays.asList(10, 35));
        easyTierThreeRanges.put("ricochet", Arrays.asList(3, 3));
        easyTierThreeRanges.put("harmony", Arrays.asList(10, 30));
        easyTierThreeRanges.put("curse", Arrays.asList(6, 10));
        easyTierThreeRanges.put("thorns", Arrays.asList(5, 12));
        easyTierThreeRanges.put("hunger", Arrays.asList(15, 30));
        easyTierThreeRanges.put("death_bomb", Arrays.asList(10, 25));
        easyTierThreeRanges.put("tracking", Arrays.asList(1, 1));
        easyTierThreeRanges.put("unyielding", Arrays.asList(3, 5));
        easyTierThreeRanges.put("summon", Arrays.asList(15, 20));
        easyTierThreeRanges.put("phantom", Arrays.asList(4, 6));
        easyTierThreeRanges.put("photosynthesis", Arrays.asList(5, 10));
        easyTierThreeRanges.put("fasting", Arrays.asList(3,8));
        tierThreeRangesByDiff.put("easy", easyTierThreeRanges);
        Map<String, List<Number>> normalTierThreeRanges = new HashMap<>();
        normalTierThreeRanges.put("frost", Arrays.asList(20, 50));
        normalTierThreeRanges.put("life", Arrays.asList(20, 50));
        normalTierThreeRanges.put("attack", Arrays.asList(20, 50));
        normalTierThreeRanges.put("megaforce", Arrays.asList(10, 30));
        normalTierThreeRanges.put("vampire", Arrays.asList(8, 20));
        normalTierThreeRanges.put("rob", Arrays.asList(3, 10));
        normalTierThreeRanges.put("displacement", Arrays.asList(3, 10));
        normalTierThreeRanges.put("aura", Arrays.asList(5,12));
        normalTierThreeRanges.put("thunder", Arrays.asList(10, 40));
        normalTierThreeRanges.put("ricochet", Arrays.asList(3, 3));
        normalTierThreeRanges.put("harmony", Arrays.asList(10, 40));
        normalTierThreeRanges.put("curse", Arrays.asList(6, 15));
        normalTierThreeRanges.put("thorns", Arrays.asList(6, 20));
        normalTierThreeRanges.put("hunger", Arrays.asList(15, 40));
        normalTierThreeRanges.put("death_bomb", Arrays.asList(10, 30));
        normalTierThreeRanges.put("tracking", Arrays.asList(1, 1));
        normalTierThreeRanges.put("unyielding", Arrays.asList(3, 8));
        normalTierThreeRanges.put("summon", Arrays.asList(18, 25));
        normalTierThreeRanges.put("phantom", Arrays.asList(5, 6));
        normalTierThreeRanges.put("photosynthesis", Arrays.asList(8, 15));
        normalTierThreeRanges.put("fasting", Arrays.asList(5,12));
        tierThreeRangesByDiff.put("normal", normalTierThreeRanges);
        Map<String, List<Number>> hardTierThreeRanges = new HashMap<>();
        hardTierThreeRanges.put("frost", Arrays.asList(30, 60));
        hardTierThreeRanges.put("life", Arrays.asList(30, 60));
        hardTierThreeRanges.put("attack", Arrays.asList(30, 60));
        hardTierThreeRanges.put("megaforce", Arrays.asList(15, 40));
        hardTierThreeRanges.put("vampire", Arrays.asList(12, 25));
        hardTierThreeRanges.put("rob", Arrays.asList(8,20));
        hardTierThreeRanges.put("displacement", Arrays.asList(5, 15));
        hardTierThreeRanges.put("thunder", Arrays.asList(20, 50));
        hardTierThreeRanges.put("ricochet", Arrays.asList(5, 5));
        hardTierThreeRanges.put("harmony", Arrays.asList(15, 45));
        hardTierThreeRanges.put("curse", Arrays.asList(10, 25));
        hardTierThreeRanges.put("thorns", Arrays.asList(10, 25));
        hardTierThreeRanges.put("aura", Arrays.asList(5,15));
        hardTierThreeRanges.put("hunger", Arrays.asList(20, 50));
        hardTierThreeRanges.put("death_bomb", Arrays.asList(15, 40));
        hardTierThreeRanges.put("tracking", Arrays.asList(1, 1));
        hardTierThreeRanges.put("unyielding", Arrays.asList(5, 15));
        hardTierThreeRanges.put("summon", Arrays.asList(25, 50));
        hardTierThreeRanges.put("phantom", Arrays.asList(6, 8));
        hardTierThreeRanges.put("photosynthesis", Arrays.asList(15, 30));
        hardTierThreeRanges.put("fasting", Arrays.asList(8,20));
        tierThreeRangesByDiff.put("hard", hardTierThreeRanges);
        defaultBuffPools.put("tier_three_ranges_by_difficulty", tierThreeRangesByDiff);
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(defaultBuffPools, writer);
        } catch (IOException e) {}
    }
}