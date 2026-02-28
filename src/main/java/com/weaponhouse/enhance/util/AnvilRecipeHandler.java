package com.weaponhouse.enhance.util;

import com.weaponhouse.enhance.items.*;
import com.weaponhouse.enhance.items.armor.EnhanceChestplate;
import com.weaponhouse.enhance.items.armor.EnhanceHelmet;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber
public class AnvilRecipeHandler {
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.getItem() instanceof EnhanceHelmet &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handleHelmetCharge(event, left, right);
        }
        else if (left.getItem() instanceof EnhanceChestplate &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handleChestplateCharge(event, left, right);
        }
        else if (left.getItem() instanceof EnhanceAxeItem &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handleAxeCharge(event, left, right);
        }
        else if (left.getItem() instanceof EnhanceHoeItem &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handleHoeCharge(event, left, right);
        }
        else if (left.getItem() instanceof EnhancePickaxeItem &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handlePickaxeCharge(event, left, right);
        }
        else if (left.getItem() instanceof EnhanceSwordItem &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handleSwordCharge(event, left, right);
        }
        else if (left.getItem() instanceof EndReturnStaffItem &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handleStaffCharge(event, left, right);
        }
        else if (left.getItem() instanceof DimensionalAnchorItem &&
                right.getItem() == RegistryHandler.ENHANCE_DUST.get()) {
            handleDimensionalAnchorCharge(event, left, right);
        }
    }
    private static void handleDimensionalAnchorCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!DimensionalAnchorItem.isInitialized(left)) {
            return;
        }
        int currentCharge = DimensionalAnchorItem.getCharge(left);
        int maxCharge = DimensionalAnchorItem.getMaxCharge();
        int chargePerPowder = DimensionalAnchorItem.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);
        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            DimensionalAnchorItem.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }
    private static void handleStaffCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!EndReturnStaffItem.isInitialized(left)) {
            return;
        }
        int currentCharge = EndReturnStaffItem.getCharge(left);
        int maxCharge = EndReturnStaffItem.getMaxCharge();
        int chargePerPowder = EndReturnStaffItem.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);
        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            EndReturnStaffItem.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }
    private static void handleHelmetCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!EnhanceHelmet.isInitialized(left)) {
            return;
        }
        int currentCharge = EnhanceHelmet.getCharge(left);
        int maxCharge = EnhanceHelmet.getMaxCharge();
        int chargePerPowder = EnhanceHelmet.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);
        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            EnhanceHelmet.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }
    private static void handleChestplateCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!EnhanceChestplate.isInitialized(left)) {
            return;
        }
        int currentCharge = EnhanceChestplate.getCharge(left);
        int maxCharge = EnhanceChestplate.getMaxCharge();
        int chargePerPowder = EnhanceChestplate.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);
        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            EnhanceChestplate.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }
    private static void handleAxeCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!EnhanceAxeItem.isInitialized(left)) {
            return;
        }
        int currentCharge = EnhanceAxeItem.getCharge(left);
        int maxCharge = EnhanceAxeItem.getMaxCharge();
        int chargePerPowder = EnhanceAxeItem.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);
        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            EnhanceAxeItem.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }
    private static void handleHoeCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!EnhanceHoeItem.isInitialized(left)) {
            return;
        }
        int currentCharge = EnhanceHoeItem.getCharge(left);
        int maxCharge = EnhanceHoeItem.getMaxCharge();
        int chargePerPowder = EnhanceHoeItem.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);

        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            EnhanceHoeItem.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }
    private static void handlePickaxeCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!EnhancePickaxeItem.isInitialized(left)) {
            return;
        }
        int currentCharge = EnhancePickaxeItem.getCharge(left);
        int maxCharge = EnhancePickaxeItem.getMaxCharge();
        int chargePerPowder = EnhancePickaxeItem.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);

        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            EnhancePickaxeItem.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }
    private static void handleSwordCharge(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!EnhanceSwordItem.isInitialized(left)) {
            return;
        }
        int currentCharge = EnhanceSwordItem.getCharge(left);
        int maxCharge = EnhanceSwordItem.getMaxCharge();
        int chargePerPowder = EnhanceSwordItem.getChargePerPowder();
        int neededCharge = maxCharge - currentCharge;
        int powderNeeded = (int) Math.ceil((double) neededCharge / chargePerPowder);
        int powderUsed = Math.min(right.getCount(), powderNeeded);
        if (powderUsed > 0) {
            ItemStack result = left.copy();
            int chargeToAdd = powderUsed * chargePerPowder;
            EnhanceSwordItem.addCharge(result, chargeToAdd);
            event.setOutput(result);
            event.setCost(powderUsed);
            event.setMaterialCost(powderUsed);
        }
    }

}