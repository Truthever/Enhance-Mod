package com.weaponhouse.enhance.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
public class GiftConfigReader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLLoader.getGamePath().resolve("config").resolve("enhance");
    public static GiftConfig readGiftConfig(String giftType) {

        File configFile = CONFIG_DIR.resolve("gift_buffs.json").toFile();
        GiftConfig config = new GiftConfig();
        if (!configFile.exists()) {
            createDefaultGiftConfig(configFile);
        }
        try (FileReader reader = new FileReader(configFile)) {
            Type rootType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> giftConfigRoot = GSON.fromJson(reader, rootType);
            Map<String, Object> giftConfig = (Map<String, Object>) giftConfigRoot.get(giftType);

            if (giftConfig != null) {
                config.buffCount = ((Number) giftConfig.getOrDefault("buff_count", 1)).intValue();
                List<String> availableBuffs = new ArrayList<>();
                if (giftConfig.containsKey("available_buffs")) {
                    List<?> rawBuffs = (List<?>) giftConfig.get("available_buffs");
                    for (Object item : rawBuffs) {
                        if (item instanceof String) {
                            availableBuffs.add((String) item);
                        }
                    }
                }
                config.availableBuffs = availableBuffs;
                Map<String, int[]> ranges = new HashMap<>();
                if (giftConfig.containsKey("ranges")) {
                    Map<String, List<Number>> rawRanges = GSON.fromJson(
                            GSON.toJson(giftConfig.get("ranges")),
                            new TypeToken<Map<String, List<Number>>>() {}.getType()
                    );
                    for (Map.Entry<String, List<Number>> entry : rawRanges.entrySet()) {
                        String buffKey = entry.getKey();
                        List<Number> rangeList = entry.getValue();
                        if (rangeList.size() >= 2) {
                            int min = rangeList.get(0).intValue();
                            int max = rangeList.get(1).intValue();
                            ranges.put(buffKey, new int[]{min, max});
                        }
                    }
                }
                config.buffRanges = ranges;
            } else {
                config = getDefaultGiftConfig(giftType);
            }
        } catch (IOException e) {
            config = getDefaultGiftConfig(giftType);
        }
        return config;
    }
    private static void createDefaultGiftConfig(File configFile) {
        try {
            ConfigLoader.loadConfigs();
        } catch (Exception ignored) {
        }
    }
    private static GiftConfig getDefaultGiftConfig(String giftType) {
        GiftConfig config = new GiftConfig();
        switch (giftType) {
            case "green_gift":
                config.buffCount = 1;
                config.availableBuffs = Arrays.asList(
                        "frost", "life", "attack", "megaforce", "vampire", "hunger", "phantom", "photosynthesis"
                );
                config.buffRanges = new HashMap<>();
                config.buffRanges.put("frost", new int[]{1, 10});
                config.buffRanges.put("life", new int[]{1, 10});
                config.buffRanges.put("attack", new int[]{1, 10});
                config.buffRanges.put("megaforce", new int[]{1, 5});
                config.buffRanges.put("vampire", new int[]{1, 3});
                config.buffRanges.put("hunger", new int[]{1, 5});
                config.buffRanges.put("phantom", new int[]{1, 1});
                config.buffRanges.put("photosynthesis", new int[]{1, 2});
                break;
            case "blue_gift":
                config.buffCount = 1;
                config.availableBuffs = Arrays.asList(
                        "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder", "ricochet",
                        "harmony", "curse", "thorns", "hunger", "displacement", "death_bomb", "unyielding",
                        "phantom", "photosynthesis", "aura", "fasting"
                );
                config.buffRanges = new HashMap<>();
                config.buffRanges.put("frost", new int[]{5, 20});
                config.buffRanges.put("life", new int[]{5, 20});
                config.buffRanges.put("attack", new int[]{5, 20});
                config.buffRanges.put("megaforce", new int[]{3, 10});
                config.buffRanges.put("vampire", new int[]{3, 7});
                config.buffRanges.put("rob", new int[]{1, 3});
                config.buffRanges.put("aura", new int[]{1, 5});
                config.buffRanges.put("fasting", new int[]{1, 5});
                config.buffRanges.put("displacement", new int[]{1, 3});
                config.buffRanges.put("thunder", new int[]{3, 10});
                config.buffRanges.put("ricochet", new int[]{1, 1});
                config.buffRanges.put("harmony", new int[]{1, 5});
                config.buffRanges.put("curse", new int[]{1, 5});
                config.buffRanges.put("thorns", new int[]{1, 5});
                config.buffRanges.put("hunger", new int[]{6, 10});
                config.buffRanges.put("death_bomb", new int[]{6, 10});
                config.buffRanges.put("unyielding", new int[]{1, 1});
                config.buffRanges.put("phantom", new int[]{3, 4});
                config.buffRanges.put("photosynthesis", new int[]{3, 7});
                break;
            case "red_gift":
                config.buffCount = 1;
                config.availableBuffs = Arrays.asList(
                        "frost", "life", "attack", "megaforce", "vampire", "rob", "thunder",
                        "ricochet", "harmony", "curse", "thorns", "hunger", "displacement",
                        "death_bomb", "unyielding", "phantom", "photosynthesis", "aura", "fasting"
                );
                config.buffRanges = new HashMap<>();
                config.buffRanges.put("frost", new int[]{20, 50});
                config.buffRanges.put("life", new int[]{20, 50});
                config.buffRanges.put("attack", new int[]{20, 50});
                config.buffRanges.put("megaforce", new int[]{10, 30});
                config.buffRanges.put("vampire", new int[]{8, 20});
                config.buffRanges.put("rob", new int[]{3, 10});
                config.buffRanges.put("displacement", new int[]{3, 10});
                config.buffRanges.put("thunder", new int[]{10, 40});
                config.buffRanges.put("ricochet", new int[]{3, 3});
                config.buffRanges.put("harmony", new int[]{10, 40});
                config.buffRanges.put("aura", new int[]{5, 12});
                config.buffRanges.put("fasting", new int[]{5, 12});
                config.buffRanges.put("curse", new int[]{6, 15});
                config.buffRanges.put("thorns", new int[]{6, 50});
                config.buffRanges.put("hunger", new int[]{15, 40});
                config.buffRanges.put("death_bomb", new int[]{10, 30});
                config.buffRanges.put("unyielding", new int[]{4, 7});
                config.buffRanges.put("phantom", new int[]{5, 6});
                config.buffRanges.put("photosynthesis", new int[]{8, 15});
                break;
        }
        return config;
    }
    public static class GiftConfig {
        public int buffCount = 1;
        public List<String> availableBuffs = new ArrayList<>();
        public Map<String, int[]> buffRanges = new HashMap<>();
    }
}