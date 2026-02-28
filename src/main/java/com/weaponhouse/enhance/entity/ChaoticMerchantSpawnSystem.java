package com.weaponhouse.enhance.entity;

import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
@Mod.EventBusSubscriber(modid = "enhance")
public class ChaoticMerchantSpawnSystem extends WorldSavedData {
    private static final String DATA_NAME = "enhance_chaotic_merchant_spawn_data";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long SPAWN_INTERVAL_TICKS = 8640000L;
    private static final long DAY_TICKS = 1728000L;
    private long lastSpawnTime = 0;
    private long firstStartTime = 0;
    private boolean hasInitialized = false;
    private boolean firstDayCompleted = false;
    public ChaoticMerchantSpawnSystem() {
        super(DATA_NAME);
    }
    public void checkAndSpawnChaoticMerchant(ServerWorld world) {
        long currentTime = world.getGameTime();
        if (!hasInitialized) {
            initializeTimer(world, currentTime);
            return;
        }
        if (!firstDayCompleted && (currentTime - firstStartTime) >= DAY_TICKS) {
            firstDayCompleted = true;
            markDirty();
        }
        if (firstDayCompleted && (lastSpawnTime == 0 || (currentTime - lastSpawnTime) >= SPAWN_INTERVAL_TICKS)) {
            if (spawnChaoticMerchant(world)) {
                lastSpawnTime = currentTime;
                markDirty();
            }
        }
    }
    private void initializeTimer(ServerWorld world, long currentTime) {
        firstStartTime = currentTime;
        hasInitialized = true;
        firstDayCompleted = false;
        markDirty();
    }
    private boolean spawnChaoticMerchant(ServerWorld world) {
        List<ServerPlayerEntity> players = world.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return false;
        }
        Random random = world.getRandom();
        ServerPlayerEntity targetPlayer = players.get(random.nextInt(players.size()));
        BlockPos playerPos = targetPlayer.getPosition();
        BlockPos spawnPos = findSafeSpawnLocation(world, playerPos);
        try {
            ChaoticMerchantEntity merchant = new ChaoticMerchantEntity(
                    RegistryHandler.CHAOTIC_MERCHANT.get(),
                    world
            );

            merchant.setLocationAndAngles(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    random.nextFloat() * 360.0F,
                    0.0F
            );
            boolean added = world.addEntity(merchant);
            if (added) {
                TranslationTextComponent spawnMessage = new TranslationTextComponent("message.enhance.chaotic_merchant.spawn.broadcast");
                for (ServerPlayerEntity player : players) {
                    player.sendMessage(spawnMessage, player.getUniqueID());
                }
                TranslationTextComponent privateMessage = new TranslationTextComponent(
                        "message.enhance.chaotic_merchant.spawn.private",
                        spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()
                );
                targetPlayer.sendMessage(privateMessage, targetPlayer.getUniqueID());
                world.playSound(null, spawnPos,
                        SoundEvents.ENTITY_WANDERING_TRADER_TRADE,
                        SoundCategory.NEUTRAL, 1.0F, 1.0F);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
    private BlockPos findSafeSpawnLocation(ServerWorld world, BlockPos centerPos) {
        Random random = world.getRandom();
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = centerPos.getX() + random.nextInt(100) - 50;
            int z = centerPos.getZ() + random.nextInt(100) - 50;
            int maxY = Math.min(world.getHeight(), 255);
            BlockPos.Mutable mutablePos = new BlockPos.Mutable(x, maxY, z);
            while (mutablePos.getY() > 0) {
                if (world.getBlockState(mutablePos).isSolid()) {
                    BlockPos standingPos = mutablePos.up();
                    BlockPos above1 = standingPos.up();
                    BlockPos above2 = standingPos.up(2);

                    if (world.isAirBlock(standingPos) &&
                            world.isAirBlock(above1) &&
                            world.isAirBlock(above2)) {
                        if (!world.getBlockState(standingPos).getMaterial().isLiquid() &&
                                !world.getBlockState(above1).getMaterial().isLiquid()) {
                            return standingPos;
                        }
                    }
                }
                mutablePos.move(net.minecraft.util.Direction.DOWN);
            }
        }
        return centerPos.up(3);
    }
    @Override
    public void read(@Nonnull CompoundNBT nbt) {
        if (nbt.contains("lastSpawnTime")) {
            this.lastSpawnTime = nbt.getLong("lastSpawnTime");
        }
        if (nbt.contains("firstStartTime")) {
            this.firstStartTime = nbt.getLong("firstStartTime");
        }
        if (nbt.contains("hasInitialized")) {
            this.hasInitialized = nbt.getBoolean("hasInitialized");
        }
        if (nbt.contains("firstDayCompleted")) {
            this.firstDayCompleted = nbt.getBoolean("firstDayCompleted");
        }
    }
    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compound) {
        compound.putLong("lastSpawnTime", this.lastSpawnTime);
        compound.putLong("firstStartTime", this.firstStartTime);
        compound.putBoolean("hasInitialized", this.hasInitialized);
        compound.putBoolean("firstDayCompleted", this.firstDayCompleted);
        return compound;
    }
    public static ChaoticMerchantSpawnSystem get(ServerWorld world) {
        return world.getSavedData().getOrCreate(
                ChaoticMerchantSpawnSystem::new,
                DATA_NAME
        );
    }
    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        for (ServerWorld world : server.getWorlds()) {
            get(world);
        }
    }
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        if (ServerLifecycleHooks.getCurrentServer().getTickCounter() % 20 != 0) {
            return;
        }
        for (ServerWorld world : ServerLifecycleHooks.getCurrentServer().getWorlds()) {
            try {
                if (world.getDimensionKey() == World.OVERWORLD) {
                    ChaoticMerchantSpawnSystem spawnSystem = get(world);
                    spawnSystem.checkAndSpawnChaoticMerchant(world);
                }
            } catch (Exception e) {
                LOGGER.error("检查混沌商人生成时发生错误", e);
            }
        }
    }
}