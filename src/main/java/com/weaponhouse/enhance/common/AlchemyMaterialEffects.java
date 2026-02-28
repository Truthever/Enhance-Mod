package com.weaponhouse.enhance.common;

import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AlchemyMaterialEffects {
    public static class MaterialEffect {
        public final float attackWeight;
        public final float lifeWeight;
        public final float defenseWeight;
        public final float speedWeight;
        public final float harmonyWeight;
        public final float successChance;
        public final float craftTimeModifier;
        public MaterialEffect(float attack, float life, float defense, float speed,
                              float harmony, float success, float time) {
            this.attackWeight = attack;
            this.lifeWeight = life;
            this.defenseWeight = defense;
            this.speedWeight = speed;
            this.harmonyWeight = harmony;
            this.successChance = success;
            this.craftTimeModifier = time;
        }
    }
    private static final Map<String, MaterialEffect> EFFECT_MAP = new HashMap<>();
    static {
        initializeEffects();
    }
    private static void initializeEffects() {
        addEffect("grass", new MaterialEffect(-0.05f, 0.05f, 0, 0, 0, 0.05f, 0));
        addEffect("fern", new MaterialEffect(-0.05f, 0.10f, -0.05f, 0, 0, 0.05f, 0));
        addEffect("dead_bush", new MaterialEffect(0, -0.10f, 0, 0, 0.10f, -0.10f, 0));
        addEffect("brown_mushroom", new MaterialEffect(-0.10f, 0.10f, 0, 0, 0, 0, 0));
        addEffect("red_mushroom", new MaterialEffect(0.10f, -0.10f, 0, 0, 0, 0, 0));
        addEffect("vine", new MaterialEffect(-0.10f, 0.05f, 0, 0.05f, 0, 0, 0));
        addEffect("lily_pad", new MaterialEffect(-0.10f, 0.05f, 0, 0.05f, 0, 0, 0));
        addEffect("seagrass", new MaterialEffect(-0.10f, 0, 0, 0.10f, 0, 0, 0));
        addEffect("sea_pickle", new MaterialEffect(-0.10f, 0, 0, 0.10f, 0, 0, 0));
        addEffect("kelp", new MaterialEffect(-0.10f, 0, 0, 0.10f, 0, 0, 0));
        addEffect("poppy", new MaterialEffect(0.05f, 0.05f, -0.05f, -0.05f, 0, 0, 0));
        addEffect("red_tulip", new MaterialEffect(0.05f, 0.05f, -0.05f, -0.05f, 0, 0, 0));
        addEffect("rose_bush", new MaterialEffect(0.05f, 0.05f, -0.05f, -0.05f, 0, 0, 0));
        addEffect("white_tulip", new MaterialEffect(0, -0.01f, 0, 0, 0.01f, 0.05f, 0));
        addEffect("oxeye_daisy", new MaterialEffect(0, -0.01f, 0, 0, 0.01f, 0.05f, 0));
        addEffect("blue_orchid", new MaterialEffect(0, -0.01f, 0, 0, 0.01f, 0.05f, 0));
        addEffect("lily_of_the_valley", new MaterialEffect(0, -0.01f, 0, 0, 0.01f, 0.05f, 0));
        addEffect("dandelion", new MaterialEffect(-0.03f, 0, 0.03f, 0, 0, 0, 0));
        addEffect("sunflower", new MaterialEffect(-0.03f, 0, 0.03f, 0, 0, 0, 0));
        addEffect("orchid", new MaterialEffect(0, 0, 0, 0, 0, 0.03f, 0));
        addEffect("allium", new MaterialEffect(-0.05f, 0, 0, 0.05f, 0, 0, 0));
        addEffect("lilac", new MaterialEffect(-0.05f, 0, 0, 0.05f, 0, 0, 0));
        addEffect("pink_tulip", new MaterialEffect(-0.06f, 0.05f, 0, 0, 0.01f, 0, 0));
        addEffect("peony", new MaterialEffect(-0.06f, 0.05f, 0, 0, 0.01f, 0, 0));
        addEffect("orange_tulip", new MaterialEffect(-0.05f, 0, 0.05f, 0, 0, 0, 0));
        addEffect("wither_rose", new MaterialEffect(-0.05f, -0.05f, 0, 0, 0.10f, -0.05f, 0));
        addEffect("wheat_seeds", new MaterialEffect(-0.05f, 0.02f, 0, 0.02f, 0.01f, 0, 0));
        addEffect("pumpkin_seeds", new MaterialEffect(-0.05f, 0.02f, 0, 0.02f, 0.01f, 0, 0));
        addEffect("melon_seeds", new MaterialEffect(-0.05f, 0.02f, 0, 0.02f, 0.01f, 0, 0));
        addEffect("beetroot_seeds", new MaterialEffect(-0.05f, 0.02f, 0, 0.02f, 0.01f, 0, 0));
        addEffect("wheat", new MaterialEffect(-0.07f, 0.10f, -0.04f, 0.01f, 0, 0, 0));
        addEffect("carrot", new MaterialEffect(-0.08f, 0.05f, 0.03f, 0, 0, 0, 0));
        addEffect("potato", new MaterialEffect(-0.08f, 0.05f, 0.03f, 0, 0, 0, 0));
        addEffect("apple", new MaterialEffect(-0.08f, 0.05f, 0.03f, 0, 0, 0, 0));
        addEffect("beetroot", new MaterialEffect(-0.05f, 0.05f, 0, 0, 0, 0.10f, 0));
        addEffect("melon", new MaterialEffect(-0.05f, 0.05f, 0, 0, 0, 0.10f, 0));
        addEffect("poisonous_potato", new MaterialEffect(0.05f, -0.08f, -0.02f, 0, 0.05f, 0, 0));
        addEffect("cocoa_beans", new MaterialEffect(0, 0, 0, 0, 0, 0.10f, 0));
        addEffect("nether_wart", new MaterialEffect(-0.03f, -0.04f, 0.05f, 0, 0.02f, 0, -0.10f));
        addEffect("glistering_melon_slice", new MaterialEffect(-0.05f, 0, 0, 0, 0.05f, -0.05f, 0));
        addEffect("spider_eye", new MaterialEffect(0.10f, -0.10f, 0, 0, 0, -0.05f, 0));
        addEffect("fermented_spider_eye", new MaterialEffect(-0.10f, 0.10f, 0, 0, 0, 0.05f, 0));
        addEffect("sugar", new MaterialEffect(-0.03f, 0, 0.02f, 0, 0.01f, 0, 0));
        addEffect("golden_carrot", new MaterialEffect(-0.10f, 0.10f, 0.05f, -0.05f, 0, 0, 0));
        addEffect("rabbit_foot", new MaterialEffect(0, -0.07f, -0.03f, 0.10f, 0, 0.05f, 0));
        addEffect("pufferfish", new MaterialEffect(0.10f, -0.07f, 0, -0.03f, 0, -0.05f, 0));
        addEffect("magma_cream", new MaterialEffect(0.10f, -0.07f, 0, -0.03f, 0, 0, -0.10f));
        addEffect("blaze_powder", new MaterialEffect(0, 0, 0, 0, 0, 0, -0.20f));
        addEffect("phantom_membrane", new MaterialEffect(0, 0, 0, 0, 0, 0, 0));
        addEffect("ghast_tear", new MaterialEffect(0, 0, 0, 0, 0.10f, -0.05f, 0.20f));
        addEffect("gunpowder", new MaterialEffect(0.15f, -0.10f, -0.02f, -0.03f, 0, -0.10f, -0.25f));
        addEffect("redstone", new MaterialEffect(0, 0, 0, 0, 0, 0.05f, 0));
        addEffect("warped_fungus", new MaterialEffect(-0.10f, 0.10f, 0, 0, 0, 0, -0.10f));
        addEffect("warped_roots", new MaterialEffect(-0.10f, 0.10f, 0, 0, 0, 0, -0.10f));
        addEffect("twisting_vines", new MaterialEffect(-0.10f, 0.10f, 0, 0, 0, 0, -0.10f));
        addEffect("nether_sprouts", new MaterialEffect(-0.10f, 0.10f, 0, 0, 0, 0, -0.10f));
        addEffect("crimson_fungus", new MaterialEffect(0.10f, -0.10f, 0, 0, 0, 0, 0.10f));
        addEffect("crimson_roots", new MaterialEffect(0.10f, -0.10f, 0, 0, 0, 0, 0.10f));
        addEffect("weeping_vines", new MaterialEffect(0.10f, -0.10f, 0, 0, 0, 0, 0.10f));
        addEffect("chorus_fruit", new MaterialEffect(-0.03f, -0.03f, 0, 0, 0.06f, 0.05f, 0));
        addEffect("popped_chorus_fruit", new MaterialEffect(-0.03f, -0.03f, 0, 0, 0.06f, 0.05f, -0.10f));
    }
    private static void addEffect(String itemName, MaterialEffect effect) {
        EFFECT_MAP.put(itemName, effect);
    }
    public static MaterialEffect getEffect(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String itemName = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).getPath();
        return EFFECT_MAP.get(itemName);
    }
    public static boolean hasEffect(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).getPath();
        return EFFECT_MAP.containsKey(itemName);
    }
    public static float getSpecialSuccessBonus(ItemStack stack, String giftType) {
        if (stack.isEmpty()) return 0;
        String itemName = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).getPath();
        if (itemName.equals("cocoa_beans")) {
            switch (giftType) {
                case "green_gift": return 0.10f;
                case "blue_gift": return 0.08f;
                case "red_gift": return 0.05f;
            }
        }
        return 0;
    }


}