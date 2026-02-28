package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class DisplacementHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String DISPLACEMENT_TAG = "displacement";
    private static final int EFFECT_DURATION_SECONDS = 120;
    private static final Random RANDOM = new Random();
    private static final EquipmentSlotType TARGET_SLOT = EquipmentSlotType.CHEST;
    private static final String BINDING_CURSE_ID = "minecraft:binding_curse";
    private static final String VANISHING_CURSE_ID = "minecraft:vanishing_curse";
    private static final String CURSE_EXPIRY_TAG = "DisplacementCurseExpiry";
    private static final String CURSE_UUID_TAG = "DisplacementCurseUUID";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (source instanceof ThornsHandler.ThornsDamageSource) {
            return;
        }
        if (event.getEntity().world.isRemote) {
            return;
        }
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) {
            return;
        }
        if (!(event.getEntityLiving() instanceof PlayerEntity)) {
            return;
        }
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        PlayerEntity target = (PlayerEntity) event.getEntityLiving();
        if (hasDisplacementBuff(attacker)) {
            int displacementLevel = getDisplacementLevel(attacker);
            replaceWithSpecialGoldChestplate(target, displacementLevel);
            removeDisplacementBuff(attacker);
            attacker.sendMessage(
                    new TranslationTextComponent("buff.displacement.trigger.attacker"),
                    Util.DUMMY_UUID
            );
        }
    }
    private static ItemStack createSpecialGoldChestplate(int displacementLevel) {
        ItemStack goldChestplate = new ItemStack(Items.GOLDEN_CHESTPLATE);
        CompoundNBT itemNBT = goldChestplate.getOrCreateTag();
        CompoundNBT displayNBT = new CompoundNBT();
        displayNBT.putString(
                "Name",
                String.format(
                        "{\"text\":\"%s\",\"color\":\"blue\",\"bold\":true}",
                        new TranslationTextComponent("item.displacement.cursed_gold_chestplate.name").getString()
                )
        );
        ListNBT loreNBT = new ListNBT();
        loreNBT.add(StringNBT.valueOf(
                String.format(
                        "{\"text\":\"%s\",\"color\":\"dark_red\"}",
                        new TranslationTextComponent("item.displacement.cursed_gold_chestplate.fixed_curses").getString()
                )
        ));
        loreNBT.add(StringNBT.valueOf(
                String.format(
                        "{\"text\":\"%s\",\"color\":\"gold\"}",
                        new TranslationTextComponent("item.displacement.cursed_gold_chestplate.random_curse_title").getString()
                )
        ));
        int randomEffectType = RANDOM.nextInt(3) + 1;
        String effectTypeName = "";
        switch (randomEffectType) {
            case 1:
                effectTypeName = "减速";
                loreNBT.add(StringNBT.valueOf(
                        String.format(
                                "{\"text\":\"- %s\",\"color\":\"blue\"}",
                                new TranslationTextComponent("item.displacement.cursed_gold_chestplate.curse_slowdown").getString()
                        )
                ));
                break;
            case 2:
                effectTypeName = "护甲降低";
                loreNBT.add(StringNBT.valueOf(
                        String.format(
                                "{\"text\":\"- %s\",\"color\":\"blue\"}",
                                new TranslationTextComponent("item.displacement.cursed_gold_chestplate.curse_armor_down").getString()
                        )
                ));
                break;
            case 3:
                effectTypeName = "生命值降低";
                loreNBT.add(StringNBT.valueOf(
                        String.format(
                                "{\"text\":\"- %s\",\"color\":\"blue\"}",
                                new TranslationTextComponent(
                                        "item.displacement.cursed_gold_chestplate.curse_health_down",
                                        5 * displacementLevel
                                ).getString()
                        )
                ));
                break;
        }
        displayNBT.put("Lore", loreNBT);
        itemNBT.put("display", displayNBT);
        ListNBT enchantmentsNBT = new ListNBT();
        CompoundNBT bindingCurse = new CompoundNBT();
        bindingCurse.putString("id", BINDING_CURSE_ID);
        bindingCurse.putShort("lvl", (short) 1);
        enchantmentsNBT.add(bindingCurse);
        CompoundNBT vanishingCurse = new CompoundNBT();
        vanishingCurse.putString("id", VANISHING_CURSE_ID);
        vanishingCurse.putShort("lvl", (short) 1);
        enchantmentsNBT.add(vanishingCurse);
        itemNBT.put("Enchantments", enchantmentsNBT);
        ListNBT attributeModifiersNBT = new ListNBT();
        UUID attrUUID = UUID.randomUUID();
        switch (randomEffectType) {
            case 1:
                CompoundNBT slowdownAttr = new CompoundNBT();
                slowdownAttr.putString("AttributeName", "minecraft:generic.movement_speed");
                slowdownAttr.putString("Name", "minecraft:generic.movement_speed");
                slowdownAttr.putDouble("Amount", -0.2);
                slowdownAttr.putInt("Operation", 1);
                slowdownAttr.putIntArray("UUID", new int[]{
                        attrUUID.hashCode(),
                        (int) (attrUUID.getMostSignificantBits() >> 32),
                        (int) attrUUID.getMostSignificantBits(),
                        (int) attrUUID.getLeastSignificantBits()
                });
                slowdownAttr.putString("Slot", "chest");
                attributeModifiersNBT.add(slowdownAttr);
                break;
            case 2:
                CompoundNBT armorAttr = new CompoundNBT();
                armorAttr.putString("AttributeName", "minecraft:generic.armor");
                armorAttr.putString("Name", "minecraft:generic.armor");
                armorAttr.putDouble("Amount", -5);
                armorAttr.putInt("Operation", 0);
                armorAttr.putIntArray("UUID", new int[]{
                        attrUUID.hashCode(),
                        (int) (attrUUID.getMostSignificantBits() >> 32),
                        (int) attrUUID.getMostSignificantBits(),
                        (int) attrUUID.getLeastSignificantBits()
                });
                armorAttr.putString("Slot", "chest");
                attributeModifiersNBT.add(armorAttr);
                break;
            case 3:
                CompoundNBT healthAttr = new CompoundNBT();
                healthAttr.putString("AttributeName", "minecraft:generic.max_health");
                healthAttr.putString("Name", "minecraft:generic.max_health");
                healthAttr.putDouble("Amount", -5 * displacementLevel);
                healthAttr.putInt("Operation", 0);
                healthAttr.putIntArray("UUID", new int[]{
                        attrUUID.hashCode(),
                        (int) (attrUUID.getMostSignificantBits() >> 32),
                        (int) attrUUID.getMostSignificantBits(),
                        (int) attrUUID.getLeastSignificantBits()
                });
                healthAttr.putString("Slot", "chest");
                attributeModifiersNBT.add(healthAttr);
                break;
        }
        itemNBT.put("AttributeModifiers", attributeModifiersNBT);
        UUID displacementUUID = UUID.randomUUID();
        int[] displacementUUIDInts = new int[]{
                displacementUUID.hashCode(),
                (int) (displacementUUID.getMostSignificantBits() >> 32),
                (int) displacementUUID.getMostSignificantBits(),
                (int) displacementUUID.getLeastSignificantBits()
        };
        itemNBT.putIntArray("DisplacementUUID", displacementUUIDInts);
        goldChestplate.setTag(itemNBT);
        return goldChestplate;
    }
    private static void replaceWithSpecialGoldChestplate(PlayerEntity player, int displacementLevel) {
        if (!(player.world instanceof ServerWorld)) {
            return;
        }
        ItemStack specialChestplate = createSpecialGoldChestplate(displacementLevel);
        CompoundNBT chestplateNBT = specialChestplate.getOrCreateTag();
        int[] displacementUUIDInts = chestplateNBT.getIntArray("DisplacementUUID");
        ItemStack originalChestplate = player.getItemStackFromSlot(TARGET_SLOT);
        if (!originalChestplate.isEmpty()) {
                player.dropItem(originalChestplate, false);
        }
        player.setItemStackToSlot(TARGET_SLOT, specialChestplate);
        player.sendMessage(
                new TranslationTextComponent("buff.displacement.trigger.target"),
                Util.DUMMY_UUID
        );
        CompoundNBT playerData = player.getPersistentData();
        long expiryTime = System.currentTimeMillis() + (EFFECT_DURATION_SECONDS * 1000);
        playerData.putLong(CURSE_EXPIRY_TAG, expiryTime);
        playerData.putIntArray(CURSE_UUID_TAG, displacementUUIDInts);
        scheduleArmorRemoval(player, displacementUUIDInts, EFFECT_DURATION_SECONDS);
    }
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof PlayerEntity) {
            PlayerEntity player = event.getPlayer();
            CompoundNBT data = player.getPersistentData();
            if (data.contains(CURSE_EXPIRY_TAG) && data.contains(CURSE_UUID_TAG)) {
                long expiryTime = data.getLong(CURSE_EXPIRY_TAG);
                int[] uuidInts = data.getIntArray(CURSE_UUID_TAG);
                if (System.currentTimeMillis() >= expiryTime) {
                    removeCursedArmor(player, uuidInts);
                } else {
                    long remainingSeconds = (expiryTime - System.currentTimeMillis()) / 1000;
                    scheduleArmorRemoval(player, uuidInts, (int) remainingSeconds);
                }
            }
        }
    }
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            CompoundNBT data = player.getPersistentData();
            if (data.contains(CURSE_EXPIRY_TAG) || data.contains(CURSE_UUID_TAG)) {
                data.remove(CURSE_EXPIRY_TAG);
                data.remove(CURSE_UUID_TAG);
            }
        }
    }
    private static void scheduleArmorRemoval(PlayerEntity player, int[] uuidInts, int delaySeconds) {
        UUID playerUUID = player.getUniqueID();
        scheduler.schedule(() -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayerEntity serverPlayer = server.getPlayerList().getPlayerByUUID(playerUUID);
                    if (serverPlayer != null) {
                        removeCursedArmor(serverPlayer, uuidInts);
                    }
                });
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }
    private static void removeCursedArmor(PlayerEntity player, int[] uuidInts) {
        ItemStack chestplate = player.getItemStackFromSlot(TARGET_SLOT);
        if (isCursedArmor(chestplate, uuidInts)) {
            player.setItemStackToSlot(TARGET_SLOT, ItemStack.EMPTY);
            player.sendMessage(
                    new TranslationTextComponent("buff.displacement.curse_armor.removed"),
                    Util.DUMMY_UUID
            );
        }
        CompoundNBT data = player.getPersistentData();
        data.remove(CURSE_EXPIRY_TAG);
        data.remove(CURSE_UUID_TAG);
    }
    private static boolean isCursedArmor(ItemStack stack, int[] uuidInts) {
        if (stack.getItem() != Items.GOLDEN_CHESTPLATE) {
            return false;
        }
        CompoundNBT tag = stack.getTag();
        if (tag == null) {
            return false;
        }
        if (!tag.contains("DisplacementUUID")) {
            return false;
        }
        int[] storedUUID = tag.getIntArray("DisplacementUUID");
        return Arrays.equals(storedUUID, uuidInts);
    }
    private static boolean hasDisplacementBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT buffsNBT = entityData.getCompound(BUFF_TAG);
            return buffsNBT.contains(DISPLACEMENT_TAG) && buffsNBT.getInt(DISPLACEMENT_TAG) > 0;
        }
        return false;
    }
    private static int getDisplacementLevel(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT buffsNBT = entityData.getCompound(BUFF_TAG);
            if (buffsNBT.contains(DISPLACEMENT_TAG)) {
                return buffsNBT.getInt(DISPLACEMENT_TAG);
            }
        }
        return 0;
    }
    private static void removeDisplacementBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT buffsNBT = entityData.getCompound(BUFF_TAG);
            buffsNBT.remove(DISPLACEMENT_TAG);
            entityData.put(BUFF_TAG, buffsNBT);
            BossBarHandler.createOrUpdateBossBar(entity);
        }
    }
}