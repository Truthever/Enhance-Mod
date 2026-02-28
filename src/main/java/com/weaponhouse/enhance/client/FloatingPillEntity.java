package com.weaponhouse.enhance.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
public class FloatingPillEntity extends Entity {
    private static final DataParameter<ItemStack> ITEM_STACK = EntityDataManager.createKey(FloatingPillEntity.class, DataSerializers.ITEMSTACK);
    private static final DataParameter<BlockPos> FURNACE_POS = EntityDataManager.createKey(FloatingPillEntity.class, DataSerializers.BLOCK_POS);
    private int age = 0;
    private float hoverHeight = 3.0f;
    private float rotation = 0.0f;
    private float bobOffset;
    public FloatingPillEntity(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
        this.bobOffset = (float) (Math.random() * Math.PI * 2.0);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }
    public FloatingPillEntity(World world, BlockPos furnacePos, ItemStack pillStack) {
        this(com.weaponhouse.enhance.util.RegistryHandler.FLOATING_PILL_ENTITY.get(), world);
        this.setPosition(furnacePos.getX() + 0.5, furnacePos.getY() + hoverHeight, furnacePos.getZ() + 0.5);
        this.setItemStack(pillStack);
        this.setFurnacePos(furnacePos);
    }
    @Override
    protected void registerData() {
        this.dataManager.register(ITEM_STACK, ItemStack.EMPTY);
        this.dataManager.register(FURNACE_POS, BlockPos.ZERO);
    }
    @Override
    public void tick() {
        super.tick();
        this.age++;
        int maxAge = 20 * 60 * 5;
        if (this.age >= maxAge && !this.world.isRemote) {
            this.remove();
            return;
        }
        updateHoverAnimation();
        if (this.world.isRemote) {
            spawnParticles();
        }
        if (!this.world.isRemote) {
            checkPlayerCollision();
        }
    }
    private void updateHoverAnimation() {
        this.rotation += 2.0f;
        if (this.rotation >= 360.0f) {
            this.rotation -= 360.0f;
        }
        BlockPos furnacePos = getFurnacePos();
        double targetY = furnacePos.getY() + hoverHeight + Math.sin((this.age + bobOffset) * 0.1) * 0.2;
        double currentY = this.getPosY();
        double newY = currentY + (targetY - currentY) * 0.1;
        this.setPosition(this.getPosX(), newY, this.getPosZ());
    }
    private void spawnParticles() {
        if (this.world.getGameTime() % 5 == 0) {
            double x = this.getPosX() + (this.rand.nextDouble() - 0.5) * 0.5;
            double y = this.getPosY() + (this.rand.nextDouble() - 0.5) * 0.5;
            double z = this.getPosZ() + (this.rand.nextDouble() - 0.5) * 0.5;

            this.world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
        }
    }
    private void checkPlayerCollision() {
        this.world.getEntitiesWithinAABB(PlayerEntity.class, this.getBoundingBox())
                .stream()
                .findFirst()
                .ifPresent(player -> {
                    ItemStack pill = getItemStack().copy();
                    if (player.addItemStackToInventory(pill)) {
                        this.world.playSound(null, this.getPosition(), SoundEvents.ENTITY_ITEM_PICKUP,
                                SoundCategory.PLAYERS, 0.5f, 1.0f);
                        this.remove();
                    } else {
                        this.world.playSound(null, this.getPosition(), SoundEvents.BLOCK_NOTE_BLOCK_BASS,
                                SoundCategory.PLAYERS, 0.5f, 0.5f);
                    }
                });
    }
    @Override
    public boolean isGlowing() {
        return true;
    }
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    @Override
    protected void readAdditional(CompoundNBT compound) {
        this.age = compound.getInt("Age");
        this.hoverHeight = compound.getFloat("HoverHeight");
        this.rotation = compound.getFloat("Rotation");
        this.bobOffset = compound.getFloat("BobOffset");
        if (compound.contains("Item")) {
            this.setItemStack(ItemStack.read(compound.getCompound("Item")));
        }
        if (compound.contains("FurnacePos")) {
            this.setFurnacePos(BlockPos.fromLong(compound.getLong("FurnacePos")));
        }
    }
    @Override
    protected void writeAdditional(CompoundNBT compound) {
        compound.putInt("Age", this.age);
        compound.putFloat("HoverHeight", this.hoverHeight);
        compound.putFloat("Rotation", this.rotation);
        compound.putFloat("BobOffset", this.bobOffset);

        if (!getItemStack().isEmpty()) {
            compound.put("Item", getItemStack().write(new CompoundNBT()));
        }
        compound.putLong("FurnacePos", getFurnacePos().toLong());
    }
    @Nonnull
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    public ItemStack getItemStack() {
        return this.dataManager.get(ITEM_STACK);
    }
    public void setItemStack(ItemStack stack) {
        this.dataManager.set(ITEM_STACK, stack.copy());
    }
    public BlockPos getFurnacePos() {
        return this.dataManager.get(FURNACE_POS);
    }
    public void setFurnacePos(BlockPos pos) {
        this.dataManager.set(FURNACE_POS, pos);
    }
    public int getAge() {
        return age;
    }
}