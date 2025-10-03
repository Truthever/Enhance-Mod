package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.items.armor.EnhanceChestplate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Random;
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorDamageReduceHandler {
    private static final Random RANDOM = new Random();
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote || !(event.getEntityLiving() instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        float originalDamage = event.getAmount();
        if (!ArmorSetEventHandler.hasDamageReduceBoost(player)) {
            return;
        }
        ItemStack chestplate = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        if (!(chestplate.getItem() instanceof EnhanceChestplate) || !EnhanceChestplate.isInitialized(chestplate)) {
            return;
        }
        int reduceLevel = EnhanceChestplate.getDefenseReduceLevel(chestplate);
        if (reduceLevel <= 0 || reduceLevel > 6) {
            reduceLevel = 1;
        }
        float triggerChance = reduceLevel * 0.1F;
        float reduceRatio = reduceLevel * 0.1F;
        CompoundNBT playerNBT = player.getPersistentData();
        float damageAfterArmor = calculateDamageAfterArmor(player, playerNBT, originalDamage);
        if (RANDOM.nextFloat() <= triggerChance) {
            float finalDamage = damageAfterArmor * (1 - reduceRatio);
            event.setAmount(Math.max(0, finalDamage));
            sendReduceTriggeredMessage(player, reduceLevel, (int)(reduceRatio*100), originalDamage, finalDamage);
        } else {
            event.setAmount(damageAfterArmor);
        }
    }
    private static float calculateDamageAfterArmor(PlayerEntity player, CompoundNBT nbt, float originalDamage) {
        float naturalArmor = nbt.contains("naturalArmor") ? (float) nbt.getDouble("naturalArmor") : 0F;
        float dynamicArmor = nbt.contains("dynamicArmor") ? (float) nbt.getDouble("dynamicArmor") : 0F;
        float totalArmor = naturalArmor + dynamicArmor;
        float damageAfterArmor = totalArmor >= 0
                ? Math.max(0, originalDamage - totalArmor)
                : originalDamage - totalArmor;
        return damageAfterArmor;
    }
    private static void sendReduceTriggeredMessage(PlayerEntity player, int level, int reducePercent, float original, float finalDmg) {
        TranslationTextComponent message = new TranslationTextComponent(
                "message.enhance_defense_reduce.triggered",
                level,
                reducePercent,
                String.format("%.1f", original),
                String.format("%.1f", finalDmg)
        );
        player.sendMessage(message.mergeStyle(TextFormatting.GREEN), player.getUniqueID());
    }
}