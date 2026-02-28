package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
public class CurseDamageAmplifier {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String CURSE_TAG = "curse";
    private static final float DAMAGE_AMPLIFIER_PER_LEVEL = 0.03f;
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        int curseLevel = getCurseLevel(entity);
        if (curseLevel > 0) {
            float originalDamage = event.getAmount();
            float extraDamage = originalDamage * DAMAGE_AMPLIFIER_PER_LEVEL * curseLevel;
            float totalDamage = originalDamage + extraDamage;
            event.setAmount(totalDamage);
            spawnCurseParticles(entity);
        }
    }
    private static void spawnCurseParticles(LivingEntity entity) {
        if (!entity.world.isRemote()) {
            net.minecraft.world.server.ServerWorld serverWorld = (net.minecraft.world.server.ServerWorld) entity.world;
            net.minecraft.util.math.vector.Vector3d pos = entity.getPositionVec();
            for (int i = 0; i < 3; i++) {
                double x = pos.x + (serverWorld.rand.nextDouble() - 0.5) * entity.getWidth();
                double y = pos.y + serverWorld.rand.nextDouble() * entity.getHeight();
                double z = pos.z + (serverWorld.rand.nextDouble() - 0.5) * entity.getWidth();
                serverWorld.spawnParticle(
                        net.minecraft.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        x, y, z,
                        1,
                        0, 0.1, 0,
                        0.02
                );
            }
        }
    }
    private static int getCurseLevel(LivingEntity entity) {
        if (entity.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains(CURSE_TAG)) {
                return buffs.getInt(CURSE_TAG);
            }
        }
        return 0;
    }
}