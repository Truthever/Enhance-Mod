package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class ThunderHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final float BASE_CHANCE = 0.05f;
    private static final float CHANCE_PER_LEVEL = 0.03f;
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        LivingEntity target = (LivingEntity) event.getEntity();
        trySpawnLightning(attacker, target.getPosX(), target.getPosY(), target.getPosZ());
    }
    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent.Arrow event) {
        AbstractArrowEntity arrow = event.getArrow();
        if (!(arrow.getShooter() instanceof LivingEntity)) return;
        LivingEntity shooter = (LivingEntity) arrow.getShooter();
        if (event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
            Entity hitEntity = ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
            if (!(hitEntity instanceof LivingEntity)) {
                return;
            }
        }
        trySpawnLightning(shooter, arrow.getPosX(), arrow.getPosY(), arrow.getPosZ());
        trySpawnLightning(shooter, shooter.getPosX(), shooter.getPosY(), shooter.getPosZ());
    }
    private static void trySpawnLightning(LivingEntity entity, double x, double y, double z) {
        if (entity.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains("thunder")) {
                int level = buffs.getInt("thunder");
                if (entity.getEntityWorld().getRandom().nextFloat() < BASE_CHANCE + CHANCE_PER_LEVEL * level) {
                    spawnLightning(entity.getEntityWorld(), x, y + 1, z);
                }
            }
        }
    }
    private static void spawnLightning(World world, double x, double y, double z) {
        if (!world.isRemote) {
            LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            lightning.moveForced(x, y, z);
            world.addEntity(lightning);
        }
    }
}
