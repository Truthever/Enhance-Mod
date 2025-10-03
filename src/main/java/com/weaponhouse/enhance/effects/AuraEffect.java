package com.weaponhouse.enhance.effects;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
public class AuraEffect extends Effect {
    public static final int AURA_AMPLIFIER = 0;
    public AuraEffect() {
        super(EffectType.HARMFUL, 0x6A0DAD);
    }
    @Override
    public boolean shouldRender(EffectInstance effect) {
        return true;
    }
    @Override
    public boolean shouldRenderHUD(EffectInstance effect) {
        return true;
    }
}