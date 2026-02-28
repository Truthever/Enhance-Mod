package com.weaponhouse.enhance.items;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
public class ThrownDaggerEntity extends ProjectileItemEntity {
    private static final float BASE_DAMAGE = 7.0f;
    private static final float THROW_VELOCITY = 2.5F;
    private static final float INACCURACY = 1.0F;
    private int powerLevel = 0;
    public ThrownDaggerEntity(EntityType<? extends ProjectileItemEntity> type, World world) {
        super(type, world);
    }
    public ThrownDaggerEntity(World world, LivingEntity thrower) {
        super(com.weaponhouse.enhance.util.RegistryHandler.THROWN_DAGGER.get(), thrower, world);
    }
    @Override
    protected Item getDefaultItem() {
        return com.weaponhouse.enhance.util.RegistryHandler.ENHANCE_DAGGER.get();
    }
    @Override
    public ItemStack getItem() {
        return new ItemStack(this.getDefaultItem());
    }
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.getType() == RayTraceResult.Type.ENTITY) {
            EntityRayTraceResult entityResult = (EntityRayTraceResult) result;
            entityResult.getEntity();
            if (entityResult.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entityResult.getEntity();
                LivingEntity shooter = (LivingEntity) this.getShooter();
                float damage = BASE_DAMAGE;
                if (powerLevel > 0) {
                    damage = damage + (damage * powerLevel * 0.25f);
                }
                if (shooter != null) {
                    target.attackEntityFrom(DamageSource.causeThrownDamage(this, shooter), damage);
                } else {
                    target.attackEntityFrom(DamageSource.causeThrownDamage(this, null), damage);
                }
            }
        }
        if (!this.world.isRemote) {
            if (!this.getPersistentData().getBoolean("IsRicochetCopy")) {
                ItemStack daggerStack = new ItemStack(com.weaponhouse.enhance.util.RegistryHandler.ENHANCE_DAGGER.get());
                this.entityDropItem(daggerStack, 0.1F);
            }
            this.remove();
        }
    }
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, THROW_VELOCITY, INACCURACY);
    }
    public void setPowerLevel(int powerLevel) {
        this.powerLevel = powerLevel;
        this.getPersistentData().putInt("PowerLevel", powerLevel);
    }
    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        if (compound.contains("PowerLevel")) {
            this.powerLevel = compound.getInt("PowerLevel");
        }
    }
    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putInt("PowerLevel", this.powerLevel);
    }
}