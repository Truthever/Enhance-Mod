package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.util.ConfigLoader;
import com.weaponhouse.enhance.util.GiftAchievementHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.resources.I18n;
@Mod.EventBusSubscriber(modid = "enhance")
public class GreenGiftHandler {
    private static final String GREEN_GIFT_ID = "enhance:green_gift";
    public static final String KEY_MULTI_BUFF = "message.enhance.green_gift.multi";
    public static final String KEY_LIFE_TRIGGER = "message.enhance.green_gift.life_trigger";
    private static String getLocalizedBuffName(String buffKey) {
        String translationKey = "buff.enhance." + buffKey;
        return I18n.format(translationKey, buffKey);
    }
    private static void sendMultiBuffFeedbackToPlayer(ServerPlayerEntity player, List<String> obtainedBuffs) {
        StringBuilder buffsText = new StringBuilder();
        for (String buffInfo : obtainedBuffs) {
            buffsText.append(buffInfo).append("\n- ");
        }
        if (buffsText.length() > 0) {
            buffsText.delete(buffsText.length() - 3, buffsText.length());
        }
        String message = I18n.format(
                GreenGiftHandler.KEY_MULTI_BUFF,
                obtainedBuffs.size(),
                buffsText.toString()
        );
        player.sendMessage(new StringTextComponent(message), player.getUniqueID());
    }
    @SubscribeEvent
    public static void onGreenGiftRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) {
            return;
        }
        PlayerEntity player = event.getPlayer();
        Hand hand = event.getHand();
        ItemStack heldStack = player.getHeldItem(hand);
        if (!isGreenGiftItem(heldStack)) {
            return;
        }
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }
        executeGreenGiftFunction((ServerPlayerEntity) player, heldStack);
        event.setCancellationResult(ActionResultType.SUCCESS);
        event.setCanceled(true);
    }
    private static boolean isGreenGiftItem(ItemStack stack) {
        if (stack.getItem().getRegistryName() == null) {
            return false;
        }
        return stack.getItem().getRegistryName().toString().equals(GREEN_GIFT_ID);
    }
    private static void executeGreenGiftFunction(ServerPlayerEntity player, ItemStack giftStack) {
        int buffCount = ConfigLoader.GREEN_GIFT_BUFF_COUNT;
        buffCount = Math.max(1, buffCount);
        List<String> obtainedBuffs = new ArrayList<>();
        for (int i = 0; i < buffCount; i++) {
            String randomBuffKey = getRandomBuffKey();
            int randomBuffLevel = getRandomBuffLevel(randomBuffKey);
            while (obtainedBuffs.contains(randomBuffKey)) {
                randomBuffKey = getRandomBuffKey();
            }
            saveBuffToPlayerNBT(player, randomBuffKey, randomBuffLevel);
            if (i == 0 && "life".equals(randomBuffKey)) {
                triggerLifeBuffEffect(player);
            }
            String localizedBuffName = getLocalizedBuffName(randomBuffKey);
            obtainedBuffs.add(String.format("%s Lv.%d", localizedBuffName, randomBuffLevel));
        }
        GiftAchievementHelper.checkAndTrigger(player);
        playUpgradeSound(player);
        sendMultiBuffFeedbackToPlayer(player, obtainedBuffs);
        consumeGreenGiftItem(player, giftStack);
    }
    private static void triggerLifeBuffEffect(ServerPlayerEntity player) {
        LifeHandler.applyLifeBuff(player);
        String message = I18n.format(GreenGiftHandler.KEY_LIFE_TRIGGER);
        player.sendMessage(new StringTextComponent(message), player.getUniqueID());
    }
    private static void playUpgradeSound(ServerPlayerEntity player) {
        World world = player.world;
        world.playSound(
                null,
                player.getPosX(),
                player.getPosY(),
                player.getPosZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS,
                0.5F,
                1.0F
        );
    }
    private static String getRandomBuffKey() {
        if (ConfigLoader.GREEN_GIFT_AVAILABLE_BUFFS.isEmpty()) {
            return "life";
        }
        Random random = new Random();
        int randomIndex = random.nextInt(ConfigLoader.GREEN_GIFT_AVAILABLE_BUFFS.size());
        return ConfigLoader.GREEN_GIFT_AVAILABLE_BUFFS.get(randomIndex);
    }
    private static int getRandomBuffLevel(String buffKey) {
        int[] levelRange = ConfigLoader.GREEN_GIFT_BUFF_RANGES.getOrDefault(buffKey, new int[]{1, 1});
        int minLevel = levelRange[0];
        int maxLevel = levelRange[1];
        if (minLevel == maxLevel) {
            return minLevel;
        } else {
            Random random = new Random();
            return minLevel + random.nextInt(maxLevel - minLevel + 1);
        }
    }
    private static void saveBuffToPlayerNBT(ServerPlayerEntity player, String buffKey, int level) {
        CompoundNBT playerPersistentData = player.getPersistentData();
        CompoundNBT weaponHouseBuffsTag;
        if (playerPersistentData.contains("WeaponHouseBuffs")) {
            weaponHouseBuffsTag = playerPersistentData.getCompound("WeaponHouseBuffs");
        } else {
            weaponHouseBuffsTag = new CompoundNBT();
        }
        weaponHouseBuffsTag.putInt(buffKey, level);
        playerPersistentData.put("WeaponHouseBuffs", weaponHouseBuffsTag);
    }
    private static void consumeGreenGiftItem(ServerPlayerEntity player, ItemStack giftStack) {
        if (player.abilities.isCreativeMode) {
            return;
        }
        giftStack.shrink(1);
    }
}
