package com.weaponhouse.enhance.events;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber
public class DamageReductionHandler {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        CompoundNBT nbt = entity.getPersistentData();
        if (!nbt.contains("dynamicArmor")) {
            nbt.putFloat("dynamicArmor", 0f);
        }
        float dynamicArmor = nbt.getFloat("dynamicArmor");
        float originalDamage = event.getAmount();
        float finalDamage;
        if (dynamicArmor >= 0) {
            finalDamage = Math.max(0, originalDamage - dynamicArmor);
        } else {
            finalDamage = originalDamage + Math.abs(dynamicArmor);
        }
        event.setAmount(finalDamage);
    }
}
