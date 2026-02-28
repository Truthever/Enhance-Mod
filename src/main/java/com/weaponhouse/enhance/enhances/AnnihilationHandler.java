package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class AnnihilationHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String ANNIHILATION_TAG = "annihilation";
    private static final String INSPIRATION_MARKER_TAG = "InspirationMarker";
    private static final String ENHANCE_CANDY_TAG = "EnhanceCandy";
    private static final Random RANDOM = new Random();
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getTrueSource() == null || !(event.getSource().getTrueSource() instanceof LivingEntity)) {
            return;
        }
        LivingEntity attacker = (LivingEntity) event.getSource().getTrueSource();
        LivingEntity target = event.getEntityLiving();
        if (!(target instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity targetPlayer = (PlayerEntity) target;
        if (!hasAnnihilationBuff(attacker)) {
            return;
        }
        int annihilationLevel = getAnnihilationLevel(attacker);
        float triggerChance = annihilationLevel * 0.1f;
        if (RANDOM.nextFloat() < triggerChance) {
            removeRandomBuffFromPlayer(targetPlayer, attacker);
        }
    }
    private static boolean hasAnnihilationBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        return entityData.contains(BUFF_TAG) &&
                entityData.getCompound(BUFF_TAG).contains(ANNIHILATION_TAG) &&
                entityData.getCompound(BUFF_TAG).getInt(ANNIHILATION_TAG) > 0;
    }
    private static int getAnnihilationLevel(LivingEntity entity) {
        CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
        return buffs.getInt(ANNIHILATION_TAG);
    }
    private static void removeRandomBuffFromPlayer(PlayerEntity targetPlayer, LivingEntity attacker) {
        CompoundNBT playerData = targetPlayer.getPersistentData();
        if (playerData.getBoolean(ENHANCE_CANDY_TAG)) {
            playerData.remove(ENHANCE_CANDY_TAG);
            targetPlayer.sendMessage(
                    new TranslationTextComponent("message.enhance_candy.annihilation_blocked")
                            .mergeStyle(TextFormatting.GOLD),
                    targetPlayer.getUniqueID()
            );
            if (attacker instanceof PlayerEntity) {
                PlayerEntity attackerPlayer = (PlayerEntity) attacker;
                attackerPlayer.sendMessage(
                        new TranslationTextComponent("message.enhance_candy.annihilation_blocked_attacker")
                                .mergeStyle(TextFormatting.YELLOW),
                        attackerPlayer.getUniqueID()
                );
            }
            return;
        }
        if (!playerData.contains(BUFF_TAG)) {
            return;
        }
        CompoundNBT buffs = playerData.getCompound(BUFF_TAG);
        String activeInspirationBuff = getActiveInspirationBuff(targetPlayer);
        List<String> removableBuffs = getRemovableBuffs(buffs, activeInspirationBuff);
        if (removableBuffs.isEmpty()) {
            return;
        }
        String buffToRemove = removableBuffs.get(RANDOM.nextInt(removableBuffs.size()));
        int removedLevel = buffs.getInt(buffToRemove);
        buffs.remove(buffToRemove);
        playerData.put(BUFF_TAG, buffs);
        handleAttributeRestoration(targetPlayer, buffToRemove);
        sendAnnihilationMessages(targetPlayer, attacker, buffToRemove, removedLevel);
        BossBarHandler.createOrUpdateBossBar(targetPlayer);
    }
    private static String getActiveInspirationBuff(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(INSPIRATION_MARKER_TAG)) {
            CompoundNBT inspiration = playerData.getCompound(INSPIRATION_MARKER_TAG);
            if (inspiration.getBoolean("active")) {
                return inspiration.getString("buffName");
            }
        }
        return null;
    }
    private static List<String> getRemovableBuffs(CompoundNBT buffs, String activeInspirationBuff) {
        List<String> removable = new ArrayList<>();
        for (String buffKey : buffs.keySet()) {
            if (isEnhancementKey(buffKey)) {
                continue;
            }
            if ("enhance_level".equals(buffKey)) {
                continue;
            }
            if (activeInspirationBuff != null && activeInspirationBuff.equals(buffKey)) {
                continue;
            }
            removable.add(buffKey);
        }
        return removable;
    }
    private static boolean isEnhancementKey(String key) {
        return key.equals("one_enhance") || key.equals("two_enhance") || key.equals("three_enhance");
    }
    private static void handleAttributeRestoration(PlayerEntity player, String buffKey) {
        if ("life".equals(buffKey)) {
            LifeHandler.restoreOriginalMaxHealth(player, player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH));
        } else if ("attack".equals(buffKey)) {
            AttackHandler.restoreOriginalAttack(player, player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        }
    }
    private static void sendAnnihilationMessages(PlayerEntity target, LivingEntity attacker, String removedBuff, int removedLevel) {
        String buffDisplayName = new TranslationTextComponent("buff.enhance." + removedBuff).getString();
        target.sendMessage(new TranslationTextComponent(
                "message.enhance_annihilation.buff_removed_target",
                buffDisplayName,
                removedLevel
        ).mergeStyle(TextFormatting.RED), target.getUniqueID());
        if (attacker instanceof PlayerEntity) {
            PlayerEntity attackerPlayer = (PlayerEntity) attacker;
            attackerPlayer.sendMessage(new TranslationTextComponent(
                    "message.enhance_annihilation.buff_removed_attacker",
                    target.getName().getString(),
                    buffDisplayName,
                    removedLevel
            ).mergeStyle(TextFormatting.DARK_PURPLE), attackerPlayer.getUniqueID());
        }
    }
}