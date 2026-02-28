package com.weaponhouse.enhance.effects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectType;
public class MountainDefenseEffect extends BaseEffect {
    public MountainDefenseEffect() {
        super(EffectType.BENEFICIAL, 0x808080);
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
        double currentDynamicArmor = nbt.getDouble("dynamicArmor");
        double adjustment = amplifier + 0.5D;
        double newDynamicArmor = currentDynamicArmor + adjustment;
        nbt.putDouble("dynamicArmor", newDynamicArmor);
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        CompoundNBT nbt = entity.getPersistentData();
        if (nbt.contains("originalDynamicArmor")) {
            nbt.putDouble("dynamicArmor", nbt.getDouble("originalDynamicArmor"));
            nbt.remove("originalDynamicArmor");
        } else if (nbt.contains("dynamicArmor")) {
            nbt.remove("dynamicArmor");
        }
    }
}