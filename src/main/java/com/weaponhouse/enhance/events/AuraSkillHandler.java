package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AuraSkillHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String AURA_TAG = "aura";
    private static final int BASE_DURATION = 10 * 20;
    private static final int DURATION_PER_LEVEL = 2 * 20;
    private static final int AURA_EFFECT_AMPLIFIER = 0;
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) return;
        if (!(event.getEntityLiving() instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        LivingEntity target = event.getEntityLiving();
        int auraLevel = getAuraLevel(attacker);
        if (auraLevel > 0) {
            applyAuraEffect(target, auraLevel);
        }
    }
    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent.Arrow event) {
        if (event.getArrow().world.isRemote) return;
        AbstractArrowEntity arrow = event.getArrow();
        if (!(arrow.getShooter() instanceof LivingEntity)) return;
        RayTraceResult traceResult = event.getRayTraceResult();
        if (traceResult.getType() != RayTraceResult.Type.ENTITY) return;
        EntityRayTraceResult entityTrace = (EntityRayTraceResult) traceResult;
        Entity hitEntity = entityTrace.getEntity();
        if (!(hitEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity shooter = (LivingEntity) arrow.getShooter();
        LivingEntity target = (LivingEntity) hitEntity;
        int auraLevel = getAuraLevel(shooter);
        if (auraLevel > 0) {
            applyAuraEffect(target, auraLevel);
        }
    }
    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntity();
        Effect auraEffect = EffectRegistry.AURA;
        EffectInstance activeEffect = player.getActivePotionEffect(auraEffect);
        if (activeEffect != null) {
            player.setMotion(player.getMotion().x, 0, player.getMotion().z);
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
        Effect auraEffect = EffectRegistry.AURA;
        int duration = BASE_DURATION + (level - 1) * DURATION_PER_LEVEL;
        target.removePotionEffect(auraEffect);
        target.addPotionEffect(new EffectInstance(
                auraEffect,
                duration,
                AURA_EFFECT_AMPLIFIER,
                false,
                true
        ));
    }
}
