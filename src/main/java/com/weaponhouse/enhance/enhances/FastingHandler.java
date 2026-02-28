package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.Event;
public class FastingHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String FASTING_TAG = "fasting";
    private static final int BASE_DURATION_TICKS = 10 * 20;
    private static final int DURATION_PER_LEVEL_TICKS = 2 * 20;
    private static final String FASTING_END_TIME_TAG = "FastingEndTime";
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        LivingEntity target = (LivingEntity) event.getEntity();
        applyFastingEffect(attacker, target);
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity().world.isRemote) return;
        LivingEntity shooter = null;
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            shooter = (LivingEntity) arrow.getShooter();
        }
        else if (event.getEntity() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) event.getEntity();
            shooter = (LivingEntity) throwable.getShooter();
        }
        if (shooter == null) return;
        if (event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
            Entity hitEntity = ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
            if (hitEntity instanceof LivingEntity) {
                applyFastingEffect(shooter, (LivingEntity) hitEntity);
            }
        }
    }
    private static void applyFastingEffect(LivingEntity attacker, LivingEntity target) {
        int fastingLevel = getFastingLevel(attacker);
        if (fastingLevel <= 0) {
            return;
        }
        CompoundNBT targetData = target.getPersistentData();
        int totalDuration = BASE_DURATION_TICKS + (fastingLevel - 1) * DURATION_PER_LEVEL_TICKS;
        long endTime = target.world.getGameTime() + totalDuration;
        targetData.putLong(FASTING_END_TIME_TAG, endTime);
        if (target instanceof PlayerEntity) {
            int seconds = totalDuration / 20;
            ((PlayerEntity) target).sendStatusMessage(
                    new TranslationTextComponent(
                            "buff.fasting.apply_message",
                            seconds
                    ),
                    true
            );
        }
    }
    public static void onPlayerUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntity();
        ItemStack itemStack = event.getItem();
        if (itemStack.getItem().isFood()) {
            CompoundNBT playerData = player.getPersistentData();
            if (playerData.contains(FASTING_END_TIME_TAG, Constants.NBT.TAG_LONG)) {
                long endTime = playerData.getLong(FASTING_END_TIME_TAG);
                long currentTime = player.world.getGameTime();
                if (currentTime < endTime) {
                    event.setCanceled(true);
                    event.setResult(Event.Result.DENY);
                    int secondsLeft = (int) ((endTime - currentTime) / 20);
                    player.sendStatusMessage(
                            new TranslationTextComponent(
                                    "buff.fasting.remaining_time",
                                    secondsLeft
                            ),
                            true
                    );
                } else {
                    playerData.remove(FASTING_END_TIME_TAG);
                }
            }
        }
    }
    public static void onPlayerUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntity();
        ItemStack itemStack = event.getItem();
        if (itemStack.getItem().isFood()) {
            CompoundNBT playerData = player.getPersistentData();
            if (playerData.contains(FASTING_END_TIME_TAG, Constants.NBT.TAG_LONG)) {
                long endTime = playerData.getLong(FASTING_END_TIME_TAG);
                long currentTime = player.world.getGameTime();
                if (currentTime < endTime) {
                    event.setCanceled(true);
                    event.setResult(Event.Result.DENY);
                    player.stopActiveHand();
                    int secondsLeft = (int) ((endTime - currentTime) / 20);
                    player.sendStatusMessage(
                            new TranslationTextComponent(
                                    "buff.fasting.remaining_time",
                                    secondsLeft
                            ),
                            true
                    );
                } else {
                    playerData.remove(FASTING_END_TIME_TAG);
                }
            }
        }
    }
    private static int getFastingLevel(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG, Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT buffs = entityData.getCompound(BUFF_TAG);
            if (buffs.contains(FASTING_TAG, Constants.NBT.TAG_INT)) {
                return buffs.getInt(FASTING_TAG);
            }
        }
        return 0;
    }
}