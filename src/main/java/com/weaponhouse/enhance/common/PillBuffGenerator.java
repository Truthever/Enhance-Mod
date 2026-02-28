package com.weaponhouse.enhance.common;

import com.weaponhouse.enhance.util.GiftConfigReader;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import java.util.*;
public class PillBuffGenerator {
    private static final Map<String, Integer> BUFF_TYPE_WEIGHTS = new HashMap<>();
    static {
        BUFF_TYPE_WEIGHTS.put("attack", 35);
        BUFF_TYPE_WEIGHTS.put("life", 35);
        BUFF_TYPE_WEIGHTS.put("defense", 15);
        BUFF_TYPE_WEIGHTS.put("speed", 10);
        BUFF_TYPE_WEIGHTS.put("harmony", 5);
    }
    private static final int[] BUFF_COUNTS = {1, 2, 3};
    private static final Map<String, List<String>> TYPE_TO_BUFFS = new HashMap<>();
    static {
        TYPE_TO_BUFFS.put("attack", Arrays.asList(
                "attack", "megaforce", "rob", "displacement", "thunder",
                "ricochet", "annihilation", "corrosion", "combo"
        ));
        TYPE_TO_BUFFS.put("life", Arrays.asList(
                "life", "fasting", "photosynthesis", "vampire", "curse",
                "unyielding", "chaos", "inspiration"
        ));
        TYPE_TO_BUFFS.put("defense", Arrays.asList(
                "thorns", "death_bomb", "spirit_shield"
        ));
        TYPE_TO_BUFFS.put("speed", Arrays.asList(
                "frost", "aura", "hunger", "phantom"
        ));
        TYPE_TO_BUFFS.put("harmony", Collections.singletonList(
                "harmony"
        ));
    }
    public static CompoundNBT generatePillBuffs(String giftType, AlchemyMaterialEffects.MaterialEffect materialEffect) {
        CompoundNBT pillBuffs = new CompoundNBT();
        Random random = new Random();
        Map<String, Integer> adjustedWeights = adjustBuffWeights(materialEffect);
        int buffCount = BUFF_COUNTS[random.nextInt(BUFF_COUNTS.length)];
        GiftConfigReader.GiftConfig giftConfig = GiftConfigReader.readGiftConfig(giftType);
        Set<String> usedBuffs = new HashSet<>();
        String firstBuffType = null;
        for (int i = 0; i < buffCount; i++) {
            String buffType = selectBuffTypeByAdjustedWeight(random, adjustedWeights);
            if (i == 0) {
                firstBuffType = buffType;
            }
            String buffKey = selectRandomBuffFromType(buffType, random, usedBuffs);
            if (buffKey == null) continue;
            int buffLevel = getRandomBuffLevel(giftConfig, buffKey, random);
            pillBuffs.putInt(buffKey, buffLevel);
            usedBuffs.add(buffKey);
        }
        if (firstBuffType != null) {
            PillTextureType textureType = getTextureTypeFromBuffType(firstBuffType);
            pillBuffs.putString("PillTextureType", textureType.name());
        } else {
            pillBuffs.putString("PillTextureType", PillTextureType.DEFAULT.name());
        }
        return pillBuffs;
    }
    private static Map<String, Integer> adjustBuffWeights(AlchemyMaterialEffects.MaterialEffect materialEffect) {
        Map<String, Integer> adjusted = new HashMap<>(BUFF_TYPE_WEIGHTS);
        adjusted.put("attack", Math.max(0, (int)(adjusted.get("attack") * (1 + materialEffect.attackWeight))));
        adjusted.put("life", Math.max(0, (int)(adjusted.get("life") * (1 + materialEffect.lifeWeight))));
        adjusted.put("defense", Math.max(0, (int)(adjusted.get("defense") * (1 + materialEffect.defenseWeight))));
        adjusted.put("speed", Math.max(0, (int)(adjusted.get("speed") * (1 + materialEffect.speedWeight))));
        adjusted.put("harmony", Math.max(0, (int)(adjusted.get("harmony") * (1 + materialEffect.harmonyWeight))));
        return adjusted;
    }
    private static String selectBuffTypeByAdjustedWeight(Random random, Map<String, Integer> adjustedWeights) {
        int totalWeight = adjustedWeights.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : adjustedWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }
        return "attack";
    }
    private static PillTextureType getTextureTypeFromBuffType(String buffType) {
        switch (buffType) {
            case "attack":
                return PillTextureType.ATTACK;
            case "life":
                return PillTextureType.LIFE;
            case "defense":
                return PillTextureType.DEFENSE;
            case "speed":
                return PillTextureType.SPEED;
            case "harmony":
            default:
                return PillTextureType.DEFAULT;
        }
    }
    public static PillTextureType getTextureTypeFromPill(ItemStack pillStack) {
        CompoundNBT nbt = pillStack.getTag();
        if (nbt != null && nbt.contains("EnhancePillBuffs")) {
            CompoundNBT pillBuffs = nbt.getCompound("EnhancePillBuffs");
            if (pillBuffs.contains("PillTextureType")) {
                try {
                    return PillTextureType.valueOf(pillBuffs.getString("PillTextureType"));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return PillTextureType.DEFAULT;
    }
    private static String selectRandomBuffFromType(String buffType, Random random, Set<String> usedBuffs) {
        List<String> availableBuffs = TYPE_TO_BUFFS.get(buffType);
        if (availableBuffs == null || availableBuffs.isEmpty()) {
            return null;
        }
        List<String> unusedBuffs = new ArrayList<>();
        for (String buff : availableBuffs) {
            if (!usedBuffs.contains(buff)) {
                unusedBuffs.add(buff);
            }
        }
        if (unusedBuffs.isEmpty()) {
            return null;
        }
        return unusedBuffs.get(random.nextInt(unusedBuffs.size()));
    }
    private static int getRandomBuffLevel(GiftConfigReader.GiftConfig config, String buffKey, Random random) {
        int[] levelRange = config.buffRanges.getOrDefault(buffKey, new int[]{1, 1});
        int minLevel = levelRange[0];
        int maxLevel = levelRange[1];
        if (minLevel == maxLevel) {
            return minLevel;
        } else {
            return minLevel + random.nextInt(maxLevel - minLevel + 1);
        }
    }
    public static void applyBuffsToPill(ItemStack pillStack, String giftType, AlchemyMaterialEffects.MaterialEffect materialEffect) {
        CompoundNBT pillBuffs = generatePillBuffs(giftType, materialEffect);
        CompoundNBT pillNBT = pillStack.getOrCreateTag();
        pillNBT.put("EnhancePillBuffs", pillBuffs);
    }
    public static String getGiftTypeFromItem(ItemStack giftStack) {
        if (Objects.requireNonNull(giftStack.getItem().getRegistryName()).toString().equals("enhance:green_gift")) {
            return "green_gift";
        } else if (giftStack.getItem().getRegistryName().toString().equals("enhance:blue_gift")) {
            return "blue_gift";
        } else if (giftStack.getItem().getRegistryName().toString().equals("enhance:red_gift")) {
            return "red_gift";
        }
        return "green_gift";
    }
}