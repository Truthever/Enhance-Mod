package com.weaponhouse.enhance.client.gui;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.client.FloatingPillEntity;
import com.weaponhouse.enhance.common.AlchemyMaterialEffects;
import com.weaponhouse.enhance.common.PillBuffGenerator;
import com.weaponhouse.enhance.enhances.ChaosHandler;
import com.weaponhouse.enhance.network.AlchemyFurnaceSyncPacket;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
public class AlchemyFurnaceTileEntity extends TileEntity implements ISidedInventory, INamedContainerProvider, ITickableTileEntity {
    private final NonNullList<ItemStack> items = NonNullList.withSize(7, ItemStack.EMPTY);
    private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    private AlchemyMaterialEffects.MaterialEffect totalEffect = new AlchemyMaterialEffects.MaterialEffect(0, 0, 0, 0, 0, 0, 0);
    private final ItemStackHandler itemHandler = createHandler();
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    private int craftingProgress = 0;
    private int craftingTotalTime = 0;
    private boolean isCrafting = false;
    private ItemStack currentGift = ItemStack.EMPTY;
    private final Random random = new Random();
    private static final int GREEN_GIFT_TIME = 30 * 20;
    private static final int BLUE_GIFT_TIME = 60 * 20;
    private static final int RED_GIFT_TIME = 180 * 20;
    private static final float GREEN_BASE_CHANCE = 0.6f;
    private static final float BLUE_BASE_CHANCE = 0.4f;
    private static final float RED_BASE_CHANCE = 0.2f;
    private static final float ENHANCE_DIRT_BONUS = 0.1f;
    private static final int MAX_ENHANCE_DIRT = 3;
    private static final int SPECIAL_CRAFTING_TIME = 60 * 20;
    private static final float SPECIAL_BASE_CHANCE = 0.5f;
    private boolean isSpecialCrafting = false;
    private ItemStackHandler createHandler() {
        return new ItemStackHandler(7) {
            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
            }
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }
        };
    }
    private void calculateTotalEffect() {
        totalEffect = new AlchemyMaterialEffects.MaterialEffect(0, 0, 0, 0, 0, 0, 0);
        int[] effectSlots = {1, 3, 4, 5};
        for (int slot : effectSlots) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty() && AlchemyMaterialEffects.hasEffect(stack)) {
                AlchemyMaterialEffects.MaterialEffect effect = AlchemyMaterialEffects.getEffect(stack);
                if (effect != null) {
                    totalEffect = new AlchemyMaterialEffects.MaterialEffect(
                            totalEffect.attackWeight + effect.attackWeight,
                            totalEffect.lifeWeight + effect.lifeWeight,
                            totalEffect.defenseWeight + effect.defenseWeight,
                            totalEffect.speedWeight + effect.speedWeight,
                            totalEffect.harmonyWeight + effect.harmonyWeight,
                            totalEffect.successChance + effect.successChance,
                            totalEffect.craftTimeModifier + effect.craftTimeModifier
                    );
                }
                String giftType = PillBuffGenerator.getGiftTypeFromItem(currentGift);
                float specialBonus = AlchemyMaterialEffects.getSpecialSuccessBonus(stack, giftType);
                if (specialBonus > 0) {
                    totalEffect = new AlchemyMaterialEffects.MaterialEffect(
                            totalEffect.attackWeight,
                            totalEffect.lifeWeight,
                            totalEffect.defenseWeight,
                            totalEffect.speedWeight,
                            totalEffect.harmonyWeight,
                            totalEffect.successChance + specialBonus,
                            totalEffect.craftTimeModifier
                    );
                }
            }
        }
    }
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        switch (slot) {
            case 0:
                return stack.getItem() == RegistryHandler.ENHANCE_DUST.get();
            case 3:
                return stack.getItem() == RegistryHandler.GREEN_GIFT.get() ||
                        stack.getItem() == RegistryHandler.BLUE_GIFT.get() ||
                        stack.getItem() == RegistryHandler.RED_GIFT.get() ||
                        stack.getItem() == RegistryHandler.PURPLE_GIFT.get();
            case 6:
                return stack.getItem() == Items.GLASS_BOTTLE;
            default:
                return true;
        }
    }
    public AlchemyFurnaceTileEntity() {
        super(RegistryHandler.ALCHEMY_FURNACE_TILE_ENTITY.get());
    }
    @Override
    public void tick() {
        if (this.world != null) {
            if (!this.world.isRemote) {
                serverTick();
            } else {
                clientTick();
            }
        }
    }
    private void serverTick() {
        boolean hasFireBelow = isFireBelow();
        if (hasFireBelow && !isCrafting) {
            tryStartCrafting();
        } else if (isCrafting) {
            if (!hasFireBelow) {
                interruptCrafting();
                return;
            }
            craftingProgress++;
            if (craftingProgress % 100 == 0) {
                syncToClient();
            }
            if (craftingProgress >= craftingTotalTime) {
                completeCrafting();
            } else {
                markDirty();
            }
        }
    }
    private void clientTick() {
        if (isCrafting && world != null) {
            if (isSpecialCrafting) {
                spawnSpecialCraftingParticles();
            } else {
                spawnCraftingParticles();
            }
        }
    }
    private void spawnSpecialCraftingParticles() {
        if (world == null || !world.isRemote) return;
        BlockPos pos = getPos();
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        for (int i = 0; i < 4; i++) {
            double angle = (System.currentTimeMillis() % 36000 / 100.0 + i * 90) * Math.PI / 180;
            double radius = 0.6;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double particleY = centerY + 0.2 + (random.nextDouble() * 0.3);
            if (i % 2 == 0) {
                world.addParticle(ParticleTypes.ENTITY_EFFECT,
                        centerX + offsetX, particleY, centerZ + offsetZ,
                        0, 0.01, 0
                );
            } else {
                world.addParticle(ParticleTypes.FLAME,
                        centerX + offsetX, particleY, centerZ + offsetZ,
                        0, 0.01, 0);
            }
        }
        if (world.getGameTime() % 10 == 0) {
            for (int side = 0; side < 4; side++) {
                double sideOffsetX = 0;
                double sideOffsetZ = 0;
                switch (side) {
                    case 0:
                        sideOffsetZ = -0.4;
                        break;
                    case 1:
                        sideOffsetZ = 0.4;
                        break;
                    case 2:
                        sideOffsetX = -0.4;
                        break;
                    case 3:
                        sideOffsetX = 0.4;
                        break;
                }
                world.addParticle(ParticleTypes.SMOKE,
                        centerX + sideOffsetX, centerY + 0.3, centerZ + sideOffsetZ,
                        0, 0.02, 0);
            }
        }
    }
    private void spawnCraftingParticles() {
        if (world == null || !world.isRemote) return;
        BlockPos pos = getPos();
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        for (int i = 0; i < 4; i++) {
            double angle = (System.currentTimeMillis() % 36000 / 100.0 + i * 90) * Math.PI / 180;
            double radius = 0.6;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double particleY = centerY + 0.2 + (random.nextDouble() * 0.3);
            world.addParticle(ParticleTypes.FLAME,
                    centerX + offsetX, particleY, centerZ + offsetZ,
                    0, 0.01, 0);
            if (!currentGift.isEmpty()) {
                if (currentGift.getItem() == RegistryHandler.GREEN_GIFT.get()) {
                    world.addParticle(ParticleTypes.ITEM_SLIME,
                            centerX + offsetX * 0.5, particleY + 0.1, centerZ + offsetZ * 0.5,
                            0, 0.005, 0);
                } else if (currentGift.getItem() == RegistryHandler.BLUE_GIFT.get()) {
                    world.addParticle(ParticleTypes.ITEM_SNOWBALL,
                            centerX + offsetX * 0.5, particleY + 0.1, centerZ + offsetZ * 0.5,
                            0, 0.005, 0);
                } else if (currentGift.getItem() == RegistryHandler.RED_GIFT.get()) {
                    world.addParticle(ParticleTypes.FLAME,
                            centerX + offsetX * 0.5, particleY + 0.1, centerZ + offsetZ * 0.5,
                            0, 0.005, 0);
                }
            }
        }
        if (world.getGameTime() % 8 == 0) {
            for (int side = 0; side < 4; side++) {
                double sideOffsetX = 0;
                double sideOffsetZ = 0;
                switch (side) {
                    case 0:
                        sideOffsetZ = -0.4;
                        break;
                    case 1:
                        sideOffsetZ = 0.4;
                        break;
                    case 2:
                        sideOffsetX = -0.4;
                        break;
                    case 3:
                        sideOffsetX = 0.4;
                        break;
                }
                world.addParticle(ParticleTypes.FLAME,
                        centerX + sideOffsetX, centerY + 0.3, centerZ + sideOffsetZ,
                        0, 0.02, 0);
            }
        }
        if (world.getGameTime() % 5 == 0) {
            for (int i = 0; i < 2; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.8;
                double offsetZ = (random.nextDouble() - 0.5) * 0.8;
                world.addParticle(ParticleTypes.FLAME,
                        centerX + offsetX, centerY - 0.2, centerZ + offsetZ,
                        0, 0.03, 0);
            }
        }
        if (world.getGameTime() % 15 == 0) {
            for (int i = 0; i < 2; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.6;
                double offsetZ = (random.nextDouble() - 0.5) * 0.6;
                world.addParticle(ParticleTypes.SMOKE,
                        centerX + offsetX, centerY + 0.1, centerZ + offsetZ,
                        0, 0.02, 0);
            }
        }
    }
    private boolean isFireBelow() {
        if (this.world == null) return false;
        BlockPos belowPos = this.pos.down();
        BlockState belowState = this.world.getBlockState(belowPos);
        return belowState.getBlock() == Blocks.FIRE ||
                belowState.getBlock() == Blocks.SOUL_FIRE ||
                belowState.getBlock() == RegistryHandler.ETERNAL_SACRED_FIRE.get();
    }
    private void tryStartCrafting() {
        calculateTotalEffect();
        ItemStack enhanceDirt = itemHandler.getStackInSlot(0);
        ItemStack gift = itemHandler.getStackInSlot(3);
        ItemStack glassBottle = itemHandler.getStackInSlot(6);
        if (isValidRecipe(enhanceDirt, gift, glassBottle)) {
            this.isSpecialCrafting = isSpecialRecipe(enhanceDirt, gift, glassBottle);
            this.isCrafting = true;
            this.currentGift = gift.copy();
            this.craftingProgress = 0;
            if (isSpecialCrafting) {
                this.craftingTotalTime = getSpecialCraftingTime();
            } else {
                this.craftingTotalTime = getCraftingTime(gift);
            }
            syncToClient();
            markDirty();
        }
    }
    private int getSpecialCraftingTime() {
        int baseTime = SPECIAL_CRAFTING_TIME;
        if (world != null) {
            BlockPos belowPos = this.pos.down();
            BlockState belowState = this.world.getBlockState(belowPos);
            if (belowState.getBlock() == RegistryHandler.ETERNAL_SACRED_FIRE.get()) {
                baseTime = (int)(baseTime * 0.5f);
            }
        }
        return baseTime;
    }
    private boolean isValidRecipe(ItemStack enhanceDirt, ItemStack gift, ItemStack glassBottle) {
        if (isSpecialRecipe(enhanceDirt, gift, glassBottle)) {
            return true;
        }
        return !enhanceDirt.isEmpty() &&
                !gift.isEmpty() &&
                !glassBottle.isEmpty() &&
                enhanceDirt.getCount() >= 1 &&
                gift.getCount() >= 1 &&
                glassBottle.getCount() >= 1 &&
                enhanceDirt.getItem() == RegistryHandler.ENHANCE_DUST.get() &&
                (gift.getItem() == RegistryHandler.GREEN_GIFT.get() ||
                        gift.getItem() == RegistryHandler.BLUE_GIFT.get() ||
                        gift.getItem() == RegistryHandler.RED_GIFT.get()) &&
                glassBottle.getItem() == Items.GLASS_BOTTLE;
    }
    private boolean isSpecialRecipe(ItemStack enhanceDirt, ItemStack gift, ItemStack glassBottle) {
        if (enhanceDirt.isEmpty() || gift.isEmpty() || glassBottle.isEmpty()) {
            return false;
        }
        if (enhanceDirt.getCount() < 10 || enhanceDirt.getItem() != RegistryHandler.ENHANCE_DUST.get()) {
            return false;
        }
        if (gift.getItem() != RegistryHandler.PURPLE_GIFT.get()) {
            return false;
        }
        if (glassBottle.getItem() != Items.GLASS_BOTTLE) {
            return false;
        }
        return checkMaterialSlots();
    }
    private boolean checkMaterialSlots() {
        int goldCount = 0;
        int diamondCount = 0;
        int[] materialSlots = {1, 2, 4, 5};
        for (int slot : materialSlots) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                return false;
            }
            if (stack.getItem() == Items.GOLD_INGOT) {
                goldCount++;
            } else if (stack.getItem() == Items.DIAMOND) {
                diamondCount++;
            } else {
                return false;
            }
        }
        return goldCount == 2 && diamondCount == 2;
    }
    private int getCraftingTime(ItemStack gift) {
        int baseTime;
        if (gift.getItem() == RegistryHandler.GREEN_GIFT.get()) {
            baseTime = GREEN_GIFT_TIME;
        } else if (gift.getItem() == RegistryHandler.BLUE_GIFT.get()) {
            baseTime = BLUE_GIFT_TIME;
        } else {
            baseTime = RED_GIFT_TIME;
        }
        float timeModifier = 1.0f + totalEffect.craftTimeModifier;
        int modifiedTime = (int)(baseTime * timeModifier);
        if (world != null) {
            BlockPos belowPos = this.pos.down();
            BlockState belowState = this.world.getBlockState(belowPos);
            if (belowState.getBlock() == RegistryHandler.ETERNAL_SACRED_FIRE.get()) {
                modifiedTime = (int)(modifiedTime * 0.5f);
            }
        }
        return Math.max(modifiedTime, 20);
    }
    private float calculateSuccessChance(ItemStack enhanceDirt, ItemStack gift) {
        float baseChance;
        if (isSpecialCrafting) {
            return calculateSpecialSuccessChance(enhanceDirt);
        }
        if (gift.getItem() == RegistryHandler.GREEN_GIFT.get()) {
            baseChance = GREEN_BASE_CHANCE;
        } else if (gift.getItem() == RegistryHandler.BLUE_GIFT.get()) {
            baseChance = BLUE_BASE_CHANCE;
        } else {
            baseChance = RED_BASE_CHANCE;
        }
        int enhanceDirtCount = Math.min(enhanceDirt.getCount(), MAX_ENHANCE_DIRT);
        float bonus = enhanceDirtCount * ENHANCE_DIRT_BONUS;
        float materialBonus = totalEffect.successChance;
        float sacredFireBonus = 0.0f;
        if (world != null) {
            BlockPos belowPos = this.pos.down();
            BlockState belowState = this.world.getBlockState(belowPos);
            if (belowState.getBlock() == RegistryHandler.ETERNAL_SACRED_FIRE.get()) {
                sacredFireBonus = 0.2f;
            }
        }
        return MathHelper.clamp(baseChance + bonus + materialBonus + sacredFireBonus, 0.0f, 1.0f);
    }

    private float calculateSpecialSuccessChance(ItemStack enhanceDirt) {
        float baseChance = SPECIAL_BASE_CHANCE;
        float sacredFireBonus = 0.0f;
        if (world != null) {
            BlockPos belowPos = this.pos.down();
            BlockState belowState = this.world.getBlockState(belowPos);
            if (belowState.getBlock() == RegistryHandler.ETERNAL_SACRED_FIRE.get()) {
                sacredFireBonus = 1.0f - baseChance;
            }
        }
        return MathHelper.clamp(baseChance + sacredFireBonus, 0.0f, 1.0f);
    }
    private void completeCrafting() {
        if (this.world == null) return;
        boolean hasPhantomMembrane = false;
        if (!isSpecialCrafting) {
            int[] effectSlots = {1, 2, 4, 5};
            for (int slot : effectSlots) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.getItem() == Items.PHANTOM_MEMBRANE) {
                    hasPhantomMembrane = true;
                    break;
                }
            }
        }
        ItemStack enhanceDirt = itemHandler.getStackInSlot(0);
        ItemStack gift = itemHandler.getStackInSlot(3);
        ItemStack glassBottle = itemHandler.getStackInSlot(6);
        if (isSpecialCrafting) {
            float successChance = calculateSpecialSuccessChance(enhanceDirt);
            boolean success = random.nextFloat() <= successChance;
            handleSpecialCraftingResult(success, enhanceDirt, gift, glassBottle);
            resetCrafting();
            syncToClient();
            markDirty();
            return;
        }
        float successChance = calculateSuccessChance(enhanceDirt, gift);
        boolean success = random.nextFloat() <= successChance;
        if (!(hasPhantomMembrane && !success)) {
            int enhanceDirtToConsume = Math.min(enhanceDirt.getCount(), 3);
            itemHandler.getStackInSlot(0).shrink(enhanceDirtToConsume);
            itemHandler.getStackInSlot(3).shrink(1);
            int[] materialSlots = {1, 2, 4, 5};
            for (int slot : materialSlots) {
                ItemStack materialStack = itemHandler.getStackInSlot(slot);
                if (!materialStack.isEmpty()) {
                    materialStack.shrink(1);
                }
            }
        }
        handleGlassBottleTransformation(success, gift);
        if (success) {
            successCrafting();
        } else {
            failCrafting();
        }
        resetCrafting();
        syncToClient();
        markDirty();
    }
    private void handleSpecialCraftingResult(boolean success, ItemStack enhanceDirt, ItemStack gift, ItemStack glassBottle) {
        itemHandler.getStackInSlot(0).shrink(10);
        itemHandler.getStackInSlot(3).shrink(1);
        int[] materialSlots = {1, 2, 4, 5};
        for (int slot : materialSlots) {
            itemHandler.getStackInSlot(slot).shrink(1);
        }
        handleSpecialGlassBottleTransformation(success, gift);
        if (success) {
            successSpecialCrafting();
        } else {
            failSpecialCrafting();
        }
    }private void handleSpecialGlassBottleTransformation(boolean success, ItemStack gift) {
        if (world == null) return;
        ItemStack glassBottle = itemHandler.getStackInSlot(6);
        if (glassBottle.isEmpty() || glassBottle.getItem() != Items.GLASS_BOTTLE) {
            return;
        }
        itemHandler.setStackInSlot(6, ItemStack.EMPTY);
    }
    private void successSpecialCrafting() {
        if (world == null) return;
        ItemStack purpleGourd = new ItemStack(RegistryHandler.PURPLE_GOURD.get());
        CompoundNBT nbt = purpleGourd.getOrCreateTag();
        nbt.putInt("SacredFireCount", 1);
        nbt.putInt("Mode", 0);
        FloatingPillEntity floatingPill = new FloatingPillEntity(world, this.pos, purpleGourd);
        world.addEntity(floatingPill);
        world.playSound(null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                1.0f, 0.8f);
        if (world.isRemote) {
            spawnSpecialSuccessParticles();
        }
    }
    private void failSpecialCrafting() {
        if (world == null) return;
        world.playSound(null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,
                4.0F, (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F);
        float explosionPower = 8.0f;
        int explosionRange = 40;
        executeCompleteDestruction(explosionPower, explosionRange);
        if (world.isRemote) {
            spawnSpecialFailureParticles();
        }
    }private void spawnSpecialSuccessParticles() {
        BlockPos pos = getPos();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 30; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            if (world != null) {
                world.addParticle(ParticleTypes.ENTITY_EFFECT,
                        x, y, z,
                        offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1
                );
            }
            if (i % 3 == 0) {
                if (world != null) {
                    world.addParticle(ParticleTypes.FLAME,
                            x, y, z,
                            offsetX * 0.15, offsetY * 0.15, offsetZ * 0.15);
                }
            }
        }
    }private void spawnSpecialFailureParticles() {
        BlockPos pos = getPos();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 20; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            if (world != null) {
                world.addParticle(ParticleTypes.SMOKE,
                        x, y, z,
                        offsetX * 0.3, offsetY * 0.3, offsetZ * 0.3);
            }
            if (i % 2 == 0) {
                if (world != null) {
                    world.addParticle(ParticleTypes.FLAME,
                            x, y, z,
                            offsetX * 0.25, offsetY * 0.25, offsetZ * 0.25);
                }
            }
        }
    }
    private void handleGlassBottleTransformation(boolean success, ItemStack gift) {
        if (world == null) return;
        ItemStack glassBottle = itemHandler.getStackInSlot(6);
        if (glassBottle.isEmpty() || glassBottle.getItem() != Items.GLASS_BOTTLE) {
            return;
        }
        ItemStack potionStack = new ItemStack(Items.POTION);
        CompoundNBT potionNBT = new CompoundNBT();
        if (success) {
            createSuccessPotion(potionNBT, gift);
        } else {
            createFailurePotion(potionNBT, gift);
        }
        potionStack.setTag(potionNBT);
        itemHandler.setStackInSlot(6, potionStack);
    }
    private void createSuccessPotion(CompoundNBT potionNBT, ItemStack gift) {
        ListNBT effectsList = new ListNBT();
        int effectCount;
        int duration;
        int amplifier;
        if (gift.getItem() == RegistryHandler.GREEN_GIFT.get()) {
            effectCount = 1;
            duration = 90 * 20;
            amplifier = 3;
        } else if (gift.getItem() == RegistryHandler.BLUE_GIFT.get()) {
            effectCount = 3;
            duration = 180 * 20;
            amplifier = 5;
        } else if (gift.getItem() == RegistryHandler.RED_GIFT.get()) {
            effectCount = 5;
            duration = 600 * 20;
            amplifier = 7 + random.nextInt(6);
        } else {
            effectCount = 1;
            duration = 60 * 20;
            amplifier = 1;
        }
        List<net.minecraft.potion.Effect> availableEffects = getChaosEffects();
        Set<Effect> usedEffects = new HashSet<>();
        int actualEffectCount = Math.min(effectCount, availableEffects.size());
        for (int i = 0; i < actualEffectCount; i++) {
            net.minecraft.potion.Effect effect = availableEffects.get(random.nextInt(availableEffects.size()));
            if (usedEffects.add(effect)) {
                CompoundNBT effectTag = new CompoundNBT();
                effectTag.putByte("Id", (byte) net.minecraft.potion.Effect.getId(effect));
                effectTag.putInt("Duration", duration);
                effectTag.putByte("Amplifier", (byte) (amplifier - 1));
                effectTag.putBoolean("Ambient", false);
                effectTag.putBoolean("ShowParticles", true);
                effectTag.putBoolean("ShowIcon", true);
                effectsList.add(effectTag);
            } else {
                i--;
                if (usedEffects.size() >= availableEffects.size()) {
                    break;
                }
            }
        }
        potionNBT.put("CustomPotionEffects", effectsList);
        if (gift.getItem() == RegistryHandler.GREEN_GIFT.get()) {
            potionNBT.putString("Potion", "enhance:green_alchemy");
        } else if (gift.getItem() == RegistryHandler.BLUE_GIFT.get()) {
            potionNBT.putString("Potion", "enhance:blue_alchemy");
        } else {
            potionNBT.putString("Potion", "enhance:red_alchemy");
        }
    }
    private void createFailurePotion(CompoundNBT potionNBT, ItemStack gift) {
        ListNBT effectsList = new ListNBT();
        int duration;
        if (gift.getItem() == RegistryHandler.GREEN_GIFT.get()) {
            duration = 100 * 20;
        } else if (gift.getItem() == RegistryHandler.BLUE_GIFT.get()) {
            duration = 200 * 20;
        } else {
            duration = 300 * 20;
        }
        net.minecraft.potion.Effect darknessEffect = getDarknessEffect();
        if (darknessEffect != null) {
            CompoundNBT effectTag = new CompoundNBT();
            effectTag.putByte("Id", (byte) net.minecraft.potion.Effect.getId(darknessEffect));
            effectTag.putInt("Duration", duration);
            effectTag.putByte("Amplifier", (byte) 19);
            effectTag.putBoolean("Ambient", false);
            effectTag.putBoolean("ShowParticles", true);
            effectTag.putBoolean("ShowIcon", true);
            effectsList.add(effectTag);
        }
        potionNBT.put("CustomPotionEffects", effectsList);
        potionNBT.putString("Potion", "enhance:darkness_potion");
    }
    private List<net.minecraft.potion.Effect> getChaosEffects() {
        try {
            return ChaosHandler.getAvailableEffects();
        } catch (Exception e) {
            return Arrays.asList(
                    net.minecraft.potion.Effects.SPEED,
                    net.minecraft.potion.Effects.STRENGTH,
                    net.minecraft.potion.Effects.JUMP_BOOST,
                    net.minecraft.potion.Effects.REGENERATION,
                    net.minecraft.potion.Effects.FIRE_RESISTANCE,
                    net.minecraft.potion.Effects.WATER_BREATHING,
                    net.minecraft.potion.Effects.INVISIBILITY,
                    net.minecraft.potion.Effects.NIGHT_VISION,
                    net.minecraft.potion.Effects.HEALTH_BOOST,
                    net.minecraft.potion.Effects.ABSORPTION,
                    net.minecraft.potion.Effects.SATURATION,
                    net.minecraft.potion.Effects.LUCK,
                    net.minecraft.potion.Effects.SLOW_FALLING,
                    net.minecraft.potion.Effects.CONDUIT_POWER,
                    net.minecraft.potion.Effects.DOLPHINS_GRACE,
                    net.minecraft.potion.Effects.HERO_OF_THE_VILLAGE
            );
        }
    }
    private net.minecraft.potion.Effect getDarknessEffect() {
        for (net.minecraft.potion.Effect effect : net.minecraftforge.registries.ForgeRegistries.POTIONS) {
            if (effect.getRegistryName() != null &&
                    effect.getRegistryName().getNamespace().equals("enhance") &&
                    effect.getRegistryName().getPath().equals("darkness")) {
                return effect;
            }
        }
        return net.minecraft.potion.Effects.WEAKNESS;
    }
    private void successCrafting() {
        if (world == null) return;
        ItemStack pill = new ItemStack(RegistryHandler.ENHANCE_PILL.get());
        String giftType = PillBuffGenerator.getGiftTypeFromItem(currentGift);
        PillBuffGenerator.applyBuffsToPill(pill, giftType, totalEffect);
        FloatingPillEntity floatingPill = new FloatingPillEntity(world, this.pos, pill);
        world.addEntity(floatingPill);
        world.playSound(null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                1.0f, 1.0f);
        if (world.isRemote) {
            spawnSuccessParticles();
        }
    }
    private void spawnSuccessParticles() {
        BlockPos pos = getPos();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 20; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            if (world != null) {
                world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                        x, y, z,
                        offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1);
            }
        }
    }
    private void failCrafting() {
        if (world == null) return;
        world.playSound(null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,
                4.0F, (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F);
        float explosionPower;
        int explosionRange;
        if (currentGift.getItem() == RegistryHandler.GREEN_GIFT.get()) {
            explosionPower = 2.0f;
            explosionRange = 15;
        } else if (currentGift.getItem() == RegistryHandler.BLUE_GIFT.get()) {
            explosionPower = 4.0f;
            explosionRange = 25;
        } else {
            explosionPower = 8.0f;
            explosionRange = 40;
        }
        executeCompleteDestruction(explosionPower, explosionRange);
        if (world.isRemote) {
            spawnFailureParticles();
        }
    }
    private void executeCompleteDestruction(float power, float radius) {
        if (world == null) return;
        Vector3d centerPos = new Vector3d(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
        if (!world.isRemote) {
            int centerX = (int) centerPos.x;
            int centerY = (int) centerPos.y;
            int centerZ = (int) centerPos.z;
            int radiusInt = (int) radius;
            int minY = Math.max(centerY - radiusInt, 0);
            int maxY = Math.min(centerY + radiusInt, world.getHeight() - 1);
            for (int x = centerX - radiusInt; x <= centerX + radiusInt; x++) {
                for (int z = centerZ - radiusInt; z <= centerZ + radiusInt; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        double distance = Math.sqrt(
                                Math.pow(x - centerPos.x, 2) +
                                        Math.pow(y - centerPos.y, 2) +
                                        Math.pow(z - centerPos.z, 2)
                        );
                        if (distance <= radius) {
                            BlockState blockState = world.getBlockState(blockPos);
                            if (!blockState.isAir() &&
                                    !isUnbreakableBlock(blockState.getBlock()) &&
                                    blockState.getBlockHardness(world, blockPos) >= 0) {
                                world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
                            }
                        }
                    }
                }
            }
        }
        AxisAlignedBB area = new AxisAlignedBB(
                centerPos.x - radius, centerPos.y - radius, centerPos.z - radius,
                centerPos.x + radius, centerPos.y + radius, centerPos.z + radius
        );
        List<net.minecraft.entity.Entity> allEntities = world.getEntitiesWithinAABB(Entity.class, area);
        for (net.minecraft.entity.Entity target : allEntities) {
            if (target instanceof net.minecraft.entity.LivingEntity) {
                net.minecraft.entity.LivingEntity livingTarget = (net.minecraft.entity.LivingEntity) target;
                double distance = target.getDistanceSq(centerPos.x, centerPos.y, centerPos.z);
                double maxDistanceSq = radius * radius;
                if (distance <= maxDistanceSq) {
                    float distanceFactor = 1.0f - (float)Math.sqrt(distance) / radius;
                    float actualDamage = power * 2.0f * distanceFactor;
                    livingTarget.attackEntityFrom(net.minecraft.util.DamageSource.causeExplosionDamage((Explosion) null), actualDamage);
                    Vector3d knockback = target.getPositionVec().subtract(centerPos).normalize()
                            .mul(distanceFactor * 3.0, distanceFactor * 3.0, distanceFactor * 3.0);
                    target.setMotion(target.getMotion().add(knockback));
                }
            }
        }
        if (world.isRemote) {
            for (int i = 0; i < 30; ++i) {
                double d0 = random.nextGaussian() * 0.02D;
                double d1 = random.nextGaussian() * 0.02D;
                double d2 = random.nextGaussian() * 0.02D;
                world.addParticle(ParticleTypes.EXPLOSION,
                        centerPos.x + (random.nextFloat() * radius * 2.0F) - radius,
                        centerPos.y + (random.nextFloat() * radius * 2.0F) - radius,
                        centerPos.z + (random.nextFloat() * radius * 2.0F) - radius,
                        d0, d1, d2);
            }
        }
    }
    private boolean isUnbreakableBlock(Block block) {
        return block == Blocks.BEDROCK ||
                block == Blocks.END_PORTAL_FRAME ||
                block == Blocks.END_PORTAL ||
                block == Blocks.NETHER_PORTAL ||
                block == RegistryHandler.ALCHEMY_FURNACE.get();
    }
    private void spawnFailureParticles() {
        BlockPos pos = getPos();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 15; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            if (world != null) {
                world.addParticle(ParticleTypes.SMOKE,
                        x, y, z,
                        offsetX * 0.2, offsetY * 0.2, offsetZ * 0.2);
            }
        }
    }
    private void interruptCrafting() {
        resetCrafting();
        syncToClient();
        markDirty();
    }
    private void resetCrafting() {
        this.isCrafting = false;
        this.isSpecialCrafting = false;
        this.craftingProgress = 0;
        this.craftingTotalTime = 0;
        this.currentGift = ItemStack.EMPTY;
    }
    private void syncToClient() {
        if (world != null && !world.isRemote) {
            AlchemyFurnaceSyncPacket packet = new AlchemyFurnaceSyncPacket(
                    pos, isCrafting, craftingProgress, craftingTotalTime
            );
            world.getPlayers().forEach(player -> {
                if (player instanceof ServerPlayerEntity) {
                    Enhance.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), packet);
                }
            });
        }
    }
    public void updateClientState(boolean isCrafting, int craftingProgress, int craftingTotalTime) {
        this.isCrafting = isCrafting;
        this.craftingProgress = craftingProgress;
        this.craftingTotalTime = craftingTotalTime;
    }
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent("Alchemy Furnace");
    }
    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AlchemyFurnaceContainer(windowId, playerInventory, this.pos);
    }
    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        this.items.clear();
        ItemStackHelper.loadAllItems(nbt, this.items);
        if (nbt.contains("Inventory")) {
            itemHandler.deserializeNBT(nbt.getCompound("Inventory"));
        }
        this.isCrafting = nbt.getBoolean("IsCrafting");
        this.isSpecialCrafting = nbt.getBoolean("IsSpecialCrafting");
        this.craftingProgress = nbt.getInt("CraftingProgress");
        this.craftingTotalTime = nbt.getInt("CraftingTotalTime");
        if (nbt.contains("CurrentGift")) {
            this.currentGift = ItemStack.read(nbt.getCompound("CurrentGift"));
        }
    }
    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        ItemStackHelper.saveAllItems(compound, this.items);
        compound.put("Inventory", itemHandler.serializeNBT());
        compound.putBoolean("IsCrafting", this.isCrafting);
        compound.putBoolean("IsSpecialCrafting", this.isSpecialCrafting);
        compound.putInt("CraftingProgress", this.craftingProgress);
        compound.putInt("CraftingTotalTime", this.craftingTotalTime);
        if (!this.currentGift.isEmpty()) {
            compound.put("CurrentGift", this.currentGift.serializeNBT());
        }
        return compound;
    }
    @Override
    public int getSizeInventory() {
        return this.items.size();
    }
    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    @Override
    public ItemStack getStackInSlot(int index) {
        return itemHandler.getStackInSlot(index);
    }
    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = getStackInSlot(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result;
        if (stack.getCount() <= count) {
            result = stack.copy();
            setInventorySlotContents(index, ItemStack.EMPTY);
        } else {
            result = stack.split(count);
            if (stack.getCount() == 0) {
                setInventorySlotContents(index, ItemStack.EMPTY);
            }
        }
        markDirty();
        return result;
    }
    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = getStackInSlot(index);
        setInventorySlotContents(index, ItemStack.EMPTY);
        return stack;
    }
    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        itemHandler.setStackInSlot(index, stack);
        if (stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }
        markDirty();
    }
    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        if (this.isCrafting) {
            return false;
        }

        if (this.world == null || this.world.getTileEntity(this.pos) != this) {
            return false;
        } else {
            return player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }
    @Override
    public void clear() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        markDirty();
    }
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6};
    }
    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        return isItemValidForSlot(index, itemStackIn);
    }
    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        return true;
    }
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (side == null) {
                return handler.cast();
            }
            return handlers[side.ordinal()].cast();
        }
        return super.getCapability(cap, side);
    }
    @Override
    public void remove() {
        super.remove();
        handler.invalidate();
        for (LazyOptional<? extends IItemHandler> handler : handlers) {
            handler.invalidate();
        }
    }
    public int getCraftingProgress() {
        return craftingProgress;
    }
    public int getCraftingTotalTime() {
        return craftingTotalTime;
    }
    public boolean isCrafting() {
        return isCrafting;
    }
    public float getSuccessChance() {
        if (!isCrafting || currentGift.isEmpty()) return 0.0f;
        ItemStack enhanceDirt = itemHandler.getStackInSlot(0);
        return calculateSuccessChance(enhanceDirt, currentGift);
    }
}