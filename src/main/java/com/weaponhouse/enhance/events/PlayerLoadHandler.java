package com.weaponhouse.enhance.events;
import com.weaponhouse.enhance.blocks.PortalActivator;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber
public class PlayerLoadHandler {
    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains("BaseMovementSpeed")) {
            float baseMovementSpeed = playerData.getFloat("BaseMovementSpeed");
            Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(baseMovementSpeed);
        }
        playerData.putFloat("dynamicArmor", 0.0f);
        try {
            for (EffectInstance effectInstance : player.getActivePotionEffects()) {
                if (effectInstance != null) {
                    Effect effect = effectInstance.getPotion();
                    player.removePotionEffect(effect);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) event.getWorld();
            PortalActivator.restoreAllPortals(world);
        }
    }
}
