package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class MegaForceHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String MEGAFORCE_TAG = "megaforce";
    private static final float DAMAGE_BONUS_PER_LEVEL = 0.10f;
    private static final float ARROW_SPEED_BONUS_PER_LEVEL = 0.05f;
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            applyMeleeBonus(attacker, event);
        }
    }
    @SubscribeEvent
    public static void onArrowShoot(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            if (arrow.getShooter() instanceof LivingEntity) {
                applyArrowSpeedBonus((LivingEntity) arrow.getShooter(), arrow);
            }
        }
    }
    private static void applyMeleeBonus(LivingEntity attacker, LivingDamageEvent event) {
        if (attacker.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = attacker.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains(MEGAFORCE_TAG)) {
                int level = buffs.getInt(MEGAFORCE_TAG);
                float newDamage = event.getAmount() * (1 + level * DAMAGE_BONUS_PER_LEVEL);
                event.setAmount(newDamage);

            }
        }
    }
    private static void applyArrowSpeedBonus(LivingEntity shooter, AbstractArrowEntity arrow) {
        if (shooter.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = shooter.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains(MEGAFORCE_TAG)) {
                int level = buffs.getInt(MEGAFORCE_TAG);
                arrow.setMotion(
                        arrow.getMotion().x * (1 + level * ARROW_SPEED_BONUS_PER_LEVEL),
                        arrow.getMotion().y * (1 + level * ARROW_SPEED_BONUS_PER_LEVEL),
                        arrow.getMotion().z * (1 + level * ARROW_SPEED_BONUS_PER_LEVEL)
                );
                arrow.setDamage(arrow.getDamage() * (1 + level * DAMAGE_BONUS_PER_LEVEL));
            }
        }
    }}
