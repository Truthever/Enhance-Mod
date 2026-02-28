package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.common.EnhanceSacrificeRules;
import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import com.weaponhouse.enhance.util.ConfigLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;
public class SacrificeBuffPacket {
    private final String buffId;
    private final int buffLevel;
    private static final String INTERNAL_ORIGINAL_SPEED = "Enhance_Internal_OriginalSpeed";
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String NATURAL_ARMOR = "naturalArmor";
    private static final String DYNAMIC_ARMOR = "dynamicArmor";
    private static final String ORIGINAL_DYNAMIC_ARMOR = "originalDynamicArmor";
    private static final String SACRIFICED_BUFFS = "SacrificedBuffs";
    private static final String BASE_MAX_HEALTH_TAG = "BaseMaxHealth";
    private static final String FIRST_SACRIFICE_MARK = "first_sacrifice_triggered";
    private static final String TOTAL_RED_SACRIFICED_COUNT = "total_red_sacrificed_count";
    private static final String PEAK_ACHIEVEMENT_TRIGGERED = "peak_achievement_triggered";
    private static final int PEAK_REQUIRED_COUNT = 9;
    public SacrificeBuffPacket(String buffId, int buffLevel) {
        this.buffId = buffId;
        this.buffLevel = buffLevel;
    }
    public static void encode(SacrificeBuffPacket msg, PacketBuffer buffer) {
        buffer.writeString(msg.buffId);
        buffer.writeInt(msg.buffLevel);
    }
    public static SacrificeBuffPacket decode(PacketBuffer buffer) {
        String buffId = buffer.readString();
        int buffLevel = buffer.readInt();
        return new SacrificeBuffPacket(buffId, buffLevel);
    }
    public static void handle(SacrificeBuffPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;
            CompoundNBT playerNBT = player.getPersistentData();
            CompoundNBT buffsData = playerNBT.getCompound(EnhanceCommand.BUFF_TAG);
            String buffId = msg.getBuffId();
            int newSacrificeLevel = msg.getBuffLevel();
            int existingSacrificeLevel = getSacrificedLevel(playerNBT, buffId);
            boolean isBuffExist = buffsData.contains(buffId);
            boolean canTriggerSacrifice = isBuffExist && (newSacrificeLevel > existingSacrificeLevel);
            int levelDiff = newSacrificeLevel - existingSacrificeLevel;
            boolean isFirstSacrifice = !playerNBT.getBoolean(FIRST_SACRIFICE_MARK) && canTriggerSacrifice;
            boolean isRedRankBuff = isRedRankBuff(buffId, newSacrificeLevel);
            if (EnhanceSacrificeRules.getSacrificeAttackBonus().containsKey(buffId) && canTriggerSacrifice) {
                ModifiableAttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr == null) return;
                AttackHandler.restoreOriginalAttack(player, attackAttr);
                double beforeBaseAttack = attackAttr.getBaseValue();
                float attackPerLevel = EnhanceSacrificeRules.getSacrificeAttackBonus().get(buffId);
                double computedNewBaseAttack = beforeBaseAttack + (double) (attackPerLevel * levelDiff);
                attackAttr.setBaseValue(computedNewBaseAttack);
                double afterBaseAttack = attackAttr.getBaseValue();
                float actualIncrementalAttack = (float) (afterBaseAttack - beforeBaseAttack);
                playerNBT.putDouble("BaseAttackDamage", afterBaseAttack);
                syncPlayerData(player);
                String buffName = getLocalizedBuffName(buffId);
                if (existingSacrificeLevel == 0) {
                    sendAttackSacrificeSuccessMessage(player, buffName, actualIncrementalAttack, afterBaseAttack, newSacrificeLevel);
                } else {
                    sendAttackOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, actualIncrementalAttack, afterBaseAttack);
                }
                if (isFirstSacrifice) {
                    triggerFirstSacrificeReward(player);
                }
                if (isRedRankBuff) {
                    updateRedSacrificedCount(playerNBT, levelDiff);
                    checkAndTriggerPeakAchievement(player, playerNBT);
                }
            }
            else if (EnhanceSacrificeRules.getSacrificeHealthBonus().containsKey(buffId) && canTriggerSacrifice) {
                ModifiableAttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr == null) return;
                LifeHandler.restoreOriginalMaxHealth(player, healthAttr);
                double beforeBaseHealth = healthAttr.getBaseValue();

                float healthPerLevel = EnhanceSacrificeRules.getSacrificeHealthBonus().get(buffId);
                double computedNewBaseHealth = beforeBaseHealth + (double) (healthPerLevel * levelDiff);
                healthAttr.setBaseValue(computedNewBaseHealth);
                double afterBaseHealth = healthAttr.getBaseValue();
                float actualIncrementalHealth = (float) (afterBaseHealth - beforeBaseHealth);
                player.setHealth((float) afterBaseHealth);
                playerNBT.putDouble(BASE_MAX_HEALTH_TAG, afterBaseHealth);
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                syncPlayerData(player);
                String buffName = getLocalizedBuffName(buffId);
                if (existingSacrificeLevel == 0) {
                    sendSacrificeSuccessMessage(player, buffName, actualIncrementalHealth, (float) afterBaseHealth, newSacrificeLevel);
                } else {
                    sendSacrificeOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, actualIncrementalHealth, (float) afterBaseHealth);
                }
                if (isFirstSacrifice) {
                    triggerFirstSacrificeReward(player);
                }
                if (isRedRankBuff) {
                    updateRedSacrificedCount(playerNBT, levelDiff);
                    checkAndTriggerPeakAchievement(player, playerNBT);
                }
            }
            else if (EnhanceSacrificeRules.getSacrificeDefenseBonus().containsKey(buffId) && canTriggerSacrifice) {
                double beforeNaturalArmor = playerNBT.contains(NATURAL_ARMOR)
                        ? playerNBT.getDouble(NATURAL_ARMOR)
                        : 0.0D;
                float defensePerLevel = EnhanceSacrificeRules.getSacrificeDefenseBonus().get(buffId);
                double computedNewNaturalArmor = beforeNaturalArmor + (double) (defensePerLevel * levelDiff);
                playerNBT.putDouble(NATURAL_ARMOR, computedNewNaturalArmor);
                double otherBonuses = 0.0D;
                if (playerNBT.contains("mountain_defense_adjustment")) otherBonuses += playerNBT.getDouble("mountain_defense_adjustment");
                if (playerNBT.contains("defenseReduction_adjustment")) otherBonuses += playerNBT.getDouble("defenseReduction_adjustment");
                double computedNewDynamicArmor = computedNewNaturalArmor + otherBonuses;
                playerNBT.putDouble(DYNAMIC_ARMOR, computedNewDynamicArmor);
                float actualIncrementalDefense = (float) (computedNewNaturalArmor - beforeNaturalArmor);
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                playerNBT.putDouble("BaseArmor", computedNewNaturalArmor);
                syncPlayerData(player);
                String buffName = getLocalizedBuffName(buffId);
                String totalStr = String.format("%.2f", computedNewNaturalArmor);
                if (existingSacrificeLevel == 0) {
                    sendDefenseSacrificeSuccessMessage(player, buffName, actualIncrementalDefense, totalStr, newSacrificeLevel);
                } else {
                    sendDefenseOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, actualIncrementalDefense, levelDiff, totalStr);
                }
                if (isFirstSacrifice) triggerFirstSacrificeReward(player);
                if (isRedRankBuff) {
                    updateRedSacrificedCount(playerNBT, levelDiff);
                    checkAndTriggerPeakAchievement(player, playerNBT);
                }
            }
            else if (EnhanceSacrificeRules.getSacrificeSpeedBonus().containsKey(buffId) && canTriggerSacrifice) {
                ModifiableAttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr == null) return;
                CompoundNBT entityData = player.getPersistentData();
                if (!entityData.contains(INTERNAL_ORIGINAL_SPEED)) {
                    entityData.putDouble(INTERNAL_ORIGINAL_SPEED, speedAttr.getBaseValue());
                }
                double beforeBaseSpeed = speedAttr.getBaseValue();
                float speedPerLevel = EnhanceSacrificeRules.getSacrificeSpeedBonus().get(buffId);
                float incrementalSpeed = speedPerLevel * levelDiff;
                double computedNewBaseSpeed = beforeBaseSpeed + (double) incrementalSpeed;
                speedAttr.setBaseValue(computedNewBaseSpeed);
                AttributeModifier existingMod = speedAttr.getModifier(SPEED_MODIFIER_UUID);
                if (existingMod != null) {
                    speedAttr.removeModifier(existingMod);
                }
                double afterBaseSpeed = speedAttr.getBaseValue();
                float actualIncrementalSpeed = (float) (afterBaseSpeed - beforeBaseSpeed);
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                entityData.putDouble("BaseMovementSpeed", afterBaseSpeed);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                syncPlayerData(player);
                String buffName = getLocalizedBuffName(buffId);
                if (existingSacrificeLevel == 0) {
                    sendSpeedSacrificeSuccessMessage(player, buffName, actualIncrementalSpeed, afterBaseSpeed, newSacrificeLevel);
                } else {
                    sendSpeedOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, actualIncrementalSpeed, afterBaseSpeed);
                }
                if (isFirstSacrifice) {
                    triggerFirstSacrificeReward(player);
                }
                if (isRedRankBuff) {
                    updateRedSacrificedCount(playerNBT, levelDiff);
                    checkAndTriggerPeakAchievement(player, playerNBT);
                }
            }
            else if (EnhanceSacrificeRules.getSacrificeHarmonyBonus().containsKey(buffId) && canTriggerSacrifice) {
                float[] bonuses = EnhanceSacrificeRules.getSacrificeHarmonyBonus().get(buffId);
                float healthPerLevel = bonuses[0];
                float attackPerLevel = bonuses[1];
                float defensePerLevel = bonuses[2];
                float incrementalHealth = healthPerLevel * levelDiff;
                float incrementalAttack = attackPerLevel * levelDiff;
                float incrementalDefense = defensePerLevel * levelDiff;
                ModifiableAttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null) {
                    LifeHandler.restoreOriginalMaxHealth(player, healthAttr);
                    double currentHealth = healthAttr.getBaseValue();
                    double newHealth = currentHealth + incrementalHealth;
                    healthAttr.setBaseValue(newHealth);
                    playerNBT.putDouble(BASE_MAX_HEALTH_TAG, newHealth);
                    if (player.getHealth() > newHealth) {
                        player.setHealth((float) newHealth);
                    }
                }
                ModifiableAttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                double harmonyFinalBaseAttack;
                if (attackAttr != null) {
                    AttackHandler.restoreOriginalAttack(player, attackAttr);
                    double currentAttack = attackAttr.getBaseValue();
                    harmonyFinalBaseAttack = currentAttack + incrementalAttack;
                    attackAttr.setBaseValue(harmonyFinalBaseAttack);
                    playerNBT.putDouble("BaseAttackDamage", harmonyFinalBaseAttack);
                }
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                syncPlayerData(player);
                double beforeNaturalArmor = playerNBT.contains(NATURAL_ARMOR) ? playerNBT.getDouble(NATURAL_ARMOR) : 0.0D;
                double newNaturalArmor = beforeNaturalArmor + (double) incrementalDefense;
                playerNBT.putDouble(NATURAL_ARMOR, newNaturalArmor);
                double otherBonuses = 0.0D;
                if (playerNBT.contains("mountain_defense_adjustment")) otherBonuses += playerNBT.getDouble("mountain_defense_adjustment");
                if (playerNBT.contains("defenseReduction_adjustment")) otherBonuses += playerNBT.getDouble("defenseReduction_adjustment");
                playerNBT.putDouble(DYNAMIC_ARMOR, newNaturalArmor + otherBonuses);
                playerNBT.putDouble("BaseArmor", newNaturalArmor);
                double finalBaseHealth = healthAttr != null ? healthAttr.getBaseValue() : 0;
                double finalBaseAttack = attackAttr != null ? attackAttr.getBaseValue() : 0;
                String buffName = getLocalizedBuffName(buffId);
                if (existingSacrificeLevel == 0) {
                    sendHarmonySacrificeSuccessMessage(
                            player, buffName,
                            incrementalHealth, incrementalAttack, incrementalDefense,
                            newSacrificeLevel,
                            finalBaseHealth, finalBaseAttack, newNaturalArmor
                    );
                } else {
                    sendHarmonyOverwriteMessage(
                            player,
                            buffName,
                            existingSacrificeLevel,
                            newSacrificeLevel,
                            incrementalHealth,
                            incrementalAttack,
                            incrementalDefense,
                            levelDiff,
                            finalBaseHealth,
                            finalBaseAttack,
                            newNaturalArmor
                    );
                }
                if (isFirstSacrifice) {
                    triggerFirstSacrificeReward(player);
                    if (isRedRankBuff) {
                        updateRedSacrificedCount(playerNBT, levelDiff);
                        checkAndTriggerPeakAchievement(player, playerNBT);
                    }
                }}
            else {
                if (isBuffExist) {
                    if (EnhanceSacrificeRules.getSacrificeHealthBonus().containsKey(buffId)) {
                        LifeHandler.restoreOriginalMaxHealth(player, player.getAttribute(Attributes.MAX_HEALTH));
                    }
                    if (EnhanceSacrificeRules.getSacrificeAttackBonus().containsKey(buffId)) {
                        AttackHandler.restoreOriginalAttack(player, player.getAttribute(Attributes.ATTACK_DAMAGE));
                    }
                    if (EnhanceSacrificeRules.getSacrificeDefenseBonus().containsKey(buffId)) {
                        restoreOriginalDefense(player);
                    }
                    if (EnhanceSacrificeRules.getSacrificeSpeedBonus().containsKey(buffId)) {
                        restoreOriginalSpeed(player);
                    }
                    if (EnhanceSacrificeRules.getSacrificeHarmonyBonus().containsKey(buffId)) {
                        LifeHandler.restoreOriginalMaxHealth(player, player.getAttribute(Attributes.MAX_HEALTH));
                        AttackHandler.restoreOriginalAttack(player, player.getAttribute(Attributes.ATTACK_DAMAGE));
                        restoreOriginalDefense(player);
                    }
                    buffsData.remove(buffId);
                    playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                    syncPlayerData(player);
                    String buffName = getLocalizedBuffName(buffId);
                    if (existingSacrificeLevel > 0 && newSacrificeLevel <= existingSacrificeLevel) {
                        sendLowLevelRemoveMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel);
                    } else {
                        sendRemoveMessage(player, buffName);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    private static String getLocalizedBuffName(String buffId) {
        TranslationTextComponent translation = new TranslationTextComponent("buff.enhance." + buffId);
        return translation.getString();
    }
    private static void updateRedSacrificedCount(CompoundNBT playerNBT, int levelDiff) {
        int currentCount = playerNBT.getInt(TOTAL_RED_SACRIFICED_COUNT);
        int newCount = currentCount + 1;
        playerNBT.putInt(TOTAL_RED_SACRIFICED_COUNT, newCount);
    }
    private static void checkAndTriggerPeakAchievement(ServerPlayerEntity player, CompoundNBT playerNBT) {
        if (playerNBT.getBoolean(PEAK_ACHIEVEMENT_TRIGGERED)) {
            return;
        }
        int totalCount = playerNBT.getInt(TOTAL_RED_SACRIFICED_COUNT);
        if (totalCount >= PEAK_REQUIRED_COUNT) {
            try {
                if (Enhance.PEAK_ACHIEVEMENT_TRIGGER != null) {
                    Enhance.PEAK_ACHIEVEMENT_TRIGGER.trigger(player);
                    CompoundNBT buffsData = playerNBT.getCompound(EnhanceCommand.BUFF_TAG);
                    int currentLevel = buffsData.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
                    int newLevel = Math.min(currentLevel + 1, EnhanceCommand.MAX_ENHANCE_LEVEL);
                    newLevel = Math.max(newLevel, 1);
                    buffsData.putInt(EnhanceCommand.ENHANCE_LEVEL_TAG, newLevel);
                    playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                    TranslationTextComponent achievementMsg = new TranslationTextComponent("message.achievement.peak_unlocked");
                    TranslationTextComponent levelUpMsg = new TranslationTextComponent("command.enhance.level_up", newLevel);
                    player.sendMessage(
                            new StringTextComponent(
                                    TextFormatting.GOLD + achievementMsg.getString() + "\n" +
                                            TextFormatting.GREEN + levelUpMsg.getString()
                            ),
                            player.getUniqueID()
                    );
                    playerNBT.putBoolean(PEAK_ACHIEVEMENT_TRIGGERED, true);
                    syncPlayerData(player);
                }
            } catch (Exception ignored) {
            }
        }
    }
    private static boolean isRedRankBuff(String buffId, int sacrificeLevel) {
        if (!ConfigLoader.RED_GIFT_AVAILABLE_BUFFS.contains(buffId)) {
            return false;
        }
        int[] normalDifficultyRange = ConfigLoader.RED_GIFT_BUFF_RANGES.getOrDefault(buffId, new int[]{1, 1});
        int minimumRequiredLevel = normalDifficultyRange[0];
        return sacrificeLevel >= minimumRequiredLevel;
    }
    private static void triggerFirstSacrificeReward(ServerPlayerEntity player) {
        try {
            CompoundNBT playerNBT = player.getPersistentData();
            CompoundNBT buffsData = playerNBT.getCompound(EnhanceCommand.BUFF_TAG);
            CompoundNBT permanentData = getPermanentData(playerNBT);
            String sacrificeTriggeredKey = "sacrifice_triggered";
            String sacrificeLevelIncreasedKey = "sacrifice_level_increased";
            if (!permanentData.getBoolean(sacrificeTriggeredKey)) {
                if (Enhance.SACRIFICE_TRIGGER != null) {
                    Enhance.SACRIFICE_TRIGGER.trigger(player);
                }
                if (!permanentData.getBoolean(sacrificeLevelIncreasedKey)) {
                    int currentLevel = buffsData.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
                    int newLevel = Math.min(currentLevel + 1, EnhanceCommand.MAX_ENHANCE_LEVEL);
                    newLevel = Math.max(newLevel, 1);
                    buffsData.putInt(EnhanceCommand.ENHANCE_LEVEL_TAG, newLevel);
                    playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                    TranslationTextComponent levelUpMsg = new TranslationTextComponent("command.enhance.level_up", newLevel);
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.GREEN + levelUpMsg.getString()),
                            player.getUniqueID()
                    );
                    permanentData.putBoolean(sacrificeLevelIncreasedKey, true);
                    savePermanentData(playerNBT, permanentData);
                }
                permanentData.putBoolean(sacrificeTriggeredKey, true);
                savePermanentData(playerNBT, permanentData);
                syncPlayerData(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static CompoundNBT getPermanentData(CompoundNBT playerNBT) {
        return playerNBT.contains("EnhancePermanentData")
                ? playerNBT.getCompound("EnhancePermanentData")
                : new CompoundNBT();
    }
    private static void savePermanentData(CompoundNBT playerNBT, CompoundNBT permanentData) {
        playerNBT.put("EnhancePermanentData", permanentData);
    }
    private static int getSacrificedLevel(CompoundNBT playerNBT, String buffId) {
        if (!playerNBT.contains(SACRIFICED_BUFFS, 10)) {
            playerNBT.put(SACRIFICED_BUFFS, new CompoundNBT());
        }
        CompoundNBT sacrificedData = playerNBT.getCompound(SACRIFICED_BUFFS);
        return sacrificedData.getInt(buffId);
    }
    private static void updateSacrificedLevel(CompoundNBT playerNBT, String buffId, int newLevel) {
        CompoundNBT sacrificedData = playerNBT.getCompound(SACRIFICED_BUFFS);
        sacrificedData.put(buffId, IntNBT.valueOf(newLevel));
        playerNBT.put(SACRIFICED_BUFFS, sacrificedData);
    }
    private static void syncPlayerData(ServerPlayerEntity player) {
        player.refreshDisplayName();
        player.setHealth(player.getHealth());
        SendBuffPacket.sendBuffData(player);
    }
    public static void restoreOriginalSpeed(LivingEntity entity) {
        ModifiableAttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null || entity.world.isRemote) return;
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(INTERNAL_ORIGINAL_SPEED)) {
            double originalBaseSpeed = entityData.getDouble(INTERNAL_ORIGINAL_SPEED);
            speedAttr.setBaseValue(originalBaseSpeed);

            AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_UUID);
            if (existing != null) {
                speedAttr.removeModifier(existing);
            }
            entityData.remove(INTERNAL_ORIGINAL_SPEED);
        }
    }
    public static void restoreOriginalDefense(LivingEntity entity) {
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains(ORIGINAL_DYNAMIC_ARMOR)) {
            nbt.putDouble(DYNAMIC_ARMOR, nbt.getDouble(ORIGINAL_DYNAMIC_ARMOR));
            nbt.remove(ORIGINAL_DYNAMIC_ARMOR);
        } else if (nbt.contains(DYNAMIC_ARMOR)) {
            nbt.remove(DYNAMIC_ARMOR);
        }
    }
    private static void sendAttackSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, double newBase, int newLevel) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.attack.success",
                buffName, newLevel, incStr, newBase
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message.getString()), player.getUniqueID());
    }
    private static void sendAttackOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, double newBase) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        int levelDiff = newLv - oldLv;
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.attack.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, newBase
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message.getString()), player.getUniqueID());
    }
    private static void sendSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, float newMax, int newLevel) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.health.success",
                buffName, newLevel, incStr, newMax
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message.getString()), player.getUniqueID());
    }
    private static void sendSacrificeOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, float newMax) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        int levelDiff = newLv - oldLv;
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.health.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, newMax
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message.getString()), player.getUniqueID());
    }
    private static void sendDefenseSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, String totalStr, int newLevel) {
        String incStr = formatNumber(incremental);
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.defense.success",
                buffName, newLevel, incStr, totalStr
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message.getString()), player.getUniqueID());
    }
    private static void sendDefenseOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, int levelDiff, String totalStr) {
        String incStr = formatNumber(incremental);
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.defense.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, totalStr
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message.getString()), player.getUniqueID());
    }
    private static void sendSpeedSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, double newSpeed, int newLevel) {
        String incStr = String.format("%.2f", incremental);
        String speedStr = String.format("%.4f", newSpeed);
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.speed.success",
                buffName, newLevel, incStr, speedStr
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message.getString()), player.getUniqueID());
    }
    private static void sendSpeedOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, double newSpeed) {
        String incStr = String.format("%.2f", incremental);
        String speedStr = String.format("%.4f", newSpeed);
        int levelDiff = newLv - oldLv;
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.speed.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, speedStr
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message.getString()), player.getUniqueID());
    }
    private static void sendHarmonySacrificeSuccessMessage(
            ServerPlayerEntity player, String buffName,
            float incHealth, float incAttack, float incDefense,
            int newLevel,
            double totalHealth, double totalAttack, double totalDefense
    ) {
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.harmony.success",
                buffName, newLevel,
                formatNumber(incHealth),
                formatNumber(incAttack),
                formatNumber(incDefense),
                totalHealth, totalAttack, totalDefense
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message.getString()), player.getUniqueID());
    }
    private static String formatNumber(float v) {
        return (v % 1 == 0) ? String.valueOf((int) v) : String.format("%.2f", v);
    }
    private static void sendHarmonyOverwriteMessage(
            ServerPlayerEntity player, String buffName, int oldLv, int newLv,
            float incHealth, float incAttack, float incDefense, int levelDiff,
            double newHealth, double newAttack, double newDefense) {
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.harmony.overwrite",
                buffName, oldLv, newLv, incHealth, levelDiff,
                incAttack, levelDiff, incDefense, levelDiff,
                newHealth, newAttack, newDefense
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message.getString()), player.getUniqueID());
    }
    private static void sendLowLevelRemoveMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv) {
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.low_level",
                buffName, oldLv, newLv
        );
        player.sendMessage(new StringTextComponent(TextFormatting.YELLOW + message.getString()), player.getUniqueID());
    }
    private static void sendRemoveMessage(ServerPlayerEntity player, String buffName) {
        TranslationTextComponent message = new TranslationTextComponent(
                "message.sacrifice.remove.normal",
                buffName
        );
        player.sendMessage(new StringTextComponent(TextFormatting.YELLOW + message.getString()), player.getUniqueID());
    }
    public String getBuffId() {
        return buffId;
    }
    public int getBuffLevel() {
        return buffLevel;
    }
}
