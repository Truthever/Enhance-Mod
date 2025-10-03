package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
@Mod.EventBusSubscriber(modid = "enhance")
public class RicochetHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String RICOCHET_TAG = "ricochet";
    private static final float SPEED_DECAY = 0.8f;
    private static final float SEARCH_RANGE = 3.0f;
    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof AbstractArrowEntity)) return;
        AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
        if (!(arrow.getShooter() instanceof LivingEntity)) return;
        LivingEntity shooter = (LivingEntity) arrow.getShooter();
        if (!shooter.getPersistentData().contains(BUFF_TAG)) return;
        CompoundNBT buffs = shooter.getPersistentData().getCompound(BUFF_TAG);
        if (!buffs.contains(RICOCHET_TAG)) return;
        int maxBounces = buffs.getInt(RICOCHET_TAG);
        int currentBounces = arrow.getPersistentData().getInt("RicochetCount");
        if (currentBounces >= maxBounces) return;
        RayTraceResult hit = event.getRayTraceResult();
        Vector3d hitPos = hit.getHitVec();
        Entity hitEntity;
        if (hit.getType() == RayTraceResult.Type.ENTITY) {
            Entity tempEntity = ((EntityRayTraceResult) hit).getEntity();
            if (tempEntity instanceof LivingEntity) {
                hitEntity = tempEntity;
            } else {
                hitEntity = null;
                return;
            }
        } else {
            hitEntity = null;
            return;
        }
        World world = arrow.world;
        List<LivingEntity> targets = world.getEntitiesWithinAABB(
                LivingEntity.class,
                new AxisAlignedBB(hitPos, hitPos).grow(SEARCH_RANGE),
                e -> e != shooter && e != hitEntity
        );
        if (!targets.isEmpty()) {
            LivingEntity newTarget = targets.get(world.rand.nextInt(targets.size()));
            spawnRicochetArrow(arrow, hitPos, newTarget, currentBounces + 1);
        }
    }
    private static void spawnRicochetArrow(AbstractArrowEntity original, Vector3d originPos, LivingEntity target, int bounceCount) {
        World world = original.world;
        Vector3d targetPos = target.getPositionVec();
        Vector3d direction = targetPos.subtract(originPos).normalize()
                .scale(original.getMotion().length() * SPEED_DECAY);
        AbstractArrowEntity newArrow = new ArrowEntity(world, originPos.x, originPos.y, originPos.z);
        newArrow.setShooter(original.getShooter());
        newArrow.setMotion(direction);
        newArrow.setDamage(original.getDamage() * SPEED_DECAY);
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("RicochetCount", bounceCount);
        newArrow.readAdditional(nbt);
        world.addEntity(newArrow);
    }
}
