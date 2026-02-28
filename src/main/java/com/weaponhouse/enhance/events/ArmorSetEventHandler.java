package com.weaponhouse.enhance.events;
import com.weaponhouse.enhance.items.armor.EnhanceBoots;
import com.weaponhouse.enhance.items.armor.EnhanceChestplate;
import com.weaponhouse.enhance.items.armor.EnhanceHelmet;
import com.weaponhouse.enhance.items.armor.EnhanceLeggings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorSetEventHandler {
    private static final String DAMAGE_REDUCE_BOOST_TAG = "enhance_damage_reduce_boost";
    private static final String NATURAL_ARMOR_TAG = "naturalArmor";
    private static final String DYNAMIC_ARMOR_TAG = "dynamicArmor";
    private static final String ORIGINAL_DYNAMIC_ARMOR_TAG = "originalDynamicArmor";
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        updateArmorSetEffects(player);
    }
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlayerEntity player = event.player;
            if (player.ticksExisted % 20 == 0) {
                updateArmorSetEffects(player);
            }
        }
    }
    private static void updateArmorSetEffects(PlayerEntity player) {
        int setCount = countEnhanceArmorPieces(player);
        boolean shouldApplyArmor = setCount >= 3;
        handleArmorBonus(player, shouldApplyArmor);
        boolean shouldApplyReduce = setCount >= 4;
        handleDamageReduceBoost(player, shouldApplyReduce);
    }
    private static int countEnhanceArmorPieces(PlayerEntity player) {
        int count = 0;
        ItemStack helmet = player.getItemStackFromSlot(EquipmentSlotType.HEAD);
        if (helmet.getItem() instanceof EnhanceHelmet) count++;
        ItemStack chestplate = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        if (chestplate.getItem() instanceof EnhanceChestplate) count++;
        ItemStack leggings = player.getItemStackFromSlot(EquipmentSlotType.LEGS);
        if (leggings.getItem() instanceof EnhanceLeggings) count++;
        ItemStack boots = player.getItemStackFromSlot(EquipmentSlotType.FEET);
        if (boots.getItem() instanceof EnhanceBoots) count++;
        return count;
    }
    private static void handleArmorBonus(PlayerEntity player, boolean shouldApply) {
        CompoundNBT nbt = player.getPersistentData();
        if (!nbt.contains(NATURAL_ARMOR_TAG)) {
            nbt.putDouble(NATURAL_ARMOR_TAG, 0.0D);
        }
        if (!nbt.contains(DYNAMIC_ARMOR_TAG)) {
            nbt.putDouble(DYNAMIC_ARMOR_TAG, nbt.getDouble(NATURAL_ARMOR_TAG));
        }
        if (shouldApply && !nbt.contains(ORIGINAL_DYNAMIC_ARMOR_TAG)) {
            nbt.putDouble(ORIGINAL_DYNAMIC_ARMOR_TAG, nbt.getDouble(DYNAMIC_ARMOR_TAG));
        }
        double defenseBonus = shouldApply ? 5.0D : 0.0D;
        double naturalArmor = nbt.getDouble(NATURAL_ARMOR_TAG);
        double newDynamicArmor = naturalArmor + defenseBonus;
        nbt.putDouble(DYNAMIC_ARMOR_TAG, newDynamicArmor);
        if (!shouldApply && nbt.contains(ORIGINAL_DYNAMIC_ARMOR_TAG)) {
            nbt.putDouble(DYNAMIC_ARMOR_TAG, nbt.getDouble(ORIGINAL_DYNAMIC_ARMOR_TAG));
            nbt.remove(ORIGINAL_DYNAMIC_ARMOR_TAG);
        }
    }
    private static void handleDamageReduceBoost(PlayerEntity player, boolean shouldApply) {
        CompoundNBT nbt = player.getPersistentData();
        if (shouldApply) {
            nbt.putBoolean(DAMAGE_REDUCE_BOOST_TAG, true);
        } else {
            nbt.remove(DAMAGE_REDUCE_BOOST_TAG);
        }
    }
    public static boolean hasDamageReduceBoost(PlayerEntity player) {
        return player.getPersistentData().getBoolean(DAMAGE_REDUCE_BOOST_TAG);
    }
}