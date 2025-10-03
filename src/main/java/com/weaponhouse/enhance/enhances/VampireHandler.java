package com.weaponhouse.enhance.enhances;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Random;
@Mod.EventBusSubscriber(modid = "enhance")
public class VampireHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String VAMPIRE_TAG = "vampire";
    private static final float HEAL_PER_LEVEL = 2.0f;
    private static final float RANGE_HEAL_RATIO = 0.6f;
    private static final RedstoneParticleData RED_FLASH_PARTICLE = new RedstoneParticleData (1.0F, 0.0F, 0.0F, 2.0F);
    private static final float PARTICLE_LIFETIME = 0.05F;
    @SubscribeEvent
    public static void onLivingHurt (LivingHurtEvent event) {
        if (!(event.getEntityLiving ().world instanceof ServerWorld)) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld) event.getEntityLiving ().world;
        if (!(event.getSource ().getTrueSource () instanceof LivingEntity)) {
            return;
        }
        if (event.getSource () instanceof ThornsHandler.ThornsDamageSource) {
            return;
        }
        LivingEntity attacker = (LivingEntity) event.getSource().getTrueSource();
        float damageDealt = event.getAmount();
        int vampireLevel = getVampireLevel(attacker);
        if (vampireLevel <= 0 || damageDealt <= 0) {
            return;
        }
        boolean isRanged = isRangedAttack (event.getSource (), attacker);
        float baseHeal = isRanged ? HEAL_PER_LEVEL * RANGE_HEAL_RATIO : HEAL_PER_LEVEL;
        float healAmount = baseHeal * vampireLevel;
        float missingHealth = attacker.getMaxHealth () - attacker.getHealth ();
        healAmount = Math.min (healAmount, missingHealth);
        if (healAmount> 0) {
            attacker.heal (healAmount);
            spawnVampireFlashEffect (serverWorld, attacker);
            if (attacker instanceof PlayerEntity) {
                ((PlayerEntity) attacker).addExhaustion (0.1f * vampireLevel);
            }
        }

    }
    private static void spawnVampireFlashEffect (ServerWorld serverWorld, LivingEntity entity) {
        Vector3d entityPos = entity.getPositionVec ();
        float entityHeight = entity.getHeight ();
        Random rand = new Random ();
        int ringCount = 6;
        for (int i = 0; i < ringCount; i++) {
            double angle = i * Math.PI * 2 /ringCount;
            double radius = 0.7 + rand.nextDouble () * 0.3;
            double posX = entityPos.x + Math.cos (angle) * radius;
            double posY = entityPos.y + entityHeight * 0.5 + (rand.nextDouble () - 0.5) * 0.4;
            double posZ = entityPos.z + Math.sin (angle) * radius;
            serverWorld.spawnParticle (
                    RED_FLASH_PARTICLE,
                    posX, posY, posZ,
                    1,
                    0, 0, 0,
                    PARTICLE_LIFETIME
            );
        }
        int burstCount = 8;
        for (int i = 0; i < burstCount; i++) {
            double angle = i * Math.PI * 2 /burstCount;
            double radius = 0.3 + rand.nextDouble () * 0.5;
            double posX = entityPos.x + Math.cos (angle) * radius;
            double posY = entityPos.y + entityHeight + 0.2;
            double posZ = entityPos.z + Math.sin (angle) * radius;
            serverWorld.spawnParticle(
                    RED_FLASH_PARTICLE,
                    posX, posY, posZ,
                    1,
                    0, 0, 0,
                    PARTICLE_LIFETIME
            );
        }
    }
    private static boolean isRangedAttack (DamageSource source, LivingEntity attacker) {
        if (source.getImmediateSource () instanceof ProjectileEntity) {
            return true;
        }
        if (attacker instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) attacker;
            return player.getHeldItemMainhand ().getItem () instanceof BowItem
                    || player.getHeldItemMainhand ().getItem () instanceof CrossbowItem
                    || player.getHeldItemOffhand ().getItem () instanceof BowItem
                    || player.getHeldItemOffhand ().getItem () instanceof CrossbowItem;
        }
        return false;
    }
    private static int getVampireLevel (LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData ();
        if (entityData.contains (BUFF_TAG) && entityData.getCompound (BUFF_TAG).contains (VAMPIRE_TAG)) {
            return entityData.getCompound (BUFF_TAG).getInt (VAMPIRE_TAG);
        }
        return 0;
    }
}