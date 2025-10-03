package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class CurseHandler {
    private static final int CURSE_EFFECT_DURATION = 200;
    private static final String CURSE_TAG = "curse";
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source instanceof ThornsHandler.ThornsDamageSource &&
                ((ThornsHandler.ThornsDamageSource) source).isThornsDamage()) {
            return;
        }
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        int curseLevel = getCurseLevel(attacker);
        if (curseLevel <= 0) {
            return;
        }
        applyCurseDamageBoostEffect(attacker, curseLevel);
        applyCurseEffect(attacker, curseLevel);
    }
    @SubscribeEvent
    public static void onArrowShoot(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof AbstractArrowEntity)) {
            return;
        }
        AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
        if (!(arrow.getShooter() instanceof LivingEntity)) {
            return;
        }
        LivingEntity shooter = (LivingEntity) arrow.getShooter();
        int curseLevel = getCurseLevel(shooter);
        if (curseLevel <= 0) {
            return;
        }
        applyCurseDamageBoostEffect(shooter, curseLevel);
        applyCurseEffect(shooter, curseLevel);
    }
    private static void applyCurseDamageBoostEffect(LivingEntity entity, int curseLevel) {
        int amplifier = curseLevel - 1;
        EffectInstance newEffect = new EffectInstance(
                EffectRegistry.CURSEDAMAGE,
                5,
                amplifier,
                false,
                true
        );
        entity.addPotionEffect(newEffect);
    }
    private static void applyCurseEffect(LivingEntity entity, int curseLevel) {
        int amplifier = curseLevel - 1;
        EffectInstance existingEffect = entity.getActivePotionEffect(EffectRegistry.CURSE);
        if (existingEffect == null || existingEffect.getDuration() < 60) {
            EffectInstance newEffect = new EffectInstance(
                    EffectRegistry.CURSE,
                    CURSE_EFFECT_DURATION,
                    amplifier,
                    false,
                    true
            );
            entity.addPotionEffect(newEffect);
        }
    }
    private static int getCurseLevel(LivingEntity entity) {
        CompoundNBT data = entity.getPersistentData();
        if (data.contains(BUFF_TAG)) {
            CompoundNBT buffs = data.getCompound(BUFF_TAG);
            if (buffs.contains(CURSE_TAG)) {
                return Math.max(buffs.getInt(CURSE_TAG), 0);
            }
        }
        if (data.contains(EnhanceCommand.BUFF_TAG)) {
            CompoundNBT buffs = data.getCompound(EnhanceCommand.BUFF_TAG);
            return Math.max(buffs.getInt(CURSE_TAG), 0);
        }
        return 0;
    }
}