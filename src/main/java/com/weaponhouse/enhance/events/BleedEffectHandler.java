package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.items.EnhanceAxeItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class BleedEffectHandler {
    private static final DamageSource BLEED_DAMAGE = new DamageSource("enhance.bleed")
            .setDamageBypassesArmor()
            .setDamageIsAbsolute();
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity instanceof PlayerEntity && !entity.world.isRemote) {
            return;
        }
        if (!entity.getPersistentData().contains(EnhanceAxeItem.BLEED_LEVEL_TAG)
                || !entity.getPersistentData().contains("bleed_duration")) {
            return;
        }
        int bleedLevel = entity.getPersistentData().getInt(EnhanceAxeItem.BLEED_LEVEL_TAG);
        int remainingDuration = entity.getPersistentData().getInt("bleed_duration");
        if (remainingDuration <= 0) {
            entity.getPersistentData().remove(EnhanceAxeItem.BLEED_LEVEL_TAG);
            entity.getPersistentData().remove("bleed_duration");
            entity.getPersistentData().remove("bleed_start_time");
            return;
        }
        if (remainingDuration % 20 == 0) {
            int damage = 2 * bleedLevel;
            entity.attackEntityFrom(BLEED_DAMAGE, damage);
            if (entity.world.isRemote) {
                spawnBloodParticles(entity);
            }
        }
        entity.getPersistentData().putInt("bleed_duration", remainingDuration - 1);
    }
    private static void spawnBloodParticles(LivingEntity entity) {
        for (int i = 0; i < 3; i++) {
            entity.world.addParticle(
                    ParticleTypes.BARRIER,
                    entity.getPosXRandom(0.5D),
                    entity.getPosY() + entity.getHeight() * 0.5D,
                    entity.getPosZRandom(0.5D),
                    1.0D, 0.0D, 0.0D // 红色粒子
            );
        }
    }
}
