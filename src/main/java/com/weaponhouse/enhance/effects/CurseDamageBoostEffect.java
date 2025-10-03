package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TranslationTextComponent;
import java.util.UUID;
public class CurseDamageBoostEffect extends Effect {
    private static final UUID DAMAGE_BOOST_UUID = UUID.fromString("1a3e3d7b-7a3d-4d3b-8a3d-3b7a3d3d3b7a");
    public CurseDamageBoostEffect() {
        super(EffectType.BENEFICIAL, 0xFF4500);
    }
    @Override
    public void applyAttributesModifiersToEntity(LivingEntity entity, AttributeModifierManager attributeMapIn, int amplifier) {
        ModifiableAttributeInstance attribute = entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            attribute.removeModifier(DAMAGE_BOOST_UUID);
            double bonusPercentage = (amplifier + 1) * 0.05;
            AttributeModifier modifier = new AttributeModifier(
                    DAMAGE_BOOST_UUID,
                    "Curse damage boost",
                    bonusPercentage,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            attribute.applyPersistentModifier(modifier);
        }
        super.applyAttributesModifiersToEntity(entity, attributeMapIn, amplifier);
    }
    @Override
    public void removeAttributesModifiersFromEntity(LivingEntity entity, AttributeModifierManager attributeMapIn, int amplifier) {
        ModifiableAttributeInstance attribute = entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            attribute.removeModifier(DAMAGE_BOOST_UUID);
        }
        super.removeAttributesModifiersFromEntity(entity, attributeMapIn, amplifier);
    }
    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (entity.world.getGameTime() % 20 == 0) {
            float damage = (amplifier + 1) * 0.5f;
            entity.attackEntityFrom(DamageSource.MAGIC, damage);
            if (entity instanceof PlayerEntity) {
                ((PlayerEntity) entity).sendStatusMessage(
                        new TranslationTextComponent(
                                "effect.curse.damage_message",
                                String.format("%.1f", damage)
                        ),
                        true
                );
            }
        }
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }
}
