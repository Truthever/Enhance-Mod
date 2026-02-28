package com.weaponhouse.enhance.invasion;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class InvasionDropHandler {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.getPersistentData().getBoolean("enhance_invasion") ||
                entity.getPersistentData().getBoolean("invasion_removed")) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }
    public static void safelyRemoveInvasionMonster(LivingEntity monster) {
        if (monster == null || monster.removed) return;
        try {
            monster.getPersistentData().putBoolean("invasion_removed", true);
            monster.remove();
        } catch (Exception e) {
            try {
                monster.setHealth(0);
                monster.removed = true;
            } catch (Exception ignored) {}
        }
    }
}