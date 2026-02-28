package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
public class OceanEffect extends BaseEffect {
    private static final float MAX_BOOST_PERCENT = 2.0F;
    private static final float PER_LEVEL_BOOST = 0.05F;
    public OceanEffect() {
        super(EffectType.BENEFICIAL, 0x00FFFF);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {}
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {}
    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            int air = player.getAir();
            if (player.isInWater() && air < player.getMaxAir() * 0.8) {
                player.setAir(MathHelper.clamp(air + 1, 0, player.getMaxAir()));
            }
        }
        if (isValidSwimmingState(entity)) {
            applyDirectionalBoost(entity, amplifier);
        }
    }
    private boolean isValidSwimmingState(LivingEntity entity) {
        if (!entity.isInWater()) return false;
        if (entity instanceof PlayerEntity) {
            return !((PlayerEntity) entity).abilities.isFlying;
        }
        return true;
    }
    private void applyDirectionalBoost(LivingEntity entity, int amplifier) {
        float boostMultiplier = 1.0F + Math.min(amplifier * PER_LEVEL_BOOST, MAX_BOOST_PERCENT - 1.0F);
        Vector3d motion = entity.getMotion();
        double horizontalX = motion.x * boostMultiplier;
        double horizontalZ = motion.z * boostMultiplier;
        double verticalY = motion.y;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            boolean isJumping = player.moveVertical > 0;
            boolean isSneaking = player.isSneaking();
            if (isJumping || player.getMotion().y > 0.1) {
                verticalY = 0.08D * boostMultiplier;
            }
            else if (isSneaking) {
                verticalY = -0.06D * boostMultiplier;
            }
        }
        Vector3d newMotion = new Vector3d(horizontalX, verticalY, horizontalZ);
        newMotion = clampMaxSpeed(newMotion);
        entity.setMotion(newMotion);
        if (entity.world.isRemote && entity.isInWater()) {
            spawnSwimmingParticles(entity);
        }
    }
    private void spawnSwimmingParticles(LivingEntity entity) {
        if (entity.world.rand.nextInt(5) == 0) {
            Vector3d lookVec = entity.getLookVec();
            Vector3d position = entity.getPositionVec()
                    .add(lookVec.x * 0.5, entity.getEyeHeight() - 0.2, lookVec.z * 0.5);
            for (int i = 0; i < 3; i++) {
                double offsetX = (entity.world.rand.nextDouble() - 0.5) * 0.5;
                double offsetY = (entity.world.rand.nextDouble() - 0.5) * 0.5;
                double offsetZ = (entity.world.rand.nextDouble() - 0.5) * 0.5;

                entity.world.addParticle(net.minecraft.particles.ParticleTypes.BUBBLE,
                        position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                        offsetX * 0.1, offsetY * 0.1 + 0.02, offsetZ * 0.1);
            }
        }
    }
    private Vector3d clampMaxSpeed(Vector3d motion) {
        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontalSpeed > 0.3D * MAX_BOOST_PERCENT) {
            double scale = (0.3D * MAX_BOOST_PERCENT) / horizontalSpeed;
            return new Vector3d(motion.x * scale, motion.y, motion.z * scale);
        }
        return new Vector3d(
                motion.x,
                MathHelper.clamp(motion.y, -0.2D * MAX_BOOST_PERCENT, 0.25D * MAX_BOOST_PERCENT),
                motion.z
        );
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }
    @Override
    public String getName() {
        return "effect.enhance.ocean";
    }
}