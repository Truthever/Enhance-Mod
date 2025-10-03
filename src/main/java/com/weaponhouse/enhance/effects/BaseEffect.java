package com.weaponhouse.enhance.effects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
public abstract class BaseEffect extends Effect {
    public BaseEffect(EffectType type, int liquidColor) {
        super(type, liquidColor);
    }
    public abstract void applyEffect(LivingEntity entity, int amplifier);
    public abstract void removeEffect(LivingEntity entity, int amplifier);
    @Override
    public void performEffect(LivingEntity entity, int amplifier) {}
    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }
    @Override
    public void applyAttributesModifiersToEntity(LivingEntity entity, net.minecraft.entity.ai.attributes.AttributeModifierManager attributeMapIn, int amplifier) {
        super.applyAttributesModifiersToEntity(entity, attributeMapIn, amplifier);
        applyEffect(entity, amplifier);
    }
    @Override
    public void removeAttributesModifiersFromEntity(LivingEntity entity, net.minecraft.entity.ai.attributes.AttributeModifierManager attributeMapIn, int amplifier) {
        super.removeAttributesModifiersFromEntity(entity, attributeMapIn, amplifier);
        removeEffect(entity, amplifier);
    }
}
