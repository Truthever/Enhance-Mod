package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.nbt.CompoundNBT;
@Mod.EventBusSubscriber(modid = "enhance")
public class FrostHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String FROST_TAG = "frost";
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            tryApplyFrost(attacker, event.getEntityLiving());
        }
    }
    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent.Arrow event) {
        AbstractArrowEntity arrow = event.getArrow();
        if (arrow.getShooter() instanceof LivingEntity) {
            LivingEntity shooter = (LivingEntity) arrow.getShooter();
            if (event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
                Entity target = ((EntityRayTraceResult)event.getRayTraceResult()).getEntity();
                if (target instanceof LivingEntity) {
                    tryApplyFrost(shooter, (LivingEntity)target);
                }
            }
        }
    }
    private static void tryApplyFrost(LivingEntity attacker, LivingEntity target) {
        if (attacker.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = attacker.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains(FROST_TAG)) {
                int level = buffs.getInt(FROST_TAG);
                applySlowness(target, level);
            }
        }
    }
    private static void applySlowness(LivingEntity target, int level) {
        int effectLevel = Math.min(3, (level - 1) / 5 + 1) - 1;
        int durationTicks = 20 * 2 * level;
        target.addPotionEffect(new EffectInstance(
                EffectRegistry.FROST,
                durationTicks,
                effectLevel,
                false,
                true
        ));
    }
}