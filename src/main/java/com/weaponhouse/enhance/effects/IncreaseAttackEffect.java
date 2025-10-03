package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.text.StringTextComponent;
import java.util.UUID;
public class IncreaseAttackEffect extends BaseEffect {
    private static final UUID ATTACK_EFFECT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SET_ATTACK_MODIFIER_UUID = UUID.fromString("82322174-5616-7200-2618-900000000001");
    public IncreaseAttackEffect() {
        super(EffectType.BENEFICIAL, 0xFFAA00);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double baseAttack = attackAttr.getBaseValue();
            AttributeModifier setAttackModifier = attackAttr.getModifier(SET_ATTACK_MODIFIER_UUID);
            if (setAttackModifier != null) {
                baseAttack += setAttackModifier.getAmount();
            }
            AttributeModifier existingModifier = attackAttr.getModifier(ATTACK_EFFECT_UUID);
            if (existingModifier != null) {
                attackAttr.removeModifier(existingModifier);
            }
            double increaseAmount = baseAttack * (0.05 * amplifier);
            AttributeModifier newModifier = new AttributeModifier(
                    ATTACK_EFFECT_UUID,
                    "increase_attack_potion_effect",
                    increaseAmount,
                    AttributeModifier.Operation.ADDITION
            );
            attackAttr.applyNonPersistentModifier(newModifier);
            double newAttack = attackAttr.getValue();
            entity.setCustomName(new StringTextComponent("攻击力: " + newAttack));
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            AttributeModifier existingModifier = attackAttr.getModifier(ATTACK_EFFECT_UUID);
            if (existingModifier != null) {
                attackAttr.removeModifier(existingModifier);
            }
            double newAttack = attackAttr.getValue();
            entity.setCustomName(new StringTextComponent("攻击力: " + newAttack));
        }
    }
}
