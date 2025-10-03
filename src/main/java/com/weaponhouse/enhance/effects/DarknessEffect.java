package com.weaponhouse.enhance.effects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
public class DarknessEffect extends Effect {
    public DarknessEffect() {
        super(EffectType.HARMFUL, 0x000000);
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        int interval = Math.max(1, 20 - (amplifier + 1));
        return duration % (interval * 20) == 0;
    }
    @Override
    public void performEffect(LivingEntity entityLiving, int amplifier) {
        if (!entityLiving.world.isRemote) {
            return;
        }
    }
}
