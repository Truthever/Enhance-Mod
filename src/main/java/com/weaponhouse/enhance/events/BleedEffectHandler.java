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
            float finalDamage = calculateBleedDamageWithResistance(entity, bleedLevel);
            if (!entity.world.isRemote && finalDamage > 0f) {
                float newHealth = entity.getHealth() - finalDamage;
                entity.setHealth(newHealth);
                if (newHealth <= 0.0F) {
                    entity.onDeath(BLEED_DAMAGE);
                }
            }
            if (entity.world.isRemote) {
                spawnBloodParticles(entity);
            }
        }
        entity.getPersistentData().putInt("bleed_duration", remainingDuration - 1);
    }
    private static float calculateBleedDamageWithResistance(LivingEntity entity, int baseDamage) {
        float base = Math.max(0f, (float) baseDamage);
        float dynamicArmor = 0f;
        if (entity.getPersistentData().contains("dynamicArmor")) {
            dynamicArmor = entity.getPersistentData().getFloat("dynamicArmor");
        }
        if (dynamicArmor < 0f) {
            dynamicArmor = 0f;
        }
        float afterBlock = base - dynamicArmor;
        float minDamage = base * 0.5f;
        float finalDamage = Math.max(afterBlock, minDamage);
        return Math.max(0f, finalDamage);
    }
    private static void spawnBloodParticles(LivingEntity entity) {
        for (int i = 0; i < 3; i++) {
            entity.world.addParticle(
                    ParticleTypes.BARRIER,
                    entity.getPosXRandom(0.5D),
                    entity.getPosY() + entity.getHeight() * 0.5D,
                    entity.getPosZRandom(0.5D),
                    1.0D, 0.0D, 0.0D
            );
        }
    }
}
