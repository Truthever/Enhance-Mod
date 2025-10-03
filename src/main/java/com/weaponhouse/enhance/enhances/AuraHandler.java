package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.effects.AuraEffect;
import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;
@Mod.EventBusSubscriber(modid = "enhance")
public class AuraHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String AURA_TAG = "aura";
    private static final int BASE_DURATION_TICKS = 20 * 10;
    private static final int DURATION_PER_LEVEL_TICKS = 20 * 2;
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (source instanceof ThornsHandler.ThornsDamageSource) {
            return;
        }
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) return;
        if (!(event.getEntityLiving() instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        LivingEntity target = (LivingEntity) event.getEntityLiving();
        int auraLevel = getAuraLevel(attacker);
        if (auraLevel > 0) {
            applyAuraEffect(target, auraLevel);
        }
    }
    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent.Arrow event) {
        AbstractArrowEntity arrow = event.getArrow();
        if (!(arrow.getShooter() instanceof LivingEntity)) {
            return;
        }
        LivingEntity shooter = (LivingEntity) arrow.getShooter();
        RayTraceResult traceResult = event.getRayTraceResult();
        if (traceResult.getType() != RayTraceResult.Type.ENTITY) {
            return;
        }
        EntityRayTraceResult entityTrace = (EntityRayTraceResult) traceResult;
        Entity hitEntity = entityTrace.getEntity();
        if (!(hitEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity) hitEntity;
        int auraLevel = getAuraLevel(shooter);
        if (auraLevel > 0) {
            applyAuraEffect(target, auraLevel);
        }
    }
    private static int getAuraLevel(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG, Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT buffs = entityData.getCompound(BUFF_TAG);
            if (buffs.contains(AURA_TAG, Constants.NBT.TAG_INT)) {
                return buffs.getInt(AURA_TAG);
            }
        }
        return 0;
    }
    private static void applyAuraEffect(LivingEntity target, int level) {
        if (EffectRegistry.AURA == null) {
            return;
        }
        int duration = BASE_DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL_TICKS;
        target.removePotionEffect(EffectRegistry.AURA);
        target.addPotionEffect(new EffectInstance(
                EffectRegistry.AURA,
                duration,
                AuraEffect.AURA_AMPLIFIER,
                false,
                true
        ));
    }
}
