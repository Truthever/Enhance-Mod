package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.nbt.CompoundNBT;
import java.util.UUID;
public class MountainDefenseEffect extends BaseEffect {
    private static final UUID DEFENSE_MODIFIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public MountainDefenseEffect() {
        super(EffectType.BENEFICIAL, 0x808080);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            CompoundNBT nbt = entity.getPersistentData();
            if (!nbt.contains("naturalArmor")) {
                nbt.putDouble("naturalArmor", armorAttr.getBaseValue());
            }
            if (!nbt.contains("dynamicArmor")) {
                nbt.putDouble("dynamicArmor", armorAttr.getBaseValue());
            }
            if (!nbt.contains("originalDynamicArmor")) {
                nbt.putDouble("originalDynamicArmor", nbt.getDouble("dynamicArmor"));
            }
            double naturalArmor = nbt.getDouble("naturalArmor");
            double currentDynamicArmor = nbt.getDouble("dynamicArmor");
            double amount = amplifier + 0.5;
            double adjustment = amount;
            double newDynamicArmor = currentDynamicArmor + adjustment;
            applyAttributeModifier(armorAttr, DEFENSE_MODIFIER_UUID, adjustment, AttributeModifier.Operation.ADDITION);
            nbt.putDouble("dynamicArmor", newDynamicArmor);
        }
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            removeAttributeModifier(armorAttr, DEFENSE_MODIFIER_UUID);
        }
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains("originalDynamicArmor")) {
            nbt.putDouble("dynamicArmor", nbt.getDouble("originalDynamicArmor"));
            nbt.remove("originalDynamicArmor");
        } else if (nbt.contains("dynamicArmor")) {
            nbt.remove("dynamicArmor");
        }
    }
    private void applyAttributeModifier(ModifiableAttributeInstance attribute, UUID uuid, double amount, AttributeModifier.Operation operation) {
        AttributeModifier existingModifier = attribute.getModifier(uuid);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }

        AttributeModifier newModifier = new AttributeModifier(uuid, "mountain_defense_boost", amount, operation);
        attribute.applyNonPersistentModifier(newModifier);
    }
    private void removeAttributeModifier(ModifiableAttributeInstance attribute, UUID uuid) {
        AttributeModifier existingModifier = attribute.getModifier(uuid);
        if (existingModifier != null) {
            attribute.removeModifier(existingModifier);
        }
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
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