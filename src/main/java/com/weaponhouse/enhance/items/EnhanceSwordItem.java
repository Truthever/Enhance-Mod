package com.weaponhouse.enhance.items;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import com.weaponhouse.enhance.commands.EnhanceCommand;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class EnhanceSwordItem extends SwordItem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("fa5c5c33-8a7a-4b8a-9b2a-2b3a9b4c5d6e");
    public static final String ENHANCE_STRIKE_TAG = "enhance_strike";
    private static final String INITIALIZED_TAG = "enhance_sword_initialized";
    private static final String FIXED_ATTACK_SPEED_TAG = "fixed_attack_speed_bonus";
    private static final int MAX_LEVEL = 6;
    public EnhanceSwordItem(IItemTier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder);
    }
    @Override
    public int getMaxDamage(ItemStack stack) {
        return 1500;
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !isSelected || !(entityIn instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
        if (player.getHeldItemMainhand() != stack) {
            return;
        }
        CompoundNBT swordNBT = stack.getOrCreateTag();
        if (swordNBT.getBoolean(INITIALIZED_TAG)) {
            applyFixedAttackSpeed(stack, player, swordNBT);
            return;
        }
        initializeSword(stack, player, swordNBT);
    }
    private void initializeSword(ItemStack stack, PlayerEntity player, CompoundNBT swordNBT) {
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        if (playerEnhanceLevel <= 0) {
            return;
        }
        int finalLevel = Math.min(playerEnhanceLevel, MAX_LEVEL);
        swordNBT.putInt(ENHANCE_STRIKE_TAG, finalLevel);
        float fixedAttackSpeed = calculateAttackSpeedBonus(finalLevel);
        swordNBT.putFloat(FIXED_ATTACK_SPEED_TAG, fixedAttackSpeed);
        swordNBT.putBoolean(INITIALIZED_TAG, true);
        stack.setTag(swordNBT);
        applyFixedAttackSpeed(stack, player, swordNBT);
    }
    private void applyFixedAttackSpeed(ItemStack stack, PlayerEntity player, CompoundNBT swordNBT) {
        float fixedSpeed = swordNBT.getFloat(FIXED_ATTACK_SPEED_TAG);
        AttributeModifier existingModifier = player.getAttribute(Attributes.ATTACK_SPEED).getModifier(ATTACK_SPEED_MODIFIER_ID);
        if (existingModifier != null) {
            player.getAttribute(Attributes.ATTACK_SPEED).removeModifier(existingModifier);
        }
        AttributeModifier speedModifier = new AttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Enhance sword fixed attack speed",
                fixedSpeed,
                AttributeModifier.Operation.ADDITION
        );
        player.getAttribute(Attributes.ATTACK_SPEED).applyPersistentModifier(speedModifier);
    }
    private float calculateAttackSpeedBonus(int level) {
        switch (level) {
            case 1: return 0.5F;
            case 2: return 1.0F;
            case 3: return 1.5F;
            case 4: return 2.0F;
            case 5: return 2.5F;
            case 6: return 10.0F;
            default: return 0.0F;
        }
    }
    private int getPlayerEnhanceLevel(PlayerEntity player) {
        if (player == null) return 0;
        CompoundNBT playerBuffNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerBuffNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        CompoundNBT swordNBT = stack.getTag();
        tooltip.add(new TranslationTextComponent("tooltip.enhance_sword.base_info")
                .mergeStyle(TextFormatting.BLUE));
        if (swordNBT != null && swordNBT.getBoolean(INITIALIZED_TAG)) {
            int strikeLevel = swordNBT.getInt(ENHANCE_STRIKE_TAG);
            float fixedSpeed = swordNBT.getFloat(FIXED_ATTACK_SPEED_TAG);
            int triggerChance = 3 * strikeLevel; // 3% * 等级
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_sword.enhance_strike_fixed",
                            strikeLevel, triggerChance
                    ).mergeStyle(TextFormatting.LIGHT_PURPLE)
            );
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_sword.fixed_attack_speed",
                            fixedSpeed
                    ).mergeStyle(TextFormatting.GREEN)
            );
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_sword.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
    }
    public static int getEnhanceStrikeLevel(ItemStack stack) {
        if (stack != null && stack.hasTag()) {
            CompoundNBT nbt = stack.getTag();
            if (nbt.getBoolean(INITIALIZED_TAG)) {
                return nbt.getInt(ENHANCE_STRIKE_TAG);
            }
        }
        return 0;
    }
}