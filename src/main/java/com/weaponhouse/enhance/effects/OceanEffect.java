package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import java.lang.reflect.Field;
public class OceanEffect extends BaseEffect {
    private static final float MAX_BOOST_PERCENT = 2.0F;
    private static final float PER_LEVEL_BOOST = 0.05F;
    private static Field jumpKeyField;
    private static Field sneakKeyField;
    static {
        try {
            jumpKeyField = ObfuscationReflectionHelper.findField(PlayerEntity.class, "field_71059_n"); // 对应jumpPressed
            sneakKeyField = ObfuscationReflectionHelper.findField(PlayerEntity.class, "field_71060_m"); // 对应sneakPressed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
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
            boolean isJumping = isPlayerJumping(player);
            boolean isSneaking = isPlayerSneaking(player);
            if (isJumping) {
                verticalY = 0.08D * boostMultiplier;
            }
            else if (isSneaking) {
                verticalY = -0.06D * boostMultiplier;
            }
        }
        Vector3d newMotion = new Vector3d(horizontalX, verticalY, horizontalZ);
        newMotion = clampMaxSpeed(newMotion);
        entity.setMotion(newMotion);
    }
    private boolean isPlayerJumping(PlayerEntity player) {
        try {
            if (jumpKeyField != null) {
                return jumpKeyField.getBoolean(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private boolean isPlayerSneaking(PlayerEntity player) {
        try {
            if (sneakKeyField != null) {
                return sneakKeyField.getBoolean(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return player.isSneaking();
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
        return true; // 每tick都应用助力
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
