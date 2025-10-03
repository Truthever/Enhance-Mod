package com.weaponhouse.enhance.common;
import java.util.HashMap;
import java.util.Map;
public class EnhanceCommonRules {
    public static final Map<String, Float> SACRIFICE_HEALTH_BONUS = new HashMap<String, Float>() {{
        put("life", 1.0f);
        put("fasting", 1.0f);
        put("photosynthesis", 0.5f);
        put("vampire", 2.0f);
        put("curse", 2.0f);
        put("unyielding", 5.0f);
    }};
    public static final Map<String, Float> SACRIFICE_ATTACK_BONUS = new HashMap<String, Float>() {{
        put("attack", 0.25f);
        put("megaforce", 0.20f);
        put("rob", 0.5f);
        put("displacement", 0.5f);
        put("thunder", 0.20f);
        put("ricochet", 1.0f);
    }};
    public static final Map<String, Float> SACRIFICE_DEFENSE_BONUS = new HashMap<String, Float>() {{
        put("thorns", 0.25f);
        put("death_bomb", 0.25f);
    }};
    public static final Map<String, Float> SACRIFICE_SPEED_BONUS = new HashMap<String, Float>() {{
        put("frost", 0.005f);
        put("aura", 0.01f);
        put("hunger", 0.005f);
        put("phantom", 0.01f);
    }};
    public static final Map<String, float[]> SACRIFICE_HARMONY_BONUS = new HashMap<String, float[]>() {{
        put("harmony", new float[]{2.0f, 0.5f, 0.25f});
    }};
    public static final Map<String, int[]> GREEN_GIFT_BUFF_RANGES = new HashMap<String, int []>() {{
        put("frost", new int[]{1, 10});
        put("life", new int[]{1, 10});
        put("attack", new int[]{1, 10});
        put("megaforce", new int[]{1, 5});
        put("aura", new int[]{1, 2});
        put("fasting", new int[]{1, 2});
        put("vampire", new int[]{1, 3});
        put("hunger", new int[]{1, 5});
        put("phantom", new int[]{1, 1});
        put("photosynthesis", new int[]{1, 2});
    }};
    public static final Map<String, int[]> BLUE_GIFT_BUFF_RANGES = new HashMap<String, int []>() {{
        put("frost", new int[]{5, 20});
        put("life", new int[]{5, 20});
        put("attack", new int[]{5, 20});
        put("megaforce", new int[]{3, 10});
        put("vampire", new int[]{3, 7});
        put("rob", new int[]{1, 3});
        put("displacement", new int[]{1, 3});
        put("thunder", new int[]{3, 10});
        put("ricochet", new int[]{1, 1});
        put("harmony", new int[]{1, 5});
        put("aura", new int[]{3, 4});
        put("fasting", new int[]{3, 4});
        put("curse", new int[]{1, 5});
        put("thorns", new int[]{1, 5});
        put("hunger", new int[]{6, 10});
        put("death_bomb", new int[]{6, 10});
        put("unyielding", new int[]{1, 1});
        put("phantom", new int[]{3, 4});
        put("photosynthesis", new int[]{3, 7});
    }};
    public static final Map<String, int[]> RED_GIFT_BUFF_RANGES = new HashMap<String, int []>() {{
        put("frost", new int[]{20, 60});
        put("life", new int[]{20, 60});
        put("attack", new int[]{20, 60});
        put("megaforce", new int[]{10, 40});
        put("vampire", new int[]{8, 25});
        put("rob", new int[]{3, 15});
        put("displacement", new int[]{3, 15});
        put("thunder", new int[]{10, 50});
        put("ricochet", new int[]{3, 5});
        put("harmony", new int[]{10, 45});
        put("curse", new int[]{6, 25});
        put("thorns", new int[]{6, 25});
        put("aura", new int[]{5, 20});
        put("fasting", new int[]{5, 20});
        put("hunger", new int[]{15, 50});
        put("death_bomb", new int[]{10, 40});
        put("unyielding", new int[]{4, 15});
        put("phantom", new int[]{5, 8});
        put("photosynthesis", new int[]{8, 25});
    }};
    public static boolean isSacrificeableBuff(String buffId) {
        return SACRIFICE_HEALTH_BONUS.containsKey(buffId)
                || SACRIFICE_ATTACK_BONUS.containsKey(buffId)
                || SACRIFICE_DEFENSE_BONUS.containsKey(buffId)
                || SACRIFICE_SPEED_BONUS.containsKey(buffId)
                || SACRIFICE_HARMONY_BONUS.containsKey(buffId);
    }
}