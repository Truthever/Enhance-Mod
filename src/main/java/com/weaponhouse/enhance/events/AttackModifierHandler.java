package com.weaponhouse.enhance.events;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
@Mod.EventBusSubscriber
public class AttackModifierHandler {
    @SubscribeEvent
    public static void onEntityAttack(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        DamageSource source = event.getSource();
        if (source.getTrueSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) source.getTrueSource();
            CompoundNBT nbt = attacker.getPersistentData();
            if (nbt.contains("attack")) {
                float customAttackDamage = nbt.getFloat("attack");
                event.setAmount(customAttackDamage);
            }
        }
    }
}
