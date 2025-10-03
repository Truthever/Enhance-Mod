package com.weaponhouse.enhance.enhances;
import com.weaponhouse.enhance.util.ConfigLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import java.util.*;
@Mod.EventBusSubscriber(modid = "enhance")
public class MobEnhancementHandler {
    private static final String GREEN_GIFT_ITEM_ID = "enhance:green_gift";
    private static final String BLUE_GIFT_ITEM_ID = "enhance:blue_gift";
    private static final String RED_GIFT_ITEM_ID = "enhance:red_gift";
    private static final String SUMMONED_MOB_TAG = "summoned_by_buff";
    private static final ResourceLocation IRON_SWORD = new ResourceLocation ("minecraft", "iron_sword");
    private static final ResourceLocation IRON_SHOVEL = new ResourceLocation ("minecraft", "iron_shovel");
    private static final ResourceLocation IRON_PICKAXE = new ResourceLocation ("minecraft", "iron_pickaxe");
    private static final ResourceLocation DIAMOND_SWORD = new ResourceLocation ("minecraft", "diamond_sword");
    private static final ResourceLocation DIAMOND_AXE = new ResourceLocation ("minecraft", "diamond_axe");
    private static final ResourceLocation ENHANCE_SWORD = new ResourceLocation ("enhance", "enhance_sword");
    private static final ResourceLocation ENHANCE_AXE = new ResourceLocation ("enhance", "enhance_axe");
    private static final ResourceLocation BOW = new ResourceLocation("minecraft", "bow");
    private static final ResourceLocation CROSSBOW = new ResourceLocation("minecraft", "crossbow");
    private static boolean isZombie(LivingEntity entity) {
        String entityId = entity.getType().getRegistryName().toString();
        return entityId.startsWith("minecraft:zombie")
                || entityId.equals("minecraft:drowned")
                || entityId.equals("minecraft:husk")
                || entityId.equals("minecraft:zombie_villager");
    }
    private static boolean isPillager(LivingEntity entity) {
        return entity.getType().getRegistryName().toString().equals("minecraft:pillager");
    }
    private static boolean isSkeletonVariant(LivingEntity entity) {
        String entityId = entity.getType().getRegistryName().toString();
        return entityId.equals("minecraft:skeleton") ||
                entityId.equals("minecraft:stray") ||
                entityId.equals("minecraft:wither_skeleton");
    }
    private static boolean isIronGolem(LivingEntity entity) {
        String entityId = entity.getType().getRegistryName().toString();
        return entityId.equals("minecraft:iron_golem")
                || entityId.endsWith(":iron_golem");
    }
    private static final Map<String, BossEnhanceConfig> BOSS_ENHANCE_CONFIGS;
    static {
        BOSS_ENHANCE_CONFIGS = new HashMap<>();
        BOSS_ENHANCE_CONFIGS.put("minecraft:ender_dragon", new BossEnhanceConfig(0.8F, 0.2F));
        BOSS_ENHANCE_CONFIGS.put("minecraft:wither", new BossEnhanceConfig(0.8F, 0.2F));
    }
    private static class BossEnhanceConfig {
        private final float tier2Chance;
        private final float tier3Chance;
        public BossEnhanceConfig(float tier2Chance, float tier3Chance) {
            this.tier2Chance = tier2Chance;
            this.tier3Chance = tier3Chance;
        }
        public float getTier2Chance() { return tier2Chance; }
        public float getTier3Chance() { return tier3Chance; }}
    private static class BuffSelectionResult {
        private final List<String> selectedBuffs;
        private final Map<String, String> forcedBuffLevelRules;
        public BuffSelectionResult(List<String> selectedBuffs, Map<String, String> forcedBuffLevelRules) {
            this.selectedBuffs = selectedBuffs;
            this.forcedBuffLevelRules = forcedBuffLevelRules;
        }
        public List<String> getSelectedBuffs() {
            return selectedBuffs;
        }
        public Map<String, String> getForcedBuffLevelRules() {
            return forcedBuffLevelRules;
        }
    }
    @SubscribeEvent
    public static void onEntitySpawn (EntityJoinWorldEvent event) {
        if (!(event.getWorld () instanceof ServerWorld)) return;
        if (!(event.getEntity () instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        ServerWorld world = (ServerWorld) event.getWorld();
        String entityId = entity.getType().getRegistryName().toString();
        if (BOSS_ENHANCE_CONFIGS.containsKey(entityId)) {
            handleBossEnhancement(entity, world, BOSS_ENHANCE_CONFIGS.get(entityId));
            return;
        }
        if (!isMonster (entity)) return;
        Difficulty difficulty = world.getDifficulty ();
        float enhancerChance = getEnhancerChanceForDifficulty (difficulty);
        Random worldRandom = world.getRandom ();
        float randomValue = worldRandom.nextFloat ();
        if (randomValue>= enhancerChance) return;
        float rand = worldRandom.nextFloat ();
        int tier;
        if (rand < ConfigLoader.LEVEL_ONE_CHANCE) {
            tier = 1;
        } else if (rand < (ConfigLoader.LEVEL_ONE_CHANCE + ConfigLoader.LEVEL_TWO_CHANCE)) {
            tier = 2;
        } else {
            tier = 3;
        }
        applyEnhancement (entity, tier, difficulty);
    }
    private static void handleBossEnhancement(LivingEntity bossEntity, ServerWorld world, BossEnhanceConfig config) {
        if (bossEntity.getPersistentData().getBoolean("WH_Enhanced")) {
            return;
        }
        Random random = world.getRandom();
        int tier = random.nextFloat() < config.getTier2Chance() ? 2 : 3;
        applyEnhancement(bossEntity, tier, world.getDifficulty());
    }
    private static float getEnhancerChanceForDifficulty (Difficulty difficulty) {
        String difficultyKey = difficultyToKey (difficulty);
        return ConfigLoader.ENHANCER_CHANCES.getOrDefault (difficultyKey, 0.3f);
    }
    private static String difficultyToKey (Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
            case PEACEFUL:
                return "easy";
            case NORMAL:
                return "normal";
            case HARD:
                return "hard";
            default:
                return "normal";
        }
    }
    private static boolean isMonster(LivingEntity entity) {
        return entity instanceof MonsterEntity
                || EntityTypeTags.getCollection().getTagByID(new ResourceLocation("monsters")).contains(entity.getType())
                || isIronGolem(entity);
    }
    public static void applyEnhancement (LivingEntity entity, int tier, Difficulty difficulty) {
        if (entity.getPersistentData ().getBoolean ("WH_Enhanced")) {
            return;
        }
        entity.getPersistentData ().putBoolean ("WH_Enhanced", true);
        enhanceBaseAttributes (entity, tier);
        BuffSelectionResult selectionResult = selectBuffs (tier, difficulty);
        List<String> selectedBuffs = selectionResult.getSelectedBuffs();
        Map<String, String> forcedBuffLevelRules = selectionResult.getForcedBuffLevelRules();
        Map<String, Integer> buffLevels = assignBuffLevels (selectedBuffs, tier, difficulty, forcedBuffLevelRules);
        saveBuffsToNBT (entity, buffLevels);
        if (selectedBuffs.contains ("life")) {
            LifeHandler.applyLifeBuff (entity);
            entity.setHealth (entity.getMaxHealth ());
        }
        AttackHandler.applyAttackBuff (entity);
        addDefaultBuff (entity, tier);
        applyFireResistanceByConfig (entity, tier);
        equipEnhancedMob (entity, tier);
        addEnhancementTag (entity, tier);
        BossBarHandler.createBossBar (entity, tier);
    }
    private static void equipEnhancedMob (LivingEntity entity, int tier) {
        Random random = entity.getRNG ();
        switch (tier) {
            case 1:
                equipTierOne (entity, random);
                break;
            case 2:
                equipTierTwo (entity, random);
                break;
            case 3:
                equipTierThree (entity, random);
                break;
            default:
                break;
        }
    }
    private static void equipTierOne (LivingEntity entity, Random random) {
        boolean useLeather = random.nextBoolean ();
        if (random.nextFloat () < 0.3f) {
            ItemStack helmet = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useLeather ? getLeatherArmor ("helmet") : getGoldArmor ("helmet")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (helmet, random, 1);
            }
            entity.setItemStackToSlot (EquipmentSlotType.HEAD, helmet);
        }
        if (random.nextFloat () < 0.3f) {
            ItemStack chestplate = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useLeather ? getLeatherArmor ("chestplate") : getGoldArmor ("chestplate")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (chestplate, random, 1);
            }
            entity.setItemStackToSlot (EquipmentSlotType.CHEST, chestplate);
        }
        if (random.nextFloat () < 0.3f) {
            ItemStack leggings = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useLeather ? getLeatherArmor ("leggings") : getGoldArmor ("leggings")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (leggings, random, 1);
            }
            entity.setItemStackToSlot (EquipmentSlotType.LEGS, leggings);
        }
        if (random.nextFloat () < 0.3f) {
            ItemStack boots = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useLeather ? getLeatherArmor ("boots") : getGoldArmor ("boots")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (boots, random, 1);
            }
            entity.setItemStackToSlot (EquipmentSlotType.FEET, boots);
        }
        if (random.nextFloat() < 0.3f) {
            ItemStack mainHandItem;
            if (isZombie(entity)) {
                float weaponChance = random.nextFloat();
                ResourceLocation mainHand;
                if (weaponChance < 0.4f) {
                    mainHand = IRON_SWORD;
                } else if (weaponChance < 0.7f) {
                    mainHand = IRON_SHOVEL;
                } else {
                    mainHand = IRON_PICKAXE;
                }
                mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
            }
            else if (isPillager(entity)) {
                mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(
                        random.nextFloat() < 0.6f ? CROSSBOW : BOW));
            }
            else if (isSkeletonVariant(entity)) {
                if (random.nextFloat() < 0.7f) {
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(BOW));
                } else {
                    float weaponChance = random.nextFloat();
                    ResourceLocation mainHand;
                    if (weaponChance < 0.4f) {
                        mainHand = IRON_SWORD;
                    } else if (weaponChance < 0.7f) {
                        mainHand = IRON_SHOVEL;
                    } else {
                        mainHand = IRON_PICKAXE;
                    }
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
                }
            }
            else {
                if (random.nextFloat() < 0.15f) {
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(
                            random.nextFloat() < 0.6f ? BOW : CROSSBOW));
                } else {
                    float weaponChance = random.nextFloat();
                    ResourceLocation mainHand;
                    if (weaponChance < 0.4f) {
                        mainHand = IRON_SWORD;
                    } else if (weaponChance < 0.7f) {
                        mainHand = IRON_SHOVEL;
                    } else {
                        mainHand = IRON_PICKAXE;
                    }
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
                }
            }
            if (random.nextFloat() < 0.3f) {
                addRandomEnchantment(mainHandItem, random, 1);
            }
            entity.setItemStackToSlot(EquipmentSlotType.MAINHAND, mainHandItem);
        }
    }
    private static void equipTierTwo (LivingEntity entity, Random random) {
        boolean useIron = random.nextBoolean ();
        if (random.nextFloat () < 0.3f) {
            ItemStack helmet = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useIron ? getIronArmor ("helmet") : getChainArmor ("helmet")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (helmet, random, 2);
            }
            entity.setItemStackToSlot (EquipmentSlotType.HEAD, helmet);
        }
        if (random.nextFloat () < 0.3f) {
            ItemStack chestplate = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useIron ? getIronArmor ("chestplate") : getChainArmor ("chestplate")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (chestplate, random, 2);
            }
            entity.setItemStackToSlot (EquipmentSlotType.CHEST, chestplate);
        }
        if (random.nextFloat () < 0.3f) {
            ItemStack leggings = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useIron ? getIronArmor ("leggings") : getChainArmor ("leggings")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (leggings, random, 2);
            }
            entity.setItemStackToSlot (EquipmentSlotType.LEGS, leggings);
        }
        if (random.nextFloat () < 0.3f) {
            ItemStack boots = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useIron ? getIronArmor ("boots") : getChainArmor ("boots")));
            if (random.nextFloat () < 0.3f) {
                addRandomArmorEnchantment (boots, random, 2);
            }
            entity.setItemStackToSlot (EquipmentSlotType.FEET, boots);
        }
        if (random.nextFloat() < 0.3f) {
            ItemStack mainHandItem;
            if (isZombie(entity)) {
                boolean useSword = random.nextBoolean();
                boolean useDiamond = random.nextFloat() < 0.4f;
                ResourceLocation mainHand;
                if (useSword) {
                    mainHand = useDiamond ? DIAMOND_SWORD : IRON_SWORD;
                } else {
                    mainHand = useDiamond ? DIAMOND_AXE : new ResourceLocation("minecraft", "iron_axe");
                }
                mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
            }
            else if (isPillager(entity)) {
                mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(
                        random.nextFloat() < 0.7f ? CROSSBOW : BOW));
            }
            else if (isSkeletonVariant(entity)) {
                if (random.nextFloat() < 0.8f) {
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(BOW));
                } else {
                    boolean useSword = random.nextBoolean();
                    boolean useDiamond = random.nextFloat() < 0.4f;
                    ResourceLocation mainHand;
                    if (useSword) {
                        mainHand = useDiamond ? DIAMOND_SWORD : IRON_SWORD;
                    } else {
                        mainHand = useDiamond ? DIAMOND_AXE : new ResourceLocation("minecraft", "iron_axe");
                    }
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
                }
            }
            else {
                if (random.nextFloat() < 0.2f) {
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(
                            random.nextFloat() < 0.7f ? BOW : CROSSBOW));
                } else {
                    boolean useSword = random.nextBoolean();
                    boolean useDiamond = random.nextFloat() < 0.4f;
                    ResourceLocation mainHand;
                    if (useSword) {
                        mainHand = useDiamond ? DIAMOND_SWORD : IRON_SWORD;
                    } else {
                        mainHand = useDiamond ? DIAMOND_AXE : new ResourceLocation("minecraft", "iron_axe");
                    }
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
                }
            }
            if (random.nextFloat() < 0.3f) {
                addRandomEnchantment(mainHandItem, random, 2);
            }
            entity.setItemStackToSlot(EquipmentSlotType.MAINHAND, mainHandItem);
        }
    }
    private static void equipTierThree (LivingEntity entity, Random random) {
        boolean useDiamond = random.nextFloat () < 0.6f;
        if (random.nextFloat () < 0.5f) {
            ItemStack helmet = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useDiamond ? getDiamondArmor ("helmet") : getEnhanceArmor ("helmet")));
            if (random.nextFloat () < 0.5f) {
                addRandomArmorEnchantment (helmet, random, 3);
            }
            entity.setItemStackToSlot (EquipmentSlotType.HEAD, helmet);
        }
        if (random.nextFloat () < 0.5f) {
            ItemStack chestplate = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useDiamond ? getDiamondArmor ("chestplate") : getEnhanceArmor ("chestplate")));
            if (random.nextFloat () < 0.5f) {
                addRandomArmorEnchantment (chestplate, random, 3);
            }
            entity.setItemStackToSlot (EquipmentSlotType.CHEST, chestplate);
        }
        if (random.nextFloat () < 0.5f) {
            ItemStack leggings = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useDiamond ? getDiamondArmor ("leggings") : getEnhanceArmor ("leggings")));
            if (random.nextFloat () < 0.5f) {
                addRandomArmorEnchantment (leggings, random, 3);
            }
            entity.setItemStackToSlot (EquipmentSlotType.LEGS, leggings);
        }
        if (random.nextFloat () < 0.5f) {
            ItemStack boots = new ItemStack (ForgeRegistries.ITEMS.getValue (
                    useDiamond ? getDiamondArmor ("boots") : getEnhanceArmor ("boots")));
            if (random.nextFloat () < 0.5f) {
                addRandomArmorEnchantment (boots, random, 3);
            }
            entity.setItemStackToSlot (EquipmentSlotType.FEET, boots);
        }
        if (random.nextFloat() < 0.5f) {
            ItemStack mainHandItem;
            if (isZombie(entity)) {
                boolean useSword = random.nextBoolean();
                ResourceLocation mainHand = useSword ? ENHANCE_SWORD : ENHANCE_AXE;
                mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
            }
            else if (isPillager(entity)) {
                mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(
                        random.nextFloat() < 0.8f ? CROSSBOW : BOW));
            }
            else if (isSkeletonVariant(entity)) {
                if (random.nextFloat() < 0.9f) {
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(BOW));
                } else {
                    boolean useSword = random.nextBoolean();
                    ResourceLocation mainHand = useSword ? ENHANCE_SWORD : ENHANCE_AXE;
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
                }
            }
            else {
                if (random.nextFloat() < 0.25f) {
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(
                            random.nextFloat() < 0.8f ? BOW : CROSSBOW));
                } else {
                    boolean useSword = random.nextBoolean();
                    ResourceLocation mainHand = useSword ? ENHANCE_SWORD : ENHANCE_AXE;
                    mainHandItem = new ItemStack(ForgeRegistries.ITEMS.getValue(mainHand));
                }
            }
            if (random.nextFloat() < 0.5f) {
                addRandomEnchantment(mainHandItem, random, 3);
            }
            entity.setItemStackToSlot(EquipmentSlotType.MAINHAND, mainHandItem);
        }
    }
    private static ResourceLocation getLeatherArmor (String type) {
        return new ResourceLocation ("minecraft", "leather_" + type);
    }
    private static ResourceLocation getGoldArmor(String type) {
        return new ResourceLocation("minecraft", "golden_" + type);
    }
    private static ResourceLocation getIronArmor(String type) {
        return new ResourceLocation("minecraft", "iron_" + type);
    }
    private static ResourceLocation getChainArmor(String type) {
        return new ResourceLocation("minecraft", "chainmail_" + type);
    }
    private static ResourceLocation getDiamondArmor(String type) {
        return new ResourceLocation("minecraft", "diamond_" + type);
    }
    private static ResourceLocation getEnhanceArmor(String type) {
        return new ResourceLocation("enhance", "enhance_" + type);
    }
    private static void addRandomEnchantment(ItemStack stack, Random random, int tier) {
        if (stack.isEmpty()) return;
        List<Enchantment> availableEnchantments = new ArrayList<>();
        if (stack.getItem() instanceof SwordItem) {
            availableEnchantments.addAll(Arrays.asList(
                    Enchantments.SHARPNESS,
                    Enchantments.SMITE,
                    Enchantments.BANE_OF_ARTHROPODS,
                    Enchantments.KNOCKBACK,
                    Enchantments.FIRE_ASPECT,
                    Enchantments.LOOTING,
                    Enchantments.UNBREAKING,
                    Enchantments.MENDING
            ));
        } else if (stack.getItem() instanceof AxeItem) {
            availableEnchantments.addAll(Arrays.asList(
                    Enchantments.SHARPNESS,
                    Enchantments.SMITE,
                    Enchantments.BANE_OF_ARTHROPODS,
                    Enchantments.UNBREAKING,
                    Enchantments.MENDING,
                    Enchantments.EFFICIENCY
            ));
        } else if (stack.getItem() instanceof ToolItem) {
            availableEnchantments.addAll(Arrays.asList(
                    Enchantments.EFFICIENCY,
                    Enchantments.UNBREAKING,
                    Enchantments.FORTUNE,
                    Enchantments.SILK_TOUCH,
                    Enchantments.MENDING
            ));
        } else if (stack.getItem() instanceof BowItem) {
            availableEnchantments.addAll(Arrays.asList(
                    Enchantments.POWER,
                    Enchantments.PUNCH,
                    Enchantments.FLAME,
                    Enchantments.INFINITY,
                    Enchantments.UNBREAKING,
                    Enchantments.MENDING
            ));
        } else if (stack.getItem() instanceof CrossbowItem) {
            availableEnchantments.addAll(Arrays.asList(
                    Enchantments.QUICK_CHARGE,
                    Enchantments.PIERCING,
                    Enchantments.MULTISHOT,
                    Enchantments.UNBREAKING,
                    Enchantments.MENDING
            ));
        }
        int enchantCount = 1 + random.nextInt(2);
        for (int i = 0; i < enchantCount && !availableEnchantments.isEmpty(); i++) {
            int randomIdx = random.nextInt(availableEnchantments.size());
            Enchantment enchantment = availableEnchantments.get(randomIdx);
            if (!enchantment.canApply(stack)) {
                availableEnchantments.remove(randomIdx);
                continue;
            }
            int vanillaMaxLevel = enchantment.getMaxLevel();
            int maxAdd = (tier >= 2) ? 2 : 1;
            int minLevel = vanillaMaxLevel + 1;
            int maxLevel = vanillaMaxLevel + maxAdd;
            int finalLevel = minLevel + random.nextInt(maxLevel - minLevel + 1);
            stack.addEnchantment(enchantment, finalLevel);
            availableEnchantments.remove(randomIdx);
        }
    }
    private static void addRandomArmorEnchantment (ItemStack stack, Random random, int tier) {
        if (stack.isEmpty ()) return;
        List<Enchantment> availableEnchantments = Arrays.asList(
                Enchantments.PROTECTION,
                Enchantments.FIRE_PROTECTION,
                Enchantments.BLAST_PROTECTION,
                Enchantments.PROJECTILE_PROTECTION,
                Enchantments.THORNS,
                Enchantments.UNBREAKING,
                Enchantments.MENDING
        );
        int enchantCount = 1 + random.nextInt (2);
        List<Enchantment> tempEnchants = new ArrayList<>(availableEnchantments);
        for (int i = 0; i < enchantCount && !tempEnchants.isEmpty (); i++) {
            int randomIdx = random.nextInt (tempEnchants.size ());
            Enchantment enchantment = tempEnchants.get (randomIdx);
            if (!enchantment.canApply (stack)) {
                tempEnchants.remove (randomIdx);
                continue;
            }
            int vanillaMaxLevel = enchantment.getMaxLevel ();
            int maxAdd = (tier>= 2) ? 2 : 1;
            int minLevel = vanillaMaxLevel + 1;
            int maxLevel = vanillaMaxLevel + maxAdd;
            int finalLevel = minLevel + random.nextInt (maxLevel - minLevel + 1);
            stack.addEnchantment (enchantment, finalLevel);
            tempEnchants.remove (randomIdx);
        }
    }
    private static void applyFireResistanceByConfig (LivingEntity entity, int tier) {
        String tierKey = "tier" + tier;
        ConfigLoader.FireResistanceSetting setting = ConfigLoader.FIRE_RESISTANCE_SETTINGS.getOrDefault (
                tierKey,
                new ConfigLoader.FireResistanceSetting (false, 300, true)
        );
        if (!setting.isEnabled ()) {
            return;
        }
        EffectInstance fireRes = new EffectInstance (
                Effects.FIRE_RESISTANCE,
                setting.getDurationTicks (),
                0,
                false,
                false
        );
        entity.addPotionEffect (fireRes);
    }
    @SubscribeEvent
    public static void onEntityFall (LivingFallEvent event) {
        LivingEntity entity = event.getEntityLiving ();
        if (entity.world.isRemote) return;
        boolean isTier3Enhancer = entity.getTags ().contains ("three_enhance");
        if (isTier3Enhancer) {
            event.setDamageMultiplier (0.0F);
            event.setCanceled (true);
        }
    }
    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        boolean hasEnhancementTag = entity.getTags().contains("one_enhance") ||
                entity.getTags().contains("two_enhance") ||
                entity.getTags().contains("three_enhance");
        if (hasEnhancementTag && entity.ticksExisted % 5 == 0) {
            BossBarHandler.updateBossBarHealth (entity);
        }
        refreshFireResistanceByConfig (entity);
    }
    private static void refreshFireResistanceByConfig (LivingEntity entity) {
        int tier = getEnhancementTier (entity);
        if (tier == 0) {
            return;
        }
        String tierKey = "tier" + tier;
        ConfigLoader.FireResistanceSetting setting = ConfigLoader.FIRE_RESISTANCE_SETTINGS.getOrDefault (
                tierKey,
                new ConfigLoader.FireResistanceSetting (false, 300, true)
        );
        if (!setting.isEnabled () || !setting.isPersistent ()) {
            return;
        }
        if (entity.world.isRemote) {
            return;
        }
        if (entity.ticksExisted % 20 != 0) {
            return;
        }
        boolean hasValidFireRes = entity.getActivePotionEffects ().stream ()
                .anyMatch (effect -> effect.getPotion () == Effects.FIRE_RESISTANCE
                        && effect.getDuration () > 10 * 20);
        if (!hasValidFireRes) {
            EffectInstance fireRes = new EffectInstance (
                    Effects.FIRE_RESISTANCE,
                    setting.getDurationTicks (),
                    0,
                    false,
                    false
            );
            entity.addPotionEffect (fireRes);
        }
    }
    private static int getEnhancementTier (LivingEntity entity) {
        if (entity.getTags ().contains ("one_enhance")) {
            return 1;
        } else if (entity.getTags ().contains ("two_enhance")) {
            return 2;
        } else if (entity.getTags ().contains ("three_enhance")) {
            return 3;
        } else {
            return 0;
        }
    }
    @SubscribeEvent
    public static void onEntityUnload (EntityLeaveWorldEvent event) {
        if (event.getEntity () instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity ();
            entity.getPersistentData ().remove ("WH_Enhanced");
            BossBarHandler.removeBossBar (entity.getUniqueID ());
        }
    }
    private static void addDefaultBuff (LivingEntity entity, int tier) {
        CompoundNBT entityData = entity.getPersistentData ();
        CompoundNBT buffsTag = entityData.contains ("WeaponHouseBuffs") ?
                entityData.getCompound ("WeaponHouseBuffs") : new CompoundNBT ();
        String buffName = "";
        switch (tier) {
            case 1:
                buffName = "one_enhance";
                break;
            case 2:
                buffName = "two_enhance";
                break;
            case 3:
                buffName = "three_enhance";
                break;
            default:
                buffName = "";
                break;
        }
        if (!buffName.isEmpty()) {
            buffsTag.putInt(buffName, 1);
            entityData.put("WeaponHouseBuffs", buffsTag);
        }
    }
    @SubscribeEvent
    public static void onEntityDeath (LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving ();
        if (entity.world.isRemote) return;
        if (entity.getTags().contains(SUMMONED_MOB_TAG)) {
            return;
        }
        if (entity.getTags ().contains ("one_enhance") && entity.getRNG ().nextFloat () <= ConfigLoader.GREEN_GIFT_DROP_CHANCE) {
            dropGift (entity, GREEN_GIFT_ITEM_ID);
        } else if (entity.getTags ().contains ("two_enhance") && entity.getRNG ().nextFloat () <= ConfigLoader.BLUE_GIFT_DROP_CHANCE) {
            dropGift (entity, BLUE_GIFT_ITEM_ID);
        } else if (entity.getTags ().contains ("three_enhance") && entity.getRNG ().nextFloat () <= ConfigLoader.RED_GIFT_DROP_CHANCE) {
            dropGift (entity, RED_GIFT_ITEM_ID);
        }
        dropEnhanceStoneByTier (entity);
    }
    private static void dropEnhanceStoneByTier (LivingEntity entity) {
        Random rng = entity.getRNG ();
        float dropChance = 0.0F;
        if (entity.getTags ().contains ("one_enhance")) {
            dropChance = ConfigLoader.ENHANCE_STONE_DROP_CHANCE_TIER1;
        } else if (entity.getTags ().contains ("two_enhance")) {
            dropChance = ConfigLoader.ENHANCE_STONE_DROP_CHANCE_TIER2;
        } else if (entity.getTags ().contains ("three_enhance")) {
            dropChance = ConfigLoader.ENHANCE_STONE_DROP_CHANCE_TIER3;
        } else {
            return;
        }
        if (rng.nextFloat () <= dropChance) {
            Item enhanceStone = ForgeRegistries.ITEMS.getValue (new ResourceLocation ("enhance", "enhance_stone"));
            if (enhanceStone != null) {
                ItemStack stoneStack = new ItemStack (enhanceStone, 1);
                entity.entityDropItem (stoneStack, 0.5F);
            }
        }
    }
    private static void dropGift (LivingEntity entity, String giftItemId) {
        Item gift = ForgeRegistries.ITEMS.getValue (new ResourceLocation (giftItemId));
        if (gift == null) {
            return;
        }
        ItemStack giftStack = new ItemStack (gift, 1);
        entity.entityDropItem (giftStack, 0.5f);
    }
    private static void enhanceBaseAttributes (LivingEntity entity, int tier) {
        float healthMultiplier;
        float attackMultiplier;
        switch (tier) {
            case 1:
                healthMultiplier = 2.0f;
                attackMultiplier = 2.0f;
                break;
            case 2:
                healthMultiplier = 5.0f;
                attackMultiplier = 3.0f;
                break;
            case 3:
                healthMultiplier = 12.0f;
                attackMultiplier = 6.0f;
                break;
            default:
                healthMultiplier = 1.0f;
                attackMultiplier = 1.0f;
                break;
        }
        ModifiableAttributeInstance healthAttr = entity.getAttribute (Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double originalHealth = healthAttr.getBaseValue ();
            double newHealth = originalHealth * healthMultiplier;
            healthAttr.setBaseValue (newHealth);
            entity.setHealth ((float) newHealth);
        }
        ModifiableAttributeInstance attackAttr = entity.getAttribute (Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double originalAttack = attackAttr.getBaseValue ();
            double newAttack = originalAttack * attackMultiplier;
            attackAttr.setBaseValue (newAttack);
        }
    }
    private static List<String> getBanListForTierAndDifficulty(int tier, String difficultyKey) {
        switch (tier) {
            case 1:
                return ConfigLoader.TIER_ONE_BAN_BY_DIFFICULTY.getOrDefault(difficultyKey, new ArrayList<>());
            case 2:
                return ConfigLoader.TIER_TWO_BAN_BY_DIFFICULTY.getOrDefault(difficultyKey, new ArrayList<>());
            case 3:
                return ConfigLoader.TIER_THREE_BAN_BY_DIFFICULTY.getOrDefault(difficultyKey, new ArrayList<>());
            default:
                return new ArrayList<>();
        }
    }
    private static BuffSelectionResult selectBuffs (int tier, Difficulty difficulty) {
        Random random = new Random ();
        List<String> availableBuffs = new ArrayList<>();
        String difficultyKey = difficultyToKey(difficulty);
        List<String> banList = getBanListForTierAndDifficulty(tier, difficultyKey);
        switch (tier) {
            case 1:
                if (ConfigLoader.TIER_ONE_BUFFS_BY_DIFFICULTY == null) {
                    ConfigLoader.TIER_ONE_BUFFS_BY_DIFFICULTY = new HashMap<>();
                }
                List<String> tier1Default = ConfigLoader.TIER_ONE_BUFFS_BY_DIFFICULTY.get("normal");
                List<String> tier1Buffs = ConfigLoader.TIER_ONE_BUFFS_BY_DIFFICULTY.getOrDefault (difficultyKey, tier1Default);
                availableBuffs = new ArrayList<>(tier1Buffs != null ? tier1Buffs : new ArrayList<>());
                break;
            case 2:
                if (ConfigLoader.TIER_TWO_BUFFS_BY_DIFFICULTY == null) {
                    ConfigLoader.TIER_TWO_BUFFS_BY_DIFFICULTY = new HashMap<>();
                }
                List<String> tier2Default = ConfigLoader.TIER_TWO_BUFFS_BY_DIFFICULTY.get("normal");
                List<String> tier2Buffs = ConfigLoader.TIER_TWO_BUFFS_BY_DIFFICULTY.getOrDefault(difficultyKey, tier2Default);
                availableBuffs = new ArrayList<>(tier2Buffs != null ? tier2Buffs : new ArrayList<>());
                break;
            case 3:
                if (ConfigLoader.TIER_THREE_BUFFS_BY_DIFFICULTY == null) {
                    ConfigLoader.TIER_THREE_BUFFS_BY_DIFFICULTY = new HashMap<>();
                }
                List<String> tier3Default = ConfigLoader.TIER_THREE_BUFFS_BY_DIFFICULTY.get("normal");
                List<String> tier3Buffs = ConfigLoader.TIER_THREE_BUFFS_BY_DIFFICULTY.getOrDefault(difficultyKey, tier3Default);
                availableBuffs = new ArrayList<>(tier3Buffs != null ? tier3Buffs : new ArrayList<>());
                break;
        }
        availableBuffs.removeAll (banList);
        List<String> originalAvailableBuffs = new ArrayList<>(availableBuffs);
        originalAvailableBuffs.remove ("one_enhance");
        originalAvailableBuffs.remove ("two_enhance");
        originalAvailableBuffs.remove ("three_enhance");
        List<String> selectedBuffs = new ArrayList<>();
        Map<String, String> forcedBuffLevelRules = ConfigLoader.getForcedBuffLevelRules (tier);
        if (forcedBuffLevelRules == null) {
            forcedBuffLevelRules = new HashMap<>();
        }
        for (Map.Entry<String, String> entry : forcedBuffLevelRules.entrySet ()) {
            String forcedBuff = entry.getKey ();
            selectedBuffs.add (forcedBuff);
        }
        Map<String, Object> guaranteeRule = ConfigLoader.getGuaranteeRuleByTier (tier);
        if (guaranteeRule == null) {
            guaranteeRule = new HashMap<>();
            guaranteeRule.put ("guaranteed_min", 1);
            guaranteeRule.put ("chain_init_prob", 0.5f);
            guaranteeRule.put ("chain_next_prob", 0.3f);
        }
        int guaranteedMin = ((Number) guaranteeRule.get ("guaranteed_min")).intValue ();
        float chainInitProb = ((Number) guaranteeRule.get ("chain_init_prob")).floatValue ();
        float chainNextProb = ((Number) guaranteeRule.get ("chain_next_prob")).floatValue ();
        List<String> selectedNonForcedBuffs = new ArrayList<>();
        List<String> nonForcedPool = new ArrayList<>(originalAvailableBuffs);
        while (selectedNonForcedBuffs.size () < guaranteedMin && !nonForcedPool.isEmpty ()) {
            int randomIndex = random.nextInt (nonForcedPool.size ());
            String guaranteedBuff = nonForcedPool.get (randomIndex);
            selectedNonForcedBuffs.add (guaranteedBuff);
            nonForcedPool.remove (randomIndex);
        }
        boolean continueSelecting = true;
        float currentChainProb = chainInitProb;
        while (continueSelecting && !nonForcedPool.isEmpty ()) {
            if (random.nextFloat () < currentChainProb) {
                int randomIndex = random.nextInt (nonForcedPool.size ());
                String extraBuff = nonForcedPool.get (randomIndex);
                selectedNonForcedBuffs.add (extraBuff);
                nonForcedPool.remove (randomIndex);
                currentChainProb = chainNextProb;
            } else {
                continueSelecting = false;
            }
        }
        selectedBuffs.addAll (selectedNonForcedBuffs);
        return new BuffSelectionResult (selectedBuffs, forcedBuffLevelRules);
    }
    private static Map<String, Integer> assignBuffLevels (
            List<String> buffs,
            int tier,
            Difficulty difficulty,
            Map<String, String> forcedBuffLevelRules
    ) {
        Random random = new Random();
        Map<String, Integer> buffLevels = new HashMap<>();
        Map<String, int[]> levelRanges = Collections.emptyMap();
        String difficultyKey = difficultyToKey(difficulty);
        switch (tier) {
            case 1:
                levelRanges = ConfigLoader.TIER_ONE_RANGES_BY_DIFFICULTY
                        .getOrDefault (difficultyKey, ConfigLoader.TIER_ONE_RANGES_BY_DIFFICULTY.get ("normal"));
                break;
            case 2:
                levelRanges = ConfigLoader.TIER_TWO_RANGES_BY_DIFFICULTY
                        .getOrDefault (difficultyKey, ConfigLoader.TIER_TWO_RANGES_BY_DIFFICULTY.get ("normal"));
                break;
            case 3:
                levelRanges = ConfigLoader.TIER_THREE_RANGES_BY_DIFFICULTY
                        .getOrDefault (difficultyKey, ConfigLoader.TIER_THREE_RANGES_BY_DIFFICULTY.get ("normal"));
                break;
        }
        for (String buff : buffs) {
            int finalLevel;
            if (forcedBuffLevelRules.containsKey (buff)) {
                String levelRule = forcedBuffLevelRules.get (buff);
                try {
                    finalLevel = Integer.parseInt (levelRule);
                } catch (NumberFormatException e) {
                    int [] range = levelRanges.getOrDefault (buff, new int []{1, 1});
                    int min = range [0];
                    int max = range [1];
                    finalLevel = min == max ? min : min + random.nextInt (max - min + 1);
                }
            } else {
                int [] range = levelRanges.getOrDefault (buff, new int []{1, 1});
                int min = range [0];
                int max = range [1];
                finalLevel = min == max ? min : min + random.nextInt (max - min + 1);
            }
            buffLevels.put (buff, Math.max (1, finalLevel));
        }
        return buffLevels;
    }
    private static void saveBuffsToNBT (LivingEntity entity, Map<String, Integer> buffLevels) {
        CompoundNBT entityData = entity.getPersistentData ();
        CompoundNBT buffsTag = entityData.contains ("WeaponHouseBuffs") ?
                entityData.getCompound ("WeaponHouseBuffs") : new CompoundNBT ();
        for (Map.Entry<String, Integer> entry : buffLevels.entrySet()) {
            buffsTag.putInt(entry.getKey(), entry.getValue());
        }
        entityData.put("WeaponHouseBuffs", buffsTag);
    }
    private static void addEnhancementTag (LivingEntity entity, int tier) {
        entity.removeTag ("one_enhance");
        entity.removeTag ("two_enhance");
        entity.removeTag ("three_enhance");
        switch (tier) {
            case 1:
                entity.addTag ("one_enhance");
                break;
            case 2:
                entity.addTag ("two_enhance");
                break;
            case 3:
                entity.addTag ("three_enhance");
                break;
        }
    }
}