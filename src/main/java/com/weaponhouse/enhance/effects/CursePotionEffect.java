package com.weaponhouse.enhance.effects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
public class CursePotionEffect extends BaseEffect {
    private static final int DAMAGE_INTERVAL = 20;
    public CursePotionEffect() {
        super(EffectType.HARMFUL, 0x8B0000);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
    }
    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (entity.ticksExisted % DAMAGE_INTERVAL != 0) {
            return;
        }
        float maxHealth = entity.getMaxHealth();
        float damage = maxHealth * 0.01f;
        entity.attackEntityFrom(DamageSource.MAGIC, MathHelper.clamp(damage, 0.1f, maxHealth));
        spawnCurseParticles(entity);
    }
    private void spawnCurseParticles(LivingEntity entity) {
        if (!entity.world.isRemote()) {
            net.minecraft.world.server.ServerWorld serverWorld = (net.minecraft.world.server.ServerWorld) entity.world;
            net.minecraft.util.math.vector.Vector3d pos = entity.getPositionVec();
            for (int i = 0; i < 3; i++) {
                double x = pos.x + (serverWorld.rand.nextDouble() - 0.5) * 0.8;
                double y = pos.y + serverWorld.rand.nextDouble() * entity.getHeight();
                double z = pos.z + (serverWorld.rand.nextDouble() - 0.5) * 0.8;
                serverWorld.spawnParticle(
                        net.minecraft.particles.ParticleTypes.SMOKE,
                        x, y, z,
                        1,
                        0, 0, 0,
                        0.05
                );
            }
        }
    }
    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
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