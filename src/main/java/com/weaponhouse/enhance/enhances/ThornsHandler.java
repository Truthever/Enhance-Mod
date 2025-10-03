package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class ThornsHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String THORNS_TAG = "thorns";
    private static final float THORNS_RATIO_PER_LEVEL = 0.05f;
    public static class ThornsDamageSource extends EntityDamageSource {
        public ThornsDamageSource(String damageTypeIn, Entity damageSourceEntityIn) {
            super(damageTypeIn, damageSourceEntityIn);
            this.setDamageBypassesArmor();
            this.setMagicDamage();
        }
        public boolean isThornsDamage() {
            return true;
        }
    }
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntityLiving();
        DamageSource source = event.getSource();
        Entity attacker = source.getImmediateSource();
        if (attacker == null || !(attacker instanceof LivingEntity)) {
            return;
        }
        LivingEntity livingAttacker = (LivingEntity) attacker;
        int thornsLevel = getThornsLevel(victim);
        if (thornsLevel <= 0) return;
        float originalDamage = event.getAmount();
        float thornsDamage = originalDamage * (thornsLevel * THORNS_RATIO_PER_LEVEL);
        DamageSource thornsSource = new ThornsDamageSource("thorns", victim);
        livingAttacker.attackEntityFrom(thornsSource, thornsDamage);
    }
    private static int getThornsLevel(LivingEntity entity) {
        CompoundNBT data = entity.getPersistentData();
        if (data.contains(BUFF_TAG)) {
            CompoundNBT buffs = data.getCompound(BUFF_TAG);
            if (buffs.contains(THORNS_TAG)) {
                return buffs.getInt(THORNS_TAG);
            }
        }
        return 0;
    }
}
