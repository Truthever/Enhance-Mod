package com.weaponhouse.enhance.events;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber
public class PlayerLoadHandler {
    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains("BaseMovementSpeed")) {
            float baseMovementSpeed = playerData.getFloat("BaseMovementSpeed");
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseMovementSpeed);
        } else {
        }
        playerData.putFloat("dynamicArmor", 0.0f);
        try {
            for (EffectInstance effectInstance : player.getActivePotionEffects()) {
                if (effectInstance != null) {
                    Effect effect = effectInstance.getPotion();
                    if (effect != null) {
                        player.removePotionEffect(effect);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
