package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
public class HungrySecondEffect extends Effect {
    public HungrySecondEffect() {
        super(EffectType.HARMFUL, 0x884422);
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration % 40 == 0;
    }
    @Override
    public void performEffect(LivingEntity entityLiving, int amplifier) {
        if (!entityLiving.world.isRemote) {
            Effect darknessEffect = ForgeRegistries.POTIONS.getValue(new ResourceLocation("enhance:darkness"));
            if (darknessEffect != null && entityLiving.getActivePotionEffect(darknessEffect) == null) {
                entityLiving.addPotionEffect(new EffectInstance(
                        darknessEffect,
                        Integer.MAX_VALUE,
                        amplifier,
                        false,
                        true
                ));
            }
            Effect nauseaEffect = ForgeRegistries.POTIONS.getValue(new ResourceLocation("minecraft:nausea"));
            if (nauseaEffect != null && entityLiving.getActivePotionEffect(nauseaEffect) == null) {
                entityLiving.addPotionEffect(new EffectInstance(
                        nauseaEffect,
                        Integer.MAX_VALUE,
                        amplifier,
                        false,
                        true
                ));
            }
        }
    }
    @Override
    public void removeAttributesModifiersFromEntity(LivingEntity entityLiving, net.minecraft.entity.ai.attributes.AttributeModifierManager attributeManager, int amplifier) {
        super.removeAttributesModifiersFromEntity(entityLiving, attributeManager, amplifier);
        Effect darknessEffect = ForgeRegistries.POTIONS.getValue(new ResourceLocation("enhance:darkness"));
        if (darknessEffect != null && entityLiving.isPotionActive(darknessEffect)) {
            entityLiving.removePotionEffect(darknessEffect);
        }
        Effect nauseaEffect = ForgeRegistries.POTIONS.getValue(new ResourceLocation("minecraft:nausea"));
        if (nauseaEffect != null && entityLiving.isPotionActive(nauseaEffect)) {
            entityLiving.removePotionEffect(nauseaEffect);
        }
    }
}
