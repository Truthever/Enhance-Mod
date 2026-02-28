package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;
public class DarknessEffect extends Effect {
    public DarknessEffect() {
        super(EffectType.HARMFUL, 0x000000);
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        int intervalSeconds = Math.max(1, amplifier);
        int intervalTicks = intervalSeconds * 20;
        return duration % intervalTicks < 20;
    }
    @Override
    public void performEffect(LivingEntity entityLiving, int amplifier) {
        if (entityLiving.world.isRemote) {
            return;
        }
        if (entityLiving instanceof PlayerEntity) {
            entityLiving.attackEntityFrom(DamageSource.MAGIC, 1.0F);
        }
        if (entityLiving instanceof MobEntity) {
            MobEntity mob = (MobEntity) entityLiving;
            DarknessManager.addDarknessEffect(mob, amplifier);
        }
    }
    @Override
    public String getName() {
        return "effect.enhance.darkness";
    }
}