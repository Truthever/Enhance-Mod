package com.weaponhouse.enhance.items.armor;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.enchant.RadiationEnchantment;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;
public class EnhanceHelmet extends ArmorItem {
    private static final String INITIALIZED_TAG = "enhance_helmet_initialized";
    private static final String NIGHT_VISION_ENABLED_TAG = "night_vision_enabled";
    private static final String LAST_GRANT_TIME_TAG = "last_night_vision_grant_time";
    private static final String LAST_TOGGLE_TIME_TAG = "last_toggle_time";
    private static final String CHARGE_TAG = "charge";
    private static final int MAX_CHARGE = 2000;
    private static final int INITIAL_CHARGE = MAX_CHARGE / 2;
    private static final int CHARGE_COST_PER_USE = 2;
    private static final int CHARGE_PER_POWDER = 200;
    private static final int NIGHT_VISION_DURATION = 400;
    private static final int GRANT_INTERVAL = 100;
    private static final int TOGGLE_COOLDOWN = 5;
    private static final int INIT_REQUIRED_LEVEL = 4;
    public EnhanceHelmet(IArmorMaterial material, EquipmentSlotType slot, Properties builder) {
        super(material, slot, builder);
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !(entityIn instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) entityIn;
        CompoundNBT nbt = stack.getOrCreateTag();
        if (!isInitialized(stack) && isMainHandSelected(player, stack)) {
            int playerLevel = getPlayerEnhanceLevel(player);
            if (playerLevel >= INIT_REQUIRED_LEVEL) {
                initializeHelmet(stack, nbt);
                player.sendMessage(new TranslationTextComponent("message.enhance_helmet.init_success")
                        .mergeStyle(TextFormatting.GREEN), player.getUniqueID());
            }
        }
        if (isInitialized(stack)) {
            boolean isEnabled = nbt.getBoolean(NIGHT_VISION_ENABLED_TAG);
            boolean isWearing = isHelmetEquipped(player, stack);
            long currentTime = worldIn.getGameTime();
            long lastGrantTime = nbt.getLong(LAST_GRANT_TIME_TAG);
            int currentCharge = getCharge(stack);
            if (isEnabled && isWearing && (currentTime - lastGrantTime >= GRANT_INTERVAL)) {
                if (currentCharge >= CHARGE_COST_PER_USE) {
                    applyNightVision(player);
                    consumeCharge(stack);
                    nbt.putLong(LAST_GRANT_TIME_TAG, currentTime);
                    stack.setTag(nbt);
                } else {
                    nbt.putBoolean(NIGHT_VISION_ENABLED_TAG, false);
                }
            }
        }
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (!(playerIn instanceof ServerPlayerEntity)) {
            return new ActionResult<>(ActionResultType.PASS, playerIn.getHeldItem(handIn));
        }
        ServerPlayerEntity player = (ServerPlayerEntity) playerIn;
        ItemStack stack = player.getHeldItem(handIn);
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND && isInitialized(stack)) {
            if (isLookingAtAir(player)) {
                if (toggleNightVision(stack, player, worldIn.getGameTime())) {
                    return new ActionResult<>(ActionResultType.SUCCESS, stack);
                }
            }
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }
    private boolean isLookingAtAir(ServerPlayerEntity player) {
        net.minecraft.util.math.RayTraceResult hitResult = player.pick(20.0D, 0.0F, false);
        return hitResult.getType() == net.minecraft.util.math.RayTraceResult.Type.MISS;
    }
    private static boolean toggleNightVision(ItemStack stack, ServerPlayerEntity player, long currentTime) {
        CompoundNBT nbt = stack.getOrCreateTag();
        long lastToggleTime = nbt.getLong(LAST_TOGGLE_TIME_TAG);
        if (currentTime - lastToggleTime < TOGGLE_COOLDOWN) {
            return false;
        }
        boolean newState = !nbt.getBoolean(NIGHT_VISION_ENABLED_TAG);
        if (newState && getCharge(stack) < CHARGE_COST_PER_USE) {
            return false;
        }
        nbt.putBoolean(NIGHT_VISION_ENABLED_TAG, newState);
        nbt.putLong(LAST_TOGGLE_TIME_TAG, currentTime);
        player.sendMessage(new TranslationTextComponent(
                newState ? "message.enhance_helmet.vision_on" : "message.enhance_helmet.vision_off"
        ).mergeStyle(newState ? TextFormatting.BLUE : TextFormatting.GRAY), player.getUniqueID());

        return true;
    }
    private void initializeHelmet(ItemStack stack, CompoundNBT nbt) {
        nbt.putBoolean(INITIALIZED_TAG, true);
        nbt.putBoolean(NIGHT_VISION_ENABLED_TAG, false);
        nbt.putLong(LAST_GRANT_TIME_TAG, 0);
        nbt.putLong(LAST_TOGGLE_TIME_TAG, 0);
        nbt.putInt(CHARGE_TAG, INITIAL_CHARGE);
        stack.setTag(nbt);
    }
    private void applyNightVision(ServerPlayerEntity player) {
        player.addPotionEffect(new EffectInstance(
                Effects.NIGHT_VISION,
                NIGHT_VISION_DURATION,
                0,
                false,
                false
        ));
    }
    private boolean isMainHandSelected(ServerPlayerEntity player, ItemStack stack) {
        return player.getHeldItemMainhand() == stack;
    }
    private boolean isHelmetEquipped(ServerPlayerEntity player, ItemStack stack) {
        ItemStack equipped = player.getItemStackFromSlot(EquipmentSlotType.HEAD);
        return equipped == stack;
    }
    private int getPlayerEnhanceLevel(ServerPlayerEntity player) {
        if (player == null) return 0;
        CompoundNBT playerNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    public static int getCharge(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return 0;
        if (stack.getTag() != null) {
            return stack.getTag().getInt(CHARGE_TAG);
        }
        return 0;
    }
    private static void consumeCharge(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return;
        int radiationLevel = RadiationEnchantment.getRadiationLevel(stack);
        if (RadiationEnchantment.shouldConserveCharge(stack, radiationLevel)) {
            return;
        }
        CompoundNBT nbt = stack.getTag();
        int currentCharge = 0;
        if (nbt != null) {
            currentCharge = nbt.getInt(CHARGE_TAG);
        }
        int newCharge = Math.max(0, currentCharge - EnhanceHelmet.CHARGE_COST_PER_USE);
        if (nbt != null) {
            nbt.putInt(CHARGE_TAG, newCharge);
        }
    }
    public static void addCharge(ItemStack stack, int amount) {
        if (stack == null || !stack.hasTag()) return;
        CompoundNBT nbt = stack.getTag();
        int currentCharge = 0;
        if (nbt != null) {
            currentCharge = nbt.getInt(CHARGE_TAG);
        }
        int newCharge = Math.min(MAX_CHARGE, currentCharge + amount);
        if (nbt != null) {
            nbt.putInt(CHARGE_TAG, newCharge);
        }
    }
    public static int getMaxCharge() {
        return MAX_CHARGE;
    }
    public static int getChargePerPowder() {
        return CHARGE_PER_POWDER;
    }
    public static boolean isInitialized(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
        }
        return false;
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_helmet.base_info")
                .mergeStyle(TextFormatting.BLUE));
        if (isInitialized(stack)) {
            CompoundNBT nbt = stack.getOrCreateTag();
            boolean isEnabled = nbt.getBoolean(NIGHT_VISION_ENABLED_TAG);
            int currentCharge = getCharge(stack);
            int maxCharge = getMaxCharge();
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_helmet.vision_state",
                            isEnabled ? "On" : "Off"
                    ).mergeStyle(isEnabled ? TextFormatting.BLUE : TextFormatting.GRAY)
            );
            TextFormatting chargeColor = currentCharge > maxCharge * 0.5 ? TextFormatting.GREEN :
                    currentCharge > maxCharge * 0.2 ? TextFormatting.YELLOW : TextFormatting.RED;
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_helmet.charge",
                            currentCharge, maxCharge
                    ).mergeStyle(chargeColor)
            );
            tooltip.add(new TranslationTextComponent("tooltip.enhance_helmet.charge_cost", CHARGE_COST_PER_USE)
                    .mergeStyle(TextFormatting.GRAY));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_helmet.toggle_hint_left")
                    .mergeStyle(TextFormatting.GRAY));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_helmet.anvil_repair")
                    .mergeStyle(TextFormatting.DARK_GREEN));
        } else {
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_helmet.init_hint",
                            INIT_REQUIRED_LEVEL
                    ).mergeStyle(TextFormatting.RED)
            );
        }
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_ARMOR_MATERIAL.getRepairMaterial().test(repair);
    }
}