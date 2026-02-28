package com.weaponhouse.enhance.invasion;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class InvasionEventHandlers {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof net.minecraft.entity.player.ServerPlayerEntity) {
            net.minecraft.entity.player.ServerPlayerEntity player = (net.minecraft.entity.player.ServerPlayerEntity) event.getEntityLiving();
            InvasionHandler.onPlayerDeath(player);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof net.minecraft.entity.player.ServerPlayerEntity) {
            net.minecraft.entity.player.ServerPlayerEntity player = (net.minecraft.entity.player.ServerPlayerEntity) event.getPlayer();
            InvasionHandler.onPlayerLogout(player);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof net.minecraft.entity.player.ServerPlayerEntity) {
            net.minecraft.entity.player.ServerPlayerEntity player = (net.minecraft.entity.player.ServerPlayerEntity) event.getPlayer();
            net.minecraft.nbt.CompoundNBT playerData = player.getPersistentData();
        }
    }

}