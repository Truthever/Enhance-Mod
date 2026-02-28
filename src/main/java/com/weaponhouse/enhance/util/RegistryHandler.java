package com.weaponhouse.enhance.util;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.blocks.*;
import com.weaponhouse.enhance.client.FloatingPillEntity;
import com.weaponhouse.enhance.client.gui.AlchemyFurnaceContainer;
import com.weaponhouse.enhance.client.gui.AlchemyFurnaceTileEntity;
import com.weaponhouse.enhance.enchant.EnhanceEnchantments;
import com.weaponhouse.enhance.entity.ChaoticMerchantEntity;
import com.weaponhouse.enhance.items.*;
import com.weaponhouse.enhance.items.armor.EnhanceBoots;
import com.weaponhouse.enhance.items.armor.EnhanceChestplate;
import com.weaponhouse.enhance.items.armor.EnhanceHelmet;
import com.weaponhouse.enhance.items.armor.EnhanceLeggings;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.Effect;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
public class RegistryHandler {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Enhance.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Enhance.MOD_ID);
    public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, "enhance");
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Enhance.MOD_ID);
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Enhance.MOD_ID);
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, Enhance.MOD_ID);
    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        EFFECTS.register(modEventBus);
        ENTITIES.register(modEventBus);
        CONTAINERS.register(modEventBus);
        TILE_ENTITIES.register(modEventBus);
        EnhanceEnchantments.ENCHANTMENTS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(RegistryHandler.class);
    }
    public static final RegistryObject<EntityType<ThrownDaggerEntity>> THROWN_DAGGER = ENTITIES.register("thrown_dagger",
            () -> EntityType.Builder.<ThrownDaggerEntity>create(ThrownDaggerEntity::new, EntityClassification.MISC)
                    .size(0.25F, 0.25F)
                    .trackingRange(4)
                    .updateInterval(10)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("thrown_dagger")
    );
    private static final int[] ARMOR_DURABILITIES = new int[]{1800, 2500, 3000, 1500};
    private static final int[] ARMOR_DEFENSE_VALUES = new int[]{5, 8, 12, 4};
    private static final int ARMOR_ENCHANTABILITY = 25;
    private static final float ARMOR_TOUGHNESS = 5.0F;
    private static final float ARMOR_KNOCKBACK_RESISTANCE = 0.2F;
    public static final RegistryObject<Block> ENHANCE_STONE_ORE = BLOCKS.register("enhance_stone_ore", EnhanceStoneOre::new);
    public static final RegistryObject<Block> ENHANCE_BLOCK = BLOCKS.register("enhance_block", EnhanceBlock::new);
    public static final RegistryObject<Block> REPLACE_BLOCK = BLOCKS.register("replace_block", ReplaceBlock::new);
    public static final RegistryObject<Block> SACRIFICE_BLOCK = BLOCKS.register("sacrifice_block", SacrificeBlock::new);
    public static final RegistryObject<Block> SPEED_REDUCER = BLOCKS.register("speed_reducer", SpeedReducer::new);
    public static final RegistryObject<Block> ALCHEMY_FURNACE = BLOCKS.register("alchemy_furnace", AlchemyFurnaceBlock::new);
    public static final RegistryObject<Block> ENHANCE_LAND_PORTAL_FRAME = BLOCKS.register("enhance_land_portal_frame", EnhanceLandPortalFrameBlock::new);
    public static final RegistryObject<TileEntityType<AlchemyFurnaceTileEntity>> ALCHEMY_FURNACE_TILE_ENTITY =
            TILE_ENTITIES.register("alchemy_furnace",
                    () -> TileEntityType.Builder.create(AlchemyFurnaceTileEntity::new,
                            RegistryHandler.ALCHEMY_FURNACE.get()).build(null));
    public static final RegistryObject<TileEntityType<EnhanceLandPortalFrameTileEntity>> ENHANCE_LAND_PORTAL_FRAME_TE =
            TILE_ENTITIES.register("enhance_land_portal_frame",
                    () -> TileEntityType.Builder.create(EnhanceLandPortalFrameTileEntity::new,
                            ENHANCE_LAND_PORTAL_FRAME.get()).build(null));
    public static final RegistryObject<Item> ENHANCE_STONE_ORE_ITEM = ITEMS.register("enhance_stone_ore",
            () -> new EnhanceStoneOreItem(ENHANCE_STONE_ORE.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<Item> ENHANCE_BLOCK_ITEM = ITEMS.register("enhance_block",
            () -> new BlockItem(ENHANCE_BLOCK.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<Item> REPLACE_BLOCK_ITEM = ITEMS.register("replace_block",
            () -> new BlockItem(REPLACE_BLOCK.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<Item> SACRIFICE_BLOCK_ITEM = ITEMS.register("sacrifice_block",
            () -> new BlockItem(SACRIFICE_BLOCK.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<Item> SPEED_REDUCER_ITEM = ITEMS.register("speed_reducer",
            () -> new BlockItem(SPEED_REDUCER.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_ITEM = ITEMS.register("alchemy_furnace",
            () -> new BlockItem(ALCHEMY_FURNACE.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<Item> ENHANCE_LAND_PORTAL_FRAME_ITEM = ITEMS.register("enhance_land_portal_frame",
            () -> new BlockItem(ENHANCE_LAND_PORTAL_FRAME.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<ContainerType<AlchemyFurnaceContainer>> ALCHEMY_FURNACE_CONTAINER = CONTAINERS.register(
            "alchemy_furnace",
            () -> IForgeContainerType.create((windowId, inv, data) ->
                    new AlchemyFurnaceContainer(windowId, inv, data))
    );
    public static final RegistryObject<Block> ENHANCE_LAND_PORTAL = BLOCKS.register("enhance_land_portal", EnhanceLandPortalBlock::new);
    public static final RegistryObject<Item> ENHANCE_LAND_PORTAL_ITEM = ITEMS.register("enhance_land_portal",
            () -> new BlockItem(ENHANCE_LAND_PORTAL.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<TileEntityType<EnhanceLandPortalTileEntity>> ENHANCE_LAND_PORTAL_TE =
            TILE_ENTITIES.register("enhance_land_portal",
                    () -> TileEntityType.Builder.create(EnhanceLandPortalTileEntity::new,
                            RegistryHandler.ENHANCE_LAND_PORTAL.get()).build(null));
    public static final RegistryObject<Block> ETERNAL_SACRED_FIRE = BLOCKS.register("eternal_sacred_fire",
            EternalSacredFireBlock::new);
    public static final RegistryObject<Item> ETERNAL_SACRED_FIRE_ITEM = ITEMS.register("eternal_sacred_fire",
            () -> new BlockItem(ETERNAL_SACRED_FIRE.get(),
                    new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<EntityType<ChaoticMerchantEntity>> CHAOTIC_MERCHANT = ENTITIES.register("chaotic_merchant",
            () -> EntityType.Builder.create(ChaoticMerchantEntity::new, EntityClassification.CREATURE)
                    .size(0.8F, 2.2F)
                    .trackingRange(10)
                    .updateInterval(3)
                    .immuneToFire()
                    .build("chaotic_merchant")
    );
    public static final RegistryObject<Item> GREEN_GIFT = ITEMS.register("green_gift", ItemBase::new);
    public static final RegistryObject<Item> BLUE_GIFT = ITEMS.register("blue_gift", ItemBase::new);
    public static final RegistryObject<Item> RED_GIFT = ITEMS.register("red_gift", ItemBase::new);
    public static final RegistryObject<Item> PURPLE_GIFT = ITEMS.register("purple_gift", ItemBase::new);
    public static final RegistryObject<Item> ENHANCE_STONE = ITEMS.register("enhance_stone", ItemBase::new);
    public static final RegistryObject<Item> ATTRIBUTE_VIEWER = ITEMS.register("attribute_viewer", ItemBase::new);
    public static final RegistryObject<Item> PURPLE_GOURD = ITEMS.register("purple_gourd",
            PurpleGourdItem::new);
    public static final RegistryObject<Item> ENHANCE_DUST = ITEMS.register("enhance_dust", ItemBase::new);
    public static final RegistryObject<Item> ENHANCE_PILL = ITEMS.register("enhance_pill",
            () -> new EnhancePillItem(new Item.Properties().group(Enhance.TAB)));
    public static final RegistryObject<Item> DIMENSIONAL_ANCHOR = ITEMS.register("dimensional_anchor",
            () -> new DimensionalAnchorItem(
                    new Item.Properties().group(Enhance.TAB)
            )
    );
    public static final RegistryObject<Item> ENHANCE_CANDY = ITEMS.register("enhance_candy",
            () -> new EnhanceCandyItem(
                    new Item.Properties()
                            .group(Enhance.TAB)
                            .food(new Food.Builder()
                                    .hunger(10)
                                    .saturation(10.0f)
                                    .build())
            )
    );
    public static final RegistryObject<Item> END_RETURN_STAFF = ITEMS.register("end_return_staff",
            () -> new EndReturnStaffItem(
                    new Item.Properties().group(Enhance.TAB)
            )
    );
    public static final RegistryObject<EntityType<FloatingPillEntity>> FLOATING_PILL_ENTITY = ENTITIES.register(
            "floating_pill",
            () -> EntityType.Builder.<FloatingPillEntity>create(FloatingPillEntity::new, EntityClassification.MISC)
                    .size(0.25F, 0.25F) // 改为0.25F，与末影之眼相同大小
                    .trackingRange(10)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("floating_pill")
    );
    public static final IItemTier ENHANCE_TIER = new IItemTier() {
        @Override
        public int getMaxUses() {
            return 1500;
        }
        @Override
        public float getEfficiency() {
            return 10.0F;
        }
        @Override
        public float getAttackDamage() {
            return 9.0F;
        }
        @Override
        public int getHarvestLevel() {
            return 3;
        }
        @Override
        public int getEnchantability() {
            return 15;
        }
        @Override
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(RegistryHandler.ENHANCE_STONE.get());
        }
    };
    public static final RegistryObject<Item> ENHANCE_DAGGER = ITEMS.register("enhance_dagger",
            () -> new EnhanceDaggerItem(new Item.Properties()
                    .group(Enhance.TAB)
                    .maxStackSize(16)
            )
    );
    public static final RegistryObject<Item> ENHANCE_SWORD = ITEMS.register("enhance_sword",
            () -> new EnhanceSwordItem(ENHANCE_TIER, 0, -2.4F,
                    new Item.Properties().group(Enhance.TAB).isImmuneToFire()));

    public static final IItemTier ENHANCE_PICKAXE_TIER = new IItemTier() {
        @Override
        public int getMaxUses() {
            return 1500;
        }
        @Override
        public float getEfficiency() {
            return 10.0F;
        }
        @Override
        public float getAttackDamage() {
            return 3.0F;
        }
        @Override
        public int getHarvestLevel() {
            return 3;
        }
        @Override
        public int getEnchantability() {
            return 10;
        }
        @Override
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(RegistryHandler.ENHANCE_STONE.get());
        }
    };
    public static final RegistryObject<Item> ENHANCE_PICKAXE = ITEMS.register("enhance_pickaxe",
            EnhancePickaxeItem::new);
    public static final IItemTier ENHANCE_AXE_TIER = new IItemTier() {
        @Override
        public int getMaxUses() {
            return 1500;
        }
        @Override
        public float getEfficiency() {
            return 8.0F;
        }
        @Override
        public float getAttackDamage() {
            return 9.0F;
        }
        @Override
        public int getHarvestLevel() {
            return 3;
        }
        @Override
        public int getEnchantability() {
            return 10;
        }
        @Override
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(RegistryHandler.ENHANCE_STONE.get());
        }
    };
    public static final RegistryObject<Item> ENHANCE_AXE = ITEMS.register("enhance_axe",
            () -> new EnhanceAxeItem(
                    ENHANCE_AXE_TIER,
                    2,
                    -3.1F,
                    new Item.Properties().group(Enhance.TAB)
            ));
    public static final IItemTier ENHANCE_SHOVEL_TIER = new IItemTier() {
        @Override
        public int getMaxUses() {
            return 2000;
        }
        @Override
        public float getEfficiency() {
            return 10.0F;
        }
        @Override
        public float getAttackDamage() {
            return 3.0F;
        }
        @Override
        public int getHarvestLevel() {
            return 3;
        }
        @Override
        public int getEnchantability() {
            return 10;
        }
        @Override
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(RegistryHandler.ENHANCE_STONE.get());
        }
    };
    public static final RegistryObject<Item> ENHANCE_SHOVEL = ITEMS.register("enhance_shovel",
            () -> new EnhanceShovelItem(
                    ENHANCE_SHOVEL_TIER,
                    1,
                    -3.0F,
                    new Item.Properties().group(Enhance.TAB)
            ));
    public static final IItemTier ENHANCE_HOE_TIER = new IItemTier() {
        @Override
        public int getMaxUses() {
            return 1500;
        }
        @Override
        public float getEfficiency() {
            return 8.0F;
        }
        @Override
        public float getAttackDamage() {
            return 5.0F;
        }
        @Override
        public int getHarvestLevel() {
            return 3;
        }
        @Override
        public int getEnchantability() {
            return 10;
        }
        @Override
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(RegistryHandler.ENHANCE_STONE.get());
        }
    };
    public static final RegistryObject<Item> ENHANCE_HOE = ITEMS.register("enhance_hoe",
            () -> new EnhanceHoeItem(
                    ENHANCE_HOE_TIER,
                    0,
                    0.0F,
                    new Item.Properties()
                            .group(Enhance.TAB)
                            .maxStackSize(1)
            ));
    public static final IArmorMaterial ENHANCE_ARMOR_MATERIAL = new IArmorMaterial() {
        @Override
        public int getDurability(EquipmentSlotType slot) {
            return ARMOR_DURABILITIES[slot.getIndex()];
        }
        @Override
        public int getDamageReductionAmount(EquipmentSlotType slot) {
            return ARMOR_DEFENSE_VALUES[slot.getIndex()];
        }
        @Override
        public int getEnchantability() {
            return ARMOR_ENCHANTABILITY;
        }
        @Override
        public SoundEvent getSoundEvent() {
            return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE;
        }
        @Override
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(ENHANCE_STONE.get());
        }
        @Override
        public String getName() {
            return "enhance:enhance";
        }
        @Override
        public float getToughness() {
            return ARMOR_TOUGHNESS;
        }
        @Override
        public float getKnockbackResistance() {
            return ARMOR_KNOCKBACK_RESISTANCE;
        }
    };
    public static final RegistryObject<Item> ENHANCE_HELMET = ITEMS.register("enhance_helmet",
            () -> new EnhanceHelmet(
                    ENHANCE_ARMOR_MATERIAL,
                    EquipmentSlotType.HEAD,
                    new Item.Properties().group(Enhance.TAB)
            ));
    public static final RegistryObject<Item> ENHANCE_CHESTPLATE = ITEMS.register("enhance_chestplate",
            () -> new EnhanceChestplate(
                    ENHANCE_ARMOR_MATERIAL,
                    EquipmentSlotType.CHEST,
                    new Item.Properties().group(Enhance.TAB)
            ));
    public static final RegistryObject<Item> ENHANCE_LEGGINGS = ITEMS.register("enhance_leggings",
            () -> new EnhanceLeggings(
                    ENHANCE_ARMOR_MATERIAL,
                    EquipmentSlotType.LEGS,
                    new Item.Properties().group(Enhance.TAB)
            ));
    public static final RegistryObject<Item> ENHANCE_BOOTS = ITEMS.register("enhance_boots",
            () -> new EnhanceBoots(
                    ENHANCE_ARMOR_MATERIAL,
                    EquipmentSlotType.FEET,
                    new Item.Properties().group(Enhance.TAB)
            ));
}