package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.nbt.CompoundNBT;
import java.util.UUID;
public class SwampDefenseEffect extends BaseEffect {
    private static final UUID SWAMP_DEFENSE_MODIFIER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    public SwampDefenseEffect() {
        super(EffectType.HARMFUL, 0x228B22);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr == null) return;
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
        int effectLevel = amplifier + 1;
        double reductionPerLevel = 1.0;
        double totalReduction = effectLevel * reductionPerLevel;
        double adjustment = -totalReduction;
        double currentDynamicArmor = nbt.getDouble("dynamicArmor");
        double newDynamicArmor = currentDynamicArmor + adjustment;
        nbt.putDouble("dynamicArmor", newDynamicArmor);
        applyAttributeModifier(armorAttr, SWAMP_DEFENSE_MODIFIER_UUID, adjustment, AttributeModifier.Operation.ADDITION);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        ModifiableAttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr == null) return;
        CompoundNBT nbt = entity.getPersistentData();
        removeAttributeModifier(armorAttr, SWAMP_DEFENSE_MODIFIER_UUID);
        if (nbt.contains("originalDynamicArmor")) {
            double originalDynamic = nbt.getDouble("originalDynamicArmor");
            nbt.putDouble("dynamicArmor", originalDynamic);
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
        AttributeModifier newModifier = new AttributeModifier(uuid, "swamp_defense_reduction", amount, operation);
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
        return false;
    }
    @Override
    public boolean shouldRenderHUD(EffectInstance effect) {
        return false;
    }
}
