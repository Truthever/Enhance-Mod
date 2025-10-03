package com.weaponhouse.enhance.network;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.common.EnhanceSacrificeRules;
import com.weaponhouse.enhance.util.ConfigLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
public class SacrificeBuffPacket {
    private final String buffId;
    private final int buffLevel;
    private static final String INTERNAL_ORIGINAL_SPEED = "Enhance_Internal_OriginalSpeed";
    private static final UUID DEFENSE_MODIFIER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
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
                float attackPerLevel = EnhanceSacrificeRules.getSacrificeAttackBonus().get(buffId);
                ModifiableAttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr == null) return;
                AttackHandler.restoreOriginalAttack(player, attackAttr);
                double currentBaseAttack = attackAttr.getBaseValue();
                float incrementalAttack = attackPerLevel * levelDiff;
                double newBaseAttack = currentBaseAttack + incrementalAttack;
                attackAttr.setBaseValue(newBaseAttack);
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                playerNBT.putDouble("BaseAttackDamage", newBaseAttack);
                syncPlayerData(player);
                String buffName = I18n.format("buff.enhance." + buffId);
                if (existingSacrificeLevel == 0) {
                    sendAttackSacrificeSuccessMessage(player, buffName, incrementalAttack, newBaseAttack, newSacrificeLevel);
                } else {
                    sendAttackOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, incrementalAttack, newBaseAttack);
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
                double currentBaseHealth = healthAttr.getBaseValue();
                if (!playerNBT.contains(BASE_MAX_HEALTH_TAG)) {
                    playerNBT.putDouble(BASE_MAX_HEALTH_TAG, currentBaseHealth);
                }
                float healthPerLevel = EnhanceSacrificeRules.getSacrificeHealthBonus().get(buffId);
                float incrementalHealth = healthPerLevel * levelDiff;
                double newBaseHealth = currentBaseHealth + incrementalHealth;
                healthAttr.setBaseValue(newBaseHealth);
                player.setHealth((float) newBaseHealth);
                playerNBT.putDouble(BASE_MAX_HEALTH_TAG, newBaseHealth);
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                syncPlayerData(player);
                String buffName = I18n.format("buff.enhance." + buffId);
                if (existingSacrificeLevel == 0) {
                    sendSacrificeSuccessMessage(player, buffName, incrementalHealth, (float) newBaseHealth, newSacrificeLevel);
                } else {
                    sendSacrificeOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, incrementalHealth, (float) newBaseHealth);
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
                ModifiableAttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
                if (armorAttr == null) return;
                if (!playerNBT.contains(NATURAL_ARMOR)) {
                    playerNBT.putDouble(NATURAL_ARMOR, armorAttr.getBaseValue());
                }
                double currentNaturalArmor = playerNBT.getDouble(NATURAL_ARMOR);
                float defensePerLevel = EnhanceSacrificeRules.getSacrificeDefenseBonus().get(buffId);
                float incrementalDefense = defensePerLevel * levelDiff;
                double newNaturalArmor = currentNaturalArmor + incrementalDefense;
                playerNBT.putDouble(NATURAL_ARMOR, newNaturalArmor);
                double otherBonuses = 0.0;
                if (playerNBT.contains("mountain_defense_adjustment")) otherBonuses += playerNBT.getDouble("mountain_defense_adjustment");
                if (playerNBT.contains("defenseReduction_adjustment")) otherBonuses += playerNBT.getDouble("defenseReduction_adjustment");
                double newDynamicArmor = newNaturalArmor + otherBonuses;
                playerNBT.putDouble(DYNAMIC_ARMOR, newDynamicArmor);
                armorAttr.setBaseValue(newNaturalArmor);
                AttributeModifier existingMod = armorAttr.getModifier(DEFENSE_MODIFIER_UUID);
                if (existingMod != null) {
                    armorAttr.removeModifier(existingMod);
                }
                if (otherBonuses != 0) {
                    AttributeModifier dynamicModifier = new AttributeModifier(
                            DEFENSE_MODIFIER_UUID,
                            "DynamicDefenseBoost",
                            otherBonuses,
                            AttributeModifier.Operation.ADDITION
                    );
                    armorAttr.applyPersistentModifier(dynamicModifier);
                }
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                playerNBT.putDouble("BaseArmor", newNaturalArmor);
                syncPlayerData(player);
                String buffName = I18n.format("buff.enhance." + buffId);
                if (existingSacrificeLevel == 0) {
                    sendDefenseSacrificeSuccessMessage(player, buffName, incrementalDefense, newNaturalArmor, newSacrificeLevel);
                } else {
                    sendDefenseOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, incrementalDefense, newNaturalArmor);
                }
                if (isFirstSacrifice) {
                    triggerFirstSacrificeReward(player);
                }
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
                double currentBaseSpeed = speedAttr.getBaseValue();
                float speedPerLevel = EnhanceSacrificeRules.getSacrificeSpeedBonus().get(buffId);
                float incrementalSpeed = speedPerLevel * levelDiff;
                double newBaseSpeed = currentBaseSpeed + incrementalSpeed;
                speedAttr.setBaseValue(newBaseSpeed);
                AttributeModifier existingMod = speedAttr.getModifier(DEFENSE_MODIFIER_UUID);
                if (existingMod != null) {
                    speedAttr.removeModifier(existingMod);
                }
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                entityData.putDouble("BaseMovementSpeed", newBaseSpeed);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                syncPlayerData(player);
                String buffName = I18n.format("buff.enhance." + buffId);
                if (existingSacrificeLevel == 0) {
                    sendSpeedSacrificeSuccessMessage(player, buffName, incrementalSpeed, newBaseSpeed, newSacrificeLevel);
                } else {
                    sendSpeedOverwriteMessage(player, buffName, existingSacrificeLevel, newSacrificeLevel, incrementalSpeed, newBaseSpeed);
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
                double harmonyFinalBaseAttack = 0;
                if (attackAttr != null) {
                    AttackHandler.restoreOriginalAttack(player, attackAttr);
                    double currentAttack = attackAttr.getBaseValue();
                    harmonyFinalBaseAttack = currentAttack + incrementalAttack;
                    attackAttr.setBaseValue(harmonyFinalBaseAttack);
                    playerNBT.putDouble("BaseAttackDamage", harmonyFinalBaseAttack);
                }
                ModifiableAttributeInstance defenseAttr = player.getAttribute(Attributes.ARMOR);
                if (defenseAttr != null) {
                    if (!playerNBT.contains(NATURAL_ARMOR)) {
                        playerNBT.putDouble(NATURAL_ARMOR, defenseAttr.getBaseValue());
                    }
                    double currentDefense = playerNBT.getDouble(NATURAL_ARMOR);
                    double newDefense = currentDefense + incrementalDefense;
                    playerNBT.putDouble(NATURAL_ARMOR, newDefense);
                    double otherBonuses = 0.0;
                    if (playerNBT.contains("mountain_defense_adjustment")) otherBonuses += playerNBT.getDouble("mountain_defense_adjustment");
                    if (playerNBT.contains("defenseReduction_adjustment")) otherBonuses += playerNBT.getDouble("defenseReduction_adjustment");
                    double newDynamicArmor = newDefense + otherBonuses;
                    playerNBT.putDouble(DYNAMIC_ARMOR, newDynamicArmor);
                    defenseAttr.setBaseValue(newDefense);
                    AttributeModifier existingMod = defenseAttr.getModifier(DEFENSE_MODIFIER_UUID);
                    if (existingMod != null) {
                        defenseAttr.removeModifier(existingMod);
                    }
                    if (otherBonuses != 0) {
                        AttributeModifier dynamicModifier = new AttributeModifier(
                                DEFENSE_MODIFIER_UUID,
                                "HarmonyDynamicDefense",
                                otherBonuses,
                                AttributeModifier.Operation.ADDITION
                        );
                        defenseAttr.applyPersistentModifier(dynamicModifier);
                    }
                    playerNBT.putDouble("BaseArmor", newDefense);
                }
                updateSacrificedLevel(playerNBT, buffId, newSacrificeLevel);
                buffsData.remove(buffId);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                syncPlayerData(player);
                double currentHealth = healthAttr != null ? healthAttr.getValue() : 0;
                double currentAttack = attackAttr != null ? attackAttr.getValue() : 0;
                double currentDefense = defenseAttr != null ? defenseAttr.getValue() : 0;
                String buffName = I18n.format("buff.enhance." + buffId);
                if (existingSacrificeLevel == 0) {
                    sendHarmonySacrificeSuccessMessage(
                            player,
                            buffName,
                            incrementalHealth,
                            incrementalAttack,
                            incrementalDefense,
                            newSacrificeLevel,
                            levelDiff,
                            currentHealth,
                            currentAttack,
                            currentDefense
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
                            currentHealth,
                            currentAttack,
                            currentDefense
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
                    String buffName = I18n.format("buff.enhance." + buffId);
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
                    int newLevel = Math.min(currentLevel + 1, EnhanceCommand.MAX_ENHANCE_LEVEL); // +1级，不超过上限
                    newLevel = Math.max(newLevel, 1);
                    buffsData.putInt(EnhanceCommand.ENHANCE_LEVEL_TAG, newLevel);
                    playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                    String achievementMsg = I18n.format("message.achievement.peak_unlocked");
                    String levelUpMsg = I18n.format("command.enhance.level_up", newLevel);
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.GOLD + achievementMsg + "\n" +
                                    TextFormatting.GREEN + levelUpMsg),
                            player.getUniqueID()
                    );
                    playerNBT.putBoolean(PEAK_ACHIEVEMENT_TRIGGERED, true);
                    syncPlayerData(player);
                }
            } catch (Exception e) {
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
            if (Enhance.SACRIFICE_TRIGGER != null) {
                Enhance.SACRIFICE_TRIGGER.trigger(player);
            }
            int currentLevel = buffsData.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
            int newLevel = Math.min(currentLevel + 1, EnhanceCommand.MAX_ENHANCE_LEVEL);
            newLevel = Math.max(newLevel, 1);
            buffsData.putInt(EnhanceCommand.ENHANCE_LEVEL_TAG, newLevel);
            playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
            player.sendMessage(
                    new StringTextComponent(TextFormatting.GREEN + I18n.format(
                            "command.enhance.level_up", newLevel
                    )),
                    player.getUniqueID()
            );
            playerNBT.putBoolean(FIRST_SACRIFICE_MARK, true);
            syncPlayerData(player);
        } catch (Exception e) {
        }
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
            AttributeModifier existing = speedAttr.getModifier(DEFENSE_MODIFIER_UUID);
            if (existing != null) {
                speedAttr.removeModifier(existing);
            }
            entityData.remove(INTERNAL_ORIGINAL_SPEED);
        }
    }
    public static void restoreOriginalDefense(LivingEntity entity) {
        ModifiableAttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr == null) return;
        AttributeModifier existing = armorAttr.getModifier(DEFENSE_MODIFIER_UUID);
        if (existing != null) {
            armorAttr.removeModifier(existing);
        }
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains(ORIGINAL_DYNAMIC_ARMOR)) {
            nbt.putDouble(DYNAMIC_ARMOR, nbt.getDouble(ORIGINAL_DYNAMIC_ARMOR));
            nbt.remove(ORIGINAL_DYNAMIC_ARMOR);
        } else if (nbt.contains(DYNAMIC_ARMOR)) {
            nbt.remove(DYNAMIC_ARMOR);
        }
        if (nbt.contains(NATURAL_ARMOR)) {
            armorAttr.setBaseValue(nbt.getDouble(NATURAL_ARMOR));
        }
    }
    private static void sendAttackSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, double newBase, int newLevel) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        String message = I18n.format(
                "message.sacrifice.attack.success",
                buffName, newLevel, incStr, newBase
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message), player.getUniqueID());
    }
    private static void sendAttackOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, double newBase) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        int levelDiff = newLv - oldLv;
        String message = I18n.format(
                "message.sacrifice.attack.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, newBase
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message), player.getUniqueID());
    }
    private static void sendSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, float newMax, int newLevel) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        String message = I18n.format(
                "message.sacrifice.health.success",
                buffName, newLevel, incStr, newMax
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message), player.getUniqueID());
    }
    private static void sendSacrificeOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, float newMax) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.valueOf(incremental);
        int levelDiff = newLv - oldLv;
        String message = I18n.format(
                "message.sacrifice.health.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, newMax
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message), player.getUniqueID());
    }
    private static void sendDefenseSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, double newDef, int newLevel) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.format("%.1f", incremental);
        String message = I18n.format(
                "message.sacrifice.defense.success",
                buffName, newLevel, incStr, newDef
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message), player.getUniqueID());
    }
    private static void sendDefenseOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, double newDef) {
        String incStr = incremental % 1 == 0 ? String.valueOf((int) incremental) : String.format("%.1f", incremental);
        int levelDiff = newLv - oldLv;
        String message = I18n.format(
                "message.sacrifice.defense.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, newDef
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message), player.getUniqueID());
    }
    private static void sendSpeedSacrificeSuccessMessage(ServerPlayerEntity player, String buffName, float incremental, double newSpeed, int newLevel) {
        String incStr = String.format("%.2f", incremental);
        String speedStr = String.format("%.4f", newSpeed);
        String message = I18n.format(
                "message.sacrifice.speed.success",
                buffName, newLevel, incStr, speedStr
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message), player.getUniqueID());
    }
    private static void sendSpeedOverwriteMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv, float incremental, double newSpeed) {
        String incStr = String.format("%.2f", incremental);
        String speedStr = String.format("%.4f", newSpeed);
        int levelDiff = newLv - oldLv;
        String message = I18n.format(
                "message.sacrifice.speed.overwrite",
                buffName, oldLv, newLv, incStr, levelDiff, speedStr
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message), player.getUniqueID());
    }
    private static void sendHarmonySacrificeSuccessMessage(
            ServerPlayerEntity player,
            String buffName,
            float incHealth,
            float incAttack,
            float incDefense,
            int newLevel,
            int levelDiff,
            double currentHealth,
            double currentAttack,
            double currentDefense) {
        String message = I18n.format(
                "message.sacrifice.harmony.success",
                buffName, newLevel,
                incHealth, levelDiff,
                incAttack, levelDiff,
                incDefense, levelDiff,
                currentHealth, currentAttack, currentDefense  // 添加当前总值
        );
        player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message), player.getUniqueID());
    }
    private static void sendHarmonyOverwriteMessage(
            ServerPlayerEntity player,
            String buffName,
            int oldLv,
            int newLv,
            float incHealth,
            float incAttack,
            float incDefense,
            int levelDiff,
            double currentHealth,
            double currentAttack,
            double currentDefense) {
        String message = I18n.format(
                "message.sacrifice.harmony.overwrite",
                buffName, oldLv, newLv,  // 修复旧等级显示
                incHealth, levelDiff,    // 显示实际增量和等级差
                incAttack, levelDiff,
                incDefense, levelDiff,
                currentHealth, currentAttack, currentDefense  // 添加当前总值参数
        );
        player.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + message), player.getUniqueID());
    }
    private static void sendLowLevelRemoveMessage(ServerPlayerEntity player, String buffName, int oldLv, int newLv) {
        String message = I18n.format(
                "message.sacrifice.low_level",
                buffName, oldLv, newLv
        );
        player.sendMessage(new StringTextComponent(TextFormatting.YELLOW + message), player.getUniqueID());
    }
    private static void sendRemoveMessage(ServerPlayerEntity player, String buffName) {
        String message = I18n.format(
                "message.sacrifice.remove.normal",
                buffName
        );
        player.sendMessage(new StringTextComponent(TextFormatting.YELLOW + message), player.getUniqueID());
    }
    public String getBuffId() {
        return buffId;
    }
    public int getBuffLevel() {
        return buffLevel;
    }
}
