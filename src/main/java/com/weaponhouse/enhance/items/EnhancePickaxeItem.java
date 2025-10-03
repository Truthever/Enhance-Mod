package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
public class EnhancePickaxeItem extends PickaxeItem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private static final String INITIALIZED_TAG = "hammer_initialized";
    public static final String HAMMER_STRIKE_TAG = "hammer_strike_level"; // 镐击等级标签
    public static final String AREA_MINING_TAG = "enhance_pickaxe_area_enabled";
    private static final float BASE_ATTACK_DAMAGE = 4.0F;
    public static final int MINING_RANGE = 1;
    public EnhancePickaxeItem() {
        super(
                RegistryHandler.ENHANCE_PICKAXE_TIER,
                1,
                -2.8F,
                new Properties()
                        .group(Enhance.TAB)
                        .addToolType(ToolType.PICKAXE, 6)
        );
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !(entityIn instanceof PlayerEntity) || isInitialized(stack)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
        CompoundNBT nbt = stack.getOrCreateTag();
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        int minLevel = 1;
        int maxLevel = 3 + playerEnhanceLevel;
        maxLevel = Math.min(maxLevel, 9);
        int hammerStrikeLevel = RANDOM.nextInt(maxLevel - minLevel + 1) + minLevel;
        nbt.putInt(HAMMER_STRIKE_TAG, hammerStrikeLevel);
        nbt.putBoolean(INITIALIZED_TAG, true);
        float totalAttackDamage = BASE_ATTACK_DAMAGE + (hammerStrikeLevel * 2.0F);
        nbt.putFloat("AttackDamage", totalAttackDamage);
        stack.setTag(nbt);
    }
    @Override
    public float getAttackDamage() {
        return BASE_ATTACK_DAMAGE;
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack pickaxe = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote && isInitialized(pickaxe)) {
            CompoundNBT nbt = pickaxe.getOrCreateTag();
            boolean newState = !nbt.getBoolean(AREA_MINING_TAG);
            nbt.putBoolean(AREA_MINING_TAG, newState);
            playerIn.sendMessage(new TranslationTextComponent(
                    newState ? "message.enhance_pickaxe.enabled" : "message.enhance_pickaxe.disabled"
            ).mergeStyle(newState ? TextFormatting.GREEN : TextFormatting.RED), playerIn.getUniqueID());
        }
        return new ActionResult<>(ActionResultType.SUCCESS, pickaxe);
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.base_info")
                .mergeStyle(TextFormatting.BLUE));
        if (isInitialized(stack)) {
            int strikeLevel = getHammerStrikeLevel(stack);
            int damageBonus = strikeLevel * 2;
            float totalDamage = BASE_ATTACK_DAMAGE + damageBonus;
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_pickaxe.total_damage",
                    totalDamage
            ).mergeStyle(TextFormatting.DARK_GREEN));
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_pickaxe.hammer_strike",
                            strikeLevel, damageBonus
                    ).mergeStyle(TextFormatting.LIGHT_PURPLE)
            );
        } else {
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_pickaxe.base_damage",
                    BASE_ATTACK_DAMAGE
            ).mergeStyle(TextFormatting.DARK_GREEN));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
        boolean isMiningEnabled = isAreaMiningEnabled(stack);
        tooltip.add(new TranslationTextComponent(
                        isMiningEnabled ? "tooltip.enhance_pickaxe.state_enabled" : "tooltip.enhance_pickaxe.state_disabled"
                ).mergeStyle(isMiningEnabled ? TextFormatting.GREEN : TextFormatting.RED)
        );
        tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.toggle_hint")
                .mergeStyle(TextFormatting.GRAY));
    }
    private int getPlayerEnhanceLevel(PlayerEntity player) {
        if (player == null) return 0;
        CompoundNBT playerBuffNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerBuffNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    public static boolean isInitialized(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
    }
    public static int getHammerStrikeLevel(ItemStack stack) {
        if (stack == null || !isInitialized(stack)) return 0;
        return stack.getTag().getInt(HAMMER_STRIKE_TAG);
    }
    public static boolean isAreaMiningEnabled(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean(AREA_MINING_TAG);
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_PICKAXE_TIER.getRepairMaterial().test(repair);
    }
}