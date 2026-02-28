package com.weaponhouse.enhance.entity;

import com.weaponhouse.enhance.util.RegistryHandler;
import com.weaponhouse.enhance.enhances.MobEnhancementHandler;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
public class ChaoticMerchantEntity extends AbstractVillagerEntity {
    private static final float BASE_HEALTH = 50.0f;
    private static final float BASE_ATTACK_DAMAGE = 4.0f;
    private static final float BASE_MOVEMENT_SPEED = 0.4f;
    private static final float NATURAL_ARMOR = 2.0f;
    private static final float RANGED_DAMAGE_BONUS = 0.5f;
    private boolean hasCustomWeapon = false;
    private int attackCooldown = 0;
    private boolean isTrading = false;
    private long spawnTime = -1;
    private int enhancementTier = 2;
    private int forcedMovementTimer = 0;
    private double lastPosX = 0;
    private double lastPosZ = 0;
    private int stuckCounter = 0;
    private boolean isSwimming = false;
    private int swimTimer = 0;
    private int breathTimer = 0;
    private final int MAX_BREATH = 300; // 15秒的呼吸时间
    private boolean hasWaterBreathing = false;
    private int landCheckTimer = 0;
    private boolean isTryingToLand = false;
    public ChaoticMerchantEntity(EntityType<? extends AbstractVillagerEntity> type, World world) {
        super(type, world);
        this.setCanPickUpLoot(false);
        this.enablePersistence();
        ((GroundPathNavigator) this.getNavigator()).setBreakDoors(true);
        this.navigator.setCanSwim(true);
        if (!world.isRemote) {
            Random random = world.getRandom();
            this.enhancementTier = random.nextBoolean() ? 2 : 3;
            lastPosX = this.getPosX();
            lastPosZ = this.getPosZ();
            forcedMovementTimer = random.nextInt(80);
        }
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            public boolean shouldExecute() {
                return getAttackTarget() != null && getAttackTarget().isAlive() && super.shouldExecute();
            }
            @Override
            public boolean shouldContinueExecuting() {
                return getAttackTarget() != null && getAttackTarget().isAlive() && super.shouldContinueExecuting();
            }
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return 4.0F + entity.getWidth();
            }
        });
        this.goalSelector.addGoal(2, new Goal() {
            @Override
            public boolean shouldExecute() {
                return isSwimming && !isTrading && !navigator.hasPath();
            }
            @Override
            public void tick() {
                tryToFindAndMoveToLand((ServerWorld) world);
            }
        });
        this.goalSelector.addGoal(3, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, LivingEntity.class, 20, true, true,
                new UndeadPredicate()) {
            @Override
            public boolean shouldExecute() {
                return !isTrading && super.shouldExecute();
            }
            @Override
            public boolean shouldContinueExecuting() {
                return getAttackTarget() != null && getAttackTarget().isAlive() && super.shouldContinueExecuting();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, LivingEntity.class, 20, true, false,
                new MonsterPredicate()) {
            @Override
            public boolean shouldExecute() {
                return !isTrading && super.shouldExecute();
            }
            @Override
            public boolean shouldContinueExecuting() {
                return getAttackTarget() != null && getAttackTarget().isAlive() && super.shouldContinueExecuting();
            }
        });
        HurtByTargetGoal hurtGoal = new HurtByTargetGoal(this) {
            @Override
            public boolean shouldExecute() {
                return super.shouldExecute();
            }
            @Override
            public boolean shouldContinueExecuting() {
                return super.shouldContinueExecuting();
            }
        };
        hurtGoal.setCallsForHelp();
        this.targetSelector.addGoal(3, hurtGoal);
    }
    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, BASE_HEALTH)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 60.0D)
                .createMutableAttribute(Attributes.ARMOR, NATURAL_ARMOR);
    }
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.world.isRemote) {
            ServerWorld serverWorld = (ServerWorld) this.world;
            this.spawnTime = serverWorld.getGameTime();
            this.setHealth(BASE_HEALTH);
            this.initializeNBTData();
            this.applyEnhancement();
            this.addTag("chaotic_merchant");
        }
    }
    @Override
    public void tick() {
        super.tick();
        if (!this.world.isRemote && this.world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) this.world;
            handleSwimming(serverWorld);
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            checkAndDespawn(serverWorld);
            if (!isTrading) {
                aggroNearbyUndead(serverWorld);
            }
            if (!isTrading && this.ticksExisted % 10 == 0 && this.getAttackTarget() == null) {
                findAndSetUndeadTarget(serverWorld);
            }
            if (!isTrading && getAttackTarget() != null && !this.navigator.hasPath()) {
                this.navigator.tryMoveToEntityLiving(getAttackTarget(), 1.2D);
            }
            if (!isTrading && !isSwimming) {
                forcedMovementTimer++;
                if (forcedMovementTimer > 30 + this.rand.nextInt(50)) {
                    forcedMovementTimer = 0;
                    double currentX = this.getPosX();
                    double currentZ = this.getPosZ();
                    double distanceMoved = Math.sqrt(
                            (currentX - lastPosX) * (currentX - lastPosX) +
                                    (currentZ - lastPosZ) * (currentZ - lastPosZ)
                    );
                    if (distanceMoved < 0.5) {
                        stuckCounter++;
                    } else {
                        stuckCounter = 0;
                    }
                    lastPosX = currentX;
                    lastPosZ = currentZ;
                    boolean needsForcedMove = false;
                    if (!this.navigator.hasPath()) {
                        needsForcedMove = true;
                    } else if (stuckCounter >= 2) {
                        needsForcedMove = true;
                        stuckCounter = 0;
                    }
                    if (needsForcedMove) {
                        double angle = this.rand.nextDouble() * Math.PI * 2;
                        double distance = 8 + this.rand.nextDouble() * 16;
                        double targetX = this.getPosX() + Math.sin(angle) * distance;
                        double targetZ = this.getPosZ() + Math.cos(angle) * distance;
                        double targetY = this.getPosY();
                        boolean pathFound = this.navigator.tryMoveToXYZ(targetX, targetY, targetZ, 0.7D);
                        if (!pathFound) {
                            distance = 4 + this.rand.nextDouble() * 8;
                            targetX = this.getPosX() + Math.sin(angle) * distance;
                            targetZ = this.getPosZ() + Math.cos(angle) * distance;
                            targetY = this.getPosY();
                            this.navigator.tryMoveToXYZ(targetX, targetY, targetZ, 0.7D);
                        }
                    }
                }
            } else if (isTrading && this.getAttackTarget() != null) {
                this.endTradingForCombat();
            }
            landCheckTimer++;
            if (landCheckTimer >= 20) {
                landCheckTimer = 0;
                if (isTryingToLand && !isSwimming) {
                    isTryingToLand = false;
                    this.navigator.clearPath();
                }
            }
        }
    }
    private void handleSwimming(ServerWorld world) {
        boolean inWater = this.isInWater() || this.areEyesInFluid(FluidTags.WATER);
        boolean inLava = this.areEyesInFluid(FluidTags.LAVA);
        this.isSwimming = (inWater || inLava) && !this.onGround;
        if (this.isSwimming) {
            swimTimer++;
            if (!this.hasWaterBreathing) {
                handleBreathing(inWater, inLava);
            }
            handleSwimMovement(world, inWater, inLava);
            if (this.ticksExisted % 5 == 0 && !this.isTrading) {
                tryToFindAndMoveToLand(world);
            }
            if (inLava) {
                this.addPotionEffect(new net.minecraft.potion.EffectInstance(
                        net.minecraft.potion.Effects.FIRE_RESISTANCE,
                        100,
                        0,
                        false,
                        false
                ));
            }
        } else {
            swimTimer = 0;
            breathTimer = MAX_BREATH;
            isTryingToLand = false;
        }
    }
    private void handleBreathing(boolean inWater, boolean inLava) {
        if (inLava) {
            breathTimer -= 10;
        } else if (inWater) {
            breathTimer--;
        }
        if (breathTimer <= 0) {
            breathTimer = 0;
            if (this.ticksExisted % 80 == 0) {
                this.attackEntityFrom(net.minecraft.util.DamageSource.DROWN, 1.0F);
                if (this.rand.nextFloat() < 0.1F) {
                    this.playSound(net.minecraft.util.SoundEvents.ENTITY_PLAYER_HURT_DROWN,
                            1.0F,
                            1.0F
                    );
                }
            }
            if (this.ticksExisted % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    double offsetX = (this.rand.nextDouble() - 0.5) * 0.5;
                    double offsetY = this.rand.nextDouble() * 0.5;
                    double offsetZ = (this.rand.nextDouble() - 0.5) * 0.5;
                    world.addParticle(
                            net.minecraft.particles.ParticleTypes.BUBBLE,
                            this.getPosX() + offsetX,
                            this.getPosY() + offsetY + this.getEyeHeight(),
                            this.getPosZ() + offsetZ,
                            0, 0.1, 0
                    );
                }
            }
        } else if (breathTimer < 100 && this.ticksExisted % 10 == 0) {
            world.addParticle(
                    net.minecraft.particles.ParticleTypes.BUBBLE,
                    this.getPosX() + (this.rand.nextDouble() - 0.5) * 0.3,
                    this.getPosY() + this.getEyeHeight(),
                    this.getPosZ() + (this.rand.nextDouble() - 0.5) * 0.3,
                    0, 0.1, 0
            );
        }
        if (!this.areEyesInFluid(FluidTags.WATER) && breathTimer < MAX_BREATH) {
            breathTimer += 2;
        }
    }
    private void handleSwimMovement(ServerWorld world, boolean inWater, boolean inLava) {
        if (this.getAttackTarget() == null) {
            this.navigator.clearPath();
        }
        if (this.getAttackTarget() != null && !this.isTrading) {
            LivingEntity target = this.getAttackTarget();
            if (!this.navigator.hasPath()) {
                this.navigator.tryMoveToEntityLiving(target, 1.0D);
            }
            if (!this.navigator.hasPath() || this.getDistanceSq(target) < 16.0) {
                double diffX = target.getPosX() - this.getPosX();
                double diffY = target.getPosY() - this.getPosY();
                double diffZ = target.getPosZ() - this.getPosZ();
                double speed = 0.15 * (inLava ? 0.5 : 1.0);
                this.moveRelative(0.1F, new Vector3d(diffX, diffY, diffZ).normalize().scale(speed));
            }
        }
    }
    private void tryToFindAndMoveToLand(ServerWorld world) {
        if (isTrading || !isSwimming) return;
        isTryingToLand = true;
        BlockPos nearestLand = findNearestLand(world);
        if (nearestLand != null) {
            BlockPos targetPos = getSafeLandPosition(world, nearestLand);
            if (targetPos != null) {
                if (!this.navigator.hasPath()) {
                    boolean pathFound = this.navigator.tryMoveToXYZ(
                            targetPos.getX() + 0.5,
                            targetPos.getY(),
                            targetPos.getZ() + 0.5,
                            1.0D
                    );
                    if (!pathFound) {
                        BlockPos waterEdge = findWaterEdge(world, nearestLand);
                        if (waterEdge != null) {
                            this.navigator.tryMoveToXYZ(
                                    waterEdge.getX() + 0.5,
                                    waterEdge.getY(),
                                    waterEdge.getZ() + 0.5,
                                    0.8D
                            );
                        }
                    }
                }
                double currentY = this.getPosY();
                double targetY = targetPos.getY();
                if (currentY < targetY + 1.0) {
                    double upForce = Math.min(0.05, (targetY - currentY) * 0.1);
                    this.setMotion(this.getMotion().add(0, upForce, 0));
                }
                double diffX = targetPos.getX() + 0.5 - this.getPosX();
                double diffZ = targetPos.getZ() + 0.5 - this.getPosZ();
                this.rotationYaw = (float) (MathHelper.atan2(diffZ, diffX) * (180D / Math.PI)) - 90.0F;
                this.renderYawOffset = this.rotationYaw;
            }
        }
    }
    private BlockPos findNearestLand(ServerWorld world) {
        int centerX = (int) Math.floor(this.getPosX());
        int centerY = (int) Math.floor(this.getPosY());
        int centerZ = (int) Math.floor(this.getPosZ());
        BlockPos bestPos;
        double bestDistance = Double.MAX_VALUE;
        for (int r = 1; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) == r || Math.abs(dz) == r) {
                        int x = centerX + dx;
                        int z = centerZ + dz;
                        int startY = Math.min(centerY + 8, world.getHeight() - 1);
                        int endY = Math.max(centerY - 8, 0);
                        for (int y = startY; y >= endY; y--) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (isStandableLand(world, pos)) {
                                double distance = pos.distanceSq(this.getPosX(), this.getPosY(), this.getPosZ(), true);
                                if (distance < bestDistance) {
                                    bestPos = pos;
                                    return bestPos;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    private BlockPos findWaterEdge(ServerWorld world, BlockPos landPos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos checkPos = landPos.add(dx, 0, dz);
                if (world.getFluidState(checkPos).isTagged(FluidTags.WATER) &&
                        world.getBlockState(checkPos.down()).isSolid()) {
                    return checkPos;
                }
            }
        }
        return landPos;
    }
    private BlockPos getSafeLandPosition(ServerWorld world, BlockPos landPos) {
        if (isStandableLand(world, landPos)) {
            return landPos;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos checkPos = landPos.add(dx, dy, dz);
                    if (isStandableLand(world, checkPos)) {
                        return checkPos;
                    }
                }
            }
        }
        return landPos;
    }
    private boolean isStandableLand(ServerWorld world, BlockPos pos) {
        BlockPos headPos = pos.up();
        BlockPos aboveHead = pos.up(2);
        return world.getBlockState(pos).isSolid() &&
                !world.getBlockState(headPos).isSolid() &&
                !world.getBlockState(aboveHead).isSolid() &&
                !world.getFluidState(pos).isTagged(FluidTags.WATER);
    }
    @Override
    public void travel(Vector3d travelVector) {
        if (this.isSwimming && !this.isTrading) {
            if (this.isServerWorld()) {
                double buoyancy = this.isInLava() ? 0.005 : 0.02;
                this.setMotion(this.getMotion().add(0, buoyancy, 0));
                super.travel(travelVector);
                Vector3d motion = this.getMotion();
                if (this.isInWater()) {
                    motion = motion.scale(0.8);
                } else if (this.isInLava()) {
                    motion = motion.scale(0.5);
                }
                this.setMotion(motion);
                if (this.getPosY() < world.getSeaLevel() && this.areEyesInFluid(FluidTags.WATER)) {
                    this.setMotion(this.getMotion().add(0, 0.01, 0));
                }
            }
        } else {
            super.travel(travelVector);
        }
    }
    @Override
    public boolean isInWater() {
        return super.isInWater();
    }
    @Override
    public boolean isSwimming() {
        return this.isSwimming;
    }
    private void endTradingForCombat() {
        if (!isTrading) return;
        this.setCustomer(null);
        LivingEntity attacker = this.getAttackTarget();
        if (attacker != null) {
            this.setAttackTarget(attacker);

            if (!this.navigator.hasPath()) {
                this.navigator.tryMoveToEntityLiving(attacker, 1.2D);
            }
        }
        forcedMovementTimer = 0;
        stuckCounter = 0;
    }
    private void findAndSetUndeadTarget(ServerWorld world) {
        if (isTrading) {
            return;
        }
        LivingEntity nearestUndead = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : world.getEntitiesWithinAABB(LivingEntity.class,
                this.getBoundingBox().grow(30), this::isUndead)) {
            if (entity.isAlive() && !entity.equals(this) && this.canEntityBeSeen(entity)) {
                double distance = this.getDistanceSq(entity);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestUndead = entity;
                }
            }
        }
        if (nearestUndead != null) {
            this.setAttackTarget(nearestUndead);
            this.navigator.tryMoveToEntityLiving(nearestUndead, 1.2D);
        }
    }
    private void aggroNearbyUndead(ServerWorld world) {
        if (isTrading) {
            return;
        }
        if (this.ticksExisted % 20 != 0) {
            return;
        }
        for (LivingEntity entity : world.getEntitiesWithinAABB(LivingEntity.class,
                this.getBoundingBox().grow(16.0), this::isUndead)) {
            if (entity instanceof MonsterEntity) {
                MonsterEntity monster = (MonsterEntity) entity;
                if (monster.getAttackTarget() == null || !monster.getAttackTarget().equals(this)) {
                    monster.setAttackTarget(this);
                    if (Objects.requireNonNull(entity.getType().getRegistryName()).toString().contains("zombie")) {
                        attractZombieAllies(world, entity);
                    }
                }
            }
        }
    }
    private void attractZombieAllies(ServerWorld world, LivingEntity sourceZombie) {
        for (LivingEntity entity : world.getEntitiesWithinAABB(LivingEntity.class,
                sourceZombie.getBoundingBox().grow(8.0), this::isUndead)) {
            if (entity instanceof MonsterEntity && !entity.equals(sourceZombie)) {
                MonsterEntity monster = (MonsterEntity) entity;
                if (monster.getAttackTarget() == null) {
                    monster.setAttackTarget(this);
                }
            }
        }
    }
    private boolean isUndead(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity.getType().isContained(EntityTypeTags.SKELETONS)) {
            return true;
        }
        String entityId = Objects.requireNonNull(entity.getType().getRegistryName()).toString();
        return entityId.contains("zombie") ||
                entityId.contains("skeleton") ||
                entityId.contains("wither") ||
                entityId.contains("phantom") ||
                entityId.equals("minecraft:drowned") ||
                entityId.equals("minecraft:husk") ||
                entityId.equals("minecraft:stray") ||
                entityId.equals("minecraft:wither_skeleton");
    }
    private void checkAndDespawn(ServerWorld world) {
        if (this.spawnTime < 0) {
            return;
        }
        long currentTime = world.getGameTime();
        long timeAlive = currentTime - this.spawnTime;
        long threeDaysInTicks = 5184000L;
        if (timeAlive > threeDaysInTicks) {
            this.playSound(SoundEvents.ENTITY_WANDERING_TRADER_DISAPPEARED, 1.0F, 1.0F);
            this.remove();
        }
    }
    private void applyEnhancement() {
        if (this.enhancementTier < 2 || this.enhancementTier > 3) {
            this.enhancementTier = 2;
        }
        Difficulty difficulty = this.world.getDifficulty();
        MobEnhancementHandler.applyEnhancement(this, this.enhancementTier, difficulty);
        this.getPersistentData().putInt("ChaoticMerchantTier", this.enhancementTier);
    }
    private void initializeNBTData() {
        CompoundNBT nbt = this.getPersistentData();
        nbt.putFloat("naturalArmor", NATURAL_ARMOR);
        nbt.putFloat("dynamicArmor", NATURAL_ARMOR);
        nbt.putFloat("rangedDamageBonus", RANGED_DAMAGE_BONUS);
        nbt.putBoolean("IsChaoticMerchant", true);
        nbt.putFloat("BaseMovementSpeed", BASE_MOVEMENT_SPEED);
        nbt.putLong("SpawnTime", this.spawnTime);
        nbt.putInt("EnhancementTier", this.enhancementTier);
        nbt.putInt("ForcedMovementTimer", forcedMovementTimer);
        nbt.putDouble("LastPosX", lastPosX);
        nbt.putDouble("LastPosZ", lastPosZ);
        nbt.putInt("StuckCounter", stuckCounter);
    }
    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        if (attackCooldown > 0) {
            return false;
        }
        float attackDamage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (entityIn instanceof LivingEntity && isUndead((LivingEntity) entityIn)) {
            attackDamage *= 1.5f;
        }
        boolean flag = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), attackDamage);
        if (flag) {
            this.applyEnchantments(this, entityIn);
            attackCooldown = 15;
        }
        return flag;
    }
    @Override
    public float applyPotionDamageCalculations(DamageSource source, float damage) {
        damage = super.applyPotionDamageCalculations(source, damage);
        CompoundNBT nbt = this.getPersistentData();
        if (nbt.contains("rangedDamageBonus") && source.isProjectile()) {
            float bonus = nbt.getFloat("rangedDamageBonus");
            damage *= (1.0f + bonus);
        }
        return damage;
    }
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.world.isRemote || !this.isAlive()) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (isTrading && attacker instanceof LivingEntity) {
            this.setAttackTarget((LivingEntity) attacker);
            this.setCustomer(null);
            if (!this.navigator.hasPath()) {
                this.navigator.tryMoveToEntityLiving(attacker, 1.2D);
            }
        }
        return super.attackEntityFrom(source, amount);
    }
    @Override
    public void setCustomer(@Nullable PlayerEntity player) {
        if (Objects.equals(this.getCustomer(), player)) {
            return;
        }
        boolean wasTrading = isTrading;
        isTrading = (player != null);
        if (isTrading) {
            this.navigator.clearPath();
            this.setMotion(Vector3d.ZERO);
            forcedMovementTimer = 0;
            stuckCounter = 0;
        } else if (wasTrading) {
            forcedMovementTimer = this.rand.nextInt(80);
            stuckCounter = 0;
            if (!this.world.isRemote && this.world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) this.world;
                findAndSetUndeadTarget(serverWorld);
            }
        }
        super.setCustomer(player);
        if (player != null) {
            try {
                this.populateTradeData();
                if (!this.world.isRemote && ServerLifecycleHooks.getCurrentServer() != null) {
                    ServerLifecycleHooks.getCurrentServer().deferTask(() -> {
                        if (this.isAlive() && player.isAlive() && player.equals(this.getCustomer())) {
                            this.openMerchantContainer(player, this.getDisplayName(), 1);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public ActionResultType getEntityInteractionResult(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResultType.PASS;
        }
        if (!this.world.isRemote && !this.isChild() && this.isAlive()) {
            if (this.isTrading && player.equals(this.getCustomer())) {
                return ActionResultType.SUCCESS;
            }
            this.setCustomer(player);
            return ActionResultType.SUCCESS;
        }
        return super.getEntityInteractionResult(player, hand);
    }
    @Override
    protected void populateTradeData() {
        MerchantOffers offers = this.getOffers();
        if (offers.isEmpty()) {
            MerchantOffer originalOffer = new MerchantOffer(
                    new ItemStack(net.minecraft.item.Items.WITHER_ROSE, 1),
                    new ItemStack(RegistryHandler.ENHANCE_LAND_PORTAL_FRAME_ITEM.get(), 1),
                    999,
                    0,
                    1.0F
            );
            offers.add(originalOffer);
            MerchantOffer pillToStoneOffer = new MerchantOffer(
                    new ItemStack(RegistryHandler.ENHANCE_PILL.get(), 1),
                    new ItemStack(RegistryHandler.ENHANCE_STONE.get(), 1),
                    999,
                    0,
                    1.0F
            );
            offers.add(pillToStoneOffer);
            ItemStack randomPill = generateRandomPill();
            Random random = new Random();
            int stoneCount = random.nextInt(4) + 1;
            CompoundNBT pillNBT = randomPill.getOrCreateTag();
            pillNBT.putInt("TradeStoneCost", stoneCount);
            MerchantOffer stoneToPillOffer = new MerchantOffer(
                    new ItemStack(RegistryHandler.ENHANCE_STONE.get(), stoneCount),
                    randomPill,
                    999,
                    0,
                    1.0F
            );
            offers.add(stoneToPillOffer);
            ItemStack greenGiftPill = generateSpecifiedGiftPill("green_gift");
            CompoundNBT greenNBT = greenGiftPill.getOrCreateTag();
            greenNBT.putInt("TradeRecipe", 4);
            MerchantOffer greenGiftOffer = new MerchantOffer(
                    new ItemStack(RegistryHandler.GREEN_GIFT.get(), 1),
                    new ItemStack(RegistryHandler.ENHANCE_STONE.get(), 2),
                    greenGiftPill,
                    999,
                    0,
                    1.0F
            );
            offers.add(greenGiftOffer);
            ItemStack blueGiftPill = generateSpecifiedGiftPill("blue_gift");
            CompoundNBT blueNBT = blueGiftPill.getOrCreateTag();
            blueNBT.putInt("TradeRecipe", 5);
            MerchantOffer blueGiftOffer = new MerchantOffer(
                    new ItemStack(RegistryHandler.BLUE_GIFT.get(), 1),
                    new ItemStack(RegistryHandler.ENHANCE_STONE.get(), 2),
                    blueGiftPill,
                    999,
                    0,
                    1.0F
            );
            offers.add(blueGiftOffer);
            ItemStack redGiftPill = generateSpecifiedGiftPill("red_gift");
            CompoundNBT redNBT = redGiftPill.getOrCreateTag();
            redNBT.putInt("TradeRecipe", 6);
            MerchantOffer redGiftOffer = new MerchantOffer(
                    new ItemStack(RegistryHandler.RED_GIFT.get(), 1),
                    new ItemStack(RegistryHandler.ENHANCE_STONE.get(), 2),
                    redGiftPill,
                    999,
                    0,
                    1.0F
            );
            offers.add(redGiftOffer);
            this.offers = offers;
            MerchantOffer purpleGiftOffer = new MerchantOffer(
                    new ItemStack(RegistryHandler.RED_GIFT.get(), 1),
                    new ItemStack(RegistryHandler.BLUE_GIFT.get(), 1),
                    new ItemStack(RegistryHandler.PURPLE_GIFT.get(), 1),
                    999,
                    0,
                    1.0F
            );
            offers.add(purpleGiftOffer);
            this.offers = offers;

        }
    }
    private ItemStack generateSpecifiedGiftPill(String giftType) {
        ItemStack pillStack = new ItemStack(RegistryHandler.ENHANCE_PILL.get());
        CompoundNBT pillNBT = pillStack.getOrCreateTag();
        Random random = new Random();
        int buffCount = random.nextInt(4) + 1;
        Map<String, Integer> buffTypeWeights = new HashMap<>();
        buffTypeWeights.put("attack", 35);
        buffTypeWeights.put("life", 35);
        buffTypeWeights.put("defense", 15);
        buffTypeWeights.put("speed", 10);
        buffTypeWeights.put("harmony", 5);
        Map<String, List<String>> typeToBuffs = new HashMap<>();
        typeToBuffs.put("attack", Arrays.asList(
                "attack", "megaforce", "rob", "displacement", "thunder",
                "ricochet", "annihilation", "corrosion", "combo"
        ));
        typeToBuffs.put("life", Arrays.asList(
                "life", "fasting", "photosynthesis", "vampire", "curse",
                "unyielding", "chaos", "inspiration"
        ));
        typeToBuffs.put("defense", Arrays.asList(
                "thorns", "death_bomb", "spirit_shield"
        ));
        typeToBuffs.put("speed", Arrays.asList(
                "frost", "aura", "hunger", "phantom"
        ));
        typeToBuffs.put("harmony", Collections.singletonList(
                "harmony"
        ));
        com.weaponhouse.enhance.util.GiftConfigReader.GiftConfig giftConfig =
                com.weaponhouse.enhance.util.GiftConfigReader.readGiftConfig(giftType);
        if (giftConfig == null) {
            giftConfig = com.weaponhouse.enhance.util.GiftConfigReader.readGiftConfig("green_gift");
        }
        CompoundNBT pillBuffs = new CompoundNBT();
        Set<String> usedBuffs = new HashSet<>();
        for (int i = 0; i < buffCount; i++) {
            String buffType = selectBuffTypeByWeight(random, buffTypeWeights);
            String buffKey = selectRandomBuffFromType(buffType, random, typeToBuffs, usedBuffs);
            if (buffKey == null) {
                i--;
                continue;
            }
            int buffLevel = getRandomBuffLevelFromConfig(giftConfig, buffKey, random);
            pillBuffs.putInt(buffKey, buffLevel);
            usedBuffs.add(buffKey);
        }
        if (!usedBuffs.isEmpty()) {
            String firstBuff = usedBuffs.iterator().next();
            String textureType = "DEFAULT";
            if (typeToBuffs.get("attack").contains(firstBuff)) {
                textureType = "ATTACK";
            } else if (typeToBuffs.get("life").contains(firstBuff)) {
                textureType = "LIFE";
            } else if (typeToBuffs.get("defense").contains(firstBuff)) {
                textureType = "DEFENSE";
            } else if (typeToBuffs.get("speed").contains(firstBuff)) {
                textureType = "SPEED";
            } else if (typeToBuffs.get("harmony").contains(firstBuff)) {
                textureType = "HARMONY";
            }
            pillBuffs.putString("PillTextureType", textureType);
        }
        pillBuffs.putString("GiftType", giftType);
        pillNBT.put("EnhancePillBuffs", pillBuffs);
        return pillStack;
    }
    private ItemStack generateRandomPill() {
        ItemStack pillStack = new ItemStack(RegistryHandler.ENHANCE_PILL.get());
        CompoundNBT pillNBT = pillStack.getOrCreateTag();
        Random random = new Random();
        int buffCount = random.nextInt(4) + 1;
        Map<String, Integer> buffTypeWeights = new HashMap<>();
        buffTypeWeights.put("attack", 35);
        buffTypeWeights.put("life", 35);
        buffTypeWeights.put("defense", 15);
        buffTypeWeights.put("speed", 10);
        buffTypeWeights.put("harmony", 5);
        Map<String, List<String>> typeToBuffs = new HashMap<>();
        typeToBuffs.put("attack", Arrays.asList(
                "attack", "megaforce", "rob", "displacement", "thunder",
                "ricochet", "annihilation", "corrosion", "combo"
        ));
        typeToBuffs.put("life", Arrays.asList(
                "life", "fasting", "photosynthesis", "vampire", "curse",
                "unyielding", "chaos", "inspiration"
        ));
        typeToBuffs.put("defense", Arrays.asList(
                "thorns", "death_bomb", "spirit_shield"
        ));
        typeToBuffs.put("speed", Arrays.asList(
                "frost", "aura", "hunger", "phantom"
        ));
        typeToBuffs.put("harmony", Collections.singletonList(
                "harmony"
        ));
        int giftTypeRandom = random.nextInt(100);
        String giftType;
        if (giftTypeRandom < 75) {
            giftType = "green_gift";
        } else if (giftTypeRandom < 95) {
            giftType = "blue_gift";
        } else {
            giftType = "red_gift";
        }
        com.weaponhouse.enhance.util.GiftConfigReader.GiftConfig giftConfig =
                com.weaponhouse.enhance.util.GiftConfigReader.readGiftConfig(giftType);
        if (giftConfig == null) {
            giftConfig = com.weaponhouse.enhance.util.GiftConfigReader.readGiftConfig("green_gift");
        }
        CompoundNBT pillBuffs = new CompoundNBT();
        Set<String> usedBuffs = new HashSet<>();
        for (int i = 0; i < buffCount; i++) {
            String buffType = selectBuffTypeByWeight(random, buffTypeWeights);
            String buffKey = selectRandomBuffFromType(buffType, random, typeToBuffs, usedBuffs);
            if (buffKey == null) {
                i--;
                continue;
            }
            int buffLevel = getRandomBuffLevelFromConfig(giftConfig, buffKey, random);
            pillBuffs.putInt(buffKey, buffLevel);
            usedBuffs.add(buffKey);
        }
        if (!usedBuffs.isEmpty()) {
            String firstBuff = usedBuffs.iterator().next();
            String textureType = "DEFAULT";
            if (typeToBuffs.get("attack").contains(firstBuff)) {
                textureType = "ATTACK";
            } else if (typeToBuffs.get("life").contains(firstBuff)) {
                textureType = "LIFE";
            } else if (typeToBuffs.get("defense").contains(firstBuff)) {
                textureType = "DEFENSE";
            } else if (typeToBuffs.get("speed").contains(firstBuff)) {
                textureType = "SPEED";
            } else if (typeToBuffs.get("harmony").contains(firstBuff)) {
                textureType = "HARMONY";
            }
            pillBuffs.putString("PillTextureType", textureType);
        }
        pillBuffs.putString("GiftType", giftType);
        pillNBT.put("EnhancePillBuffs", pillBuffs);
        return pillStack;
    }
    private int getRandomBuffLevelFromConfig(com.weaponhouse.enhance.util.GiftConfigReader.GiftConfig config,
                                             String buffKey, Random random) {
        int[] levelRange = config.buffRanges.getOrDefault(buffKey, new int[]{1, 1});
        int minLevel = levelRange[0];
        int maxLevel = levelRange[1];

        if (minLevel == maxLevel) {
            return minLevel;
        } else {
            return minLevel + random.nextInt(maxLevel - minLevel + 1);
        }
    }
    private String selectBuffTypeByWeight(Random random, Map<String, Integer> weights) {
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }
        return "attack";
    }
    private String selectRandomBuffFromType(String buffType, Random random,
                                            Map<String, List<String>> typeToBuffs,
                                            Set<String> usedBuffs) {
        List<String> availableBuffs = typeToBuffs.get(buffType);
        if (availableBuffs == null || availableBuffs.isEmpty()) {
            return null;
        }
        List<String> unusedBuffs = new ArrayList<>();
        for (String buff : availableBuffs) {
            if (!usedBuffs.contains(buff)) {
                unusedBuffs.add(buff);
            }
        }
        if (unusedBuffs.isEmpty()) {
            return availableBuffs.get(random.nextInt(availableBuffs.size()));
        }
        return unusedBuffs.get(random.nextInt(unusedBuffs.size()));
    }
    @Override
    protected void onVillagerTrade(MerchantOffer offer) {
        this.world.setEntityState(this, (byte)14);
    }
    @Nullable
    @Override
    public AgeableEntity createChild(ServerWorld world, AgeableEntity mate) {
        return null;
    }
    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WANDERING_TRADER_AMBIENT;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_WANDERING_TRADER_HURT;
    }
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WANDERING_TRADER_DEATH;
    }
    @Override
    protected SoundEvent getVillagerYesNoSound(boolean getYesSound) {
        return getYesSound ? SoundEvents.ENTITY_WANDERING_TRADER_YES : SoundEvents.ENTITY_WANDERING_TRADER_NO;
    }
    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putBoolean("IsChaoticMerchant", true);
        compound.putFloat("rangedDamageBonus", RANGED_DAMAGE_BONUS);
        compound.putBoolean("HasCustomWeapon", hasCustomWeapon);
        compound.putBoolean("IsTrading", isTrading);
        compound.putLong("SpawnTime", this.spawnTime);
        compound.putInt("EnhancementTier", this.enhancementTier);
        compound.putInt("ForcedMovementTimer", forcedMovementTimer);
        compound.putDouble("LastPosX", lastPosX);
        compound.putDouble("LastPosZ", lastPosZ);
        compound.putInt("StuckCounter", stuckCounter);
        compound.putBoolean("IsSwimming", this.isSwimming);
        compound.putInt("SwimTimer", this.swimTimer);
        compound.putInt("BreathTimer", this.breathTimer);
        compound.putBoolean("HasWaterBreathing", this.hasWaterBreathing);
        compound.putInt("LandCheckTimer", this.landCheckTimer);
        compound.putBoolean("IsTryingToLand", this.isTryingToLand);
    }
    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        hasCustomWeapon = compound.getBoolean("HasCustomWeapon");
        isTrading = compound.getBoolean("IsTrading");
        if (compound.contains("SpawnTime")) {
            this.spawnTime = compound.getLong("SpawnTime");
        }
        if (compound.contains("EnhancementTier")) {
            this.enhancementTier = compound.getInt("EnhancementTier");
        }
        if (compound.contains("ForcedMovementTimer")) {
            forcedMovementTimer = compound.getInt("ForcedMovementTimer");
        }
        if (compound.contains("LastPosX")) {
            lastPosX = compound.getDouble("LastPosX");
        }
        if (compound.contains("LastPosZ")) {
            lastPosZ = compound.getDouble("LastPosZ");
        }
        if (compound.contains("StuckCounter")) {
            stuckCounter = compound.getInt("StuckCounter");
        }
        this.isSwimming = compound.getBoolean("IsSwimming");
        this.swimTimer = compound.getInt("SwimTimer");
        this.breathTimer = compound.getInt("BreathTimer");
        this.hasWaterBreathing = compound.getBoolean("HasWaterBreathing");
        if (this.breathTimer < 0) this.breathTimer = 0;
        if (this.breathTimer > MAX_BREATH) this.breathTimer = MAX_BREATH;
        this.landCheckTimer = compound.getInt("LandCheckTimer");
        this.isTryingToLand = compound.getBoolean("IsTryingToLand");
        this.initializeNBTData();
    }
    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }
    @Override
    public Inventory getVillagerInventory() {
        return super.getVillagerInventory();
    }
    private static class UndeadPredicate implements Predicate<LivingEntity> {
        @Override
        public boolean test(LivingEntity entity) {
            if (!entity.isAlive()) {
                return false;
            }
            if (entity instanceof PlayerEntity) {
                return false;
            }
            if (entity instanceof AbstractVillagerEntity) {
                return false;
            }
            if (entity instanceof net.minecraft.entity.passive.IronGolemEntity) {
                return false;
            }
            if (entity instanceof net.minecraft.entity.passive.GolemEntity) {
                return false;
            }
            String entityId = Objects.requireNonNull(entity.getType().getRegistryName()).toString();
            return entityId.contains("zombie") ||
                    entityId.contains("skeleton") ||
                    entityId.contains("wither") ||
                    entityId.contains("phantom") ||
                    entityId.equals("minecraft:drowned") ||
                    entityId.equals("minecraft:husk") ||
                    entityId.equals("minecraft:stray") ||
                    entityId.equals("minecraft:wither_skeleton");
        }
    }
    private static class MonsterPredicate implements Predicate<LivingEntity> {
        @Override
        public boolean test(LivingEntity entity) {
            if (!entity.isAlive()) {
                return false;
            }
            if (entity instanceof PlayerEntity) {
                return false;
            }
            if (entity instanceof AbstractVillagerEntity) {
                return false;
            }
            if (entity instanceof net.minecraft.entity.passive.IronGolemEntity) {
                return false;
            }
            if (entity instanceof net.minecraft.entity.passive.GolemEntity) {
                return false;
            }
            String entityId = Objects.requireNonNull(entity.getType().getRegistryName()).toString();
            boolean isUndead = entityId.contains("zombie") ||
                    entityId.contains("skeleton") ||
                    entityId.contains("wither") ||
                    entityId.contains("phantom") ||
                    entityId.equals("minecraft:drowned") ||
                    entityId.equals("minecraft:husk") ||
                    entityId.equals("minecraft:stray") ||
                    entityId.equals("minecraft:wither_skeleton");
            if (isUndead) {
                return false;
            }
            return entity instanceof MonsterEntity ||
                    entity instanceof net.minecraft.entity.monster.SlimeEntity ||
                    entity instanceof net.minecraft.entity.monster.PhantomEntity;
        }
    }
}