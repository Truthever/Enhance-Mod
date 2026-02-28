package com.weaponhouse.enhance.effects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
public class DefenseReductionEffect extends BaseEffect {
    public DefenseReductionEffect() {
        super(EffectType.HARMFUL, 0xFF3333);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        CompoundNBT nbt = entity.getPersistentData();
        if (!nbt.contains("naturalArmor")) {
            nbt.putDouble("naturalArmor", 0.0D);
        }
        if (!nbt.contains("dynamicArmor")) {
            nbt.putDouble("dynamicArmor", nbt.getDouble("naturalArmor"));
        }
        if (!nbt.contains("originalDynamicArmor")) {
            nbt.putDouble("originalDynamicArmor", nbt.getDouble("dynamicArmor"));
        }
        int effectLevel = amplifier + 1;
        double reductionPerLevel = 2.0D;
        double totalReduction = effectLevel * reductionPerLevel;
        double adjustment = -totalReduction;
        double currentDynamicArmor = nbt.getDouble("dynamicArmor");
        double newDynamicArmor = currentDynamicArmor + adjustment;
        nbt.putDouble("dynamicArmor", newDynamicArmor);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains("originalDynamicArmor")) {
            double originalDynamic = nbt.getDouble("originalDynamicArmor");
            nbt.putDouble("dynamicArmor", originalDynamic);
            nbt.remove("originalDynamicArmor");
        } else if (nbt.contains("dynamicArmor")) {
            nbt.remove("dynamicArmor");
        }
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