package com.weaponhouse.enhance;

import com.weaponhouse.enhance.advancements.*;
import com.weaponhouse.enhance.network.*;
import com.weaponhouse.enhance.util.ConfigLoader;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("enhance")
public class Enhance {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "enhance";
    public static final boolean DEBUG_MODE = true;
    public static SacrificeTrigger SACRIFICE_TRIGGER;
    public static EnhancePowerTrigger ENHANCE_POWER_TRIGGER;
    public static ExtremeRealmTrigger EXTREME_REALM_TRIGGER;
    public static PeakAchievementTrigger PEAK_ACHIEVEMENT_TRIGGER;
    public static FirstEnhancementTrigger FIRST_ENHANCEMENT_TRIGGER;
    public static ObtainEnhanceStoneTrigger OBTAIN_ENHANCE_STONE_TRIGGER;
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public Enhance() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        RegistryHandler.init(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverSetup);
        ConfigLoader.loadConfigs();
    }

    public static void sendToClient(Object msg, ServerPlayerEntity player) {
        INSTANCE.sendTo(
                msg,
                player.connection.getNetworkManager(),
                NetworkDirection.PLAY_TO_CLIENT
        );
    }

    private void setup(final FMLCommonSetupEvent event) {
        int id = 0;
        INSTANCE.registerMessage(id++, RequestSessionPacket.class, RequestSessionPacket::encode, RequestSessionPacket::decode, RequestSessionPacket::handle);
        INSTANCE.registerMessage(id++, SessionSyncPacket.class, SessionSyncPacket::encode, SessionSyncPacket::decode, SessionSyncPacket::handle);
        INSTANCE.registerMessage(id++, RequestBuffPacket.class, RequestBuffPacket::encode, RequestBuffPacket::decode, RequestBuffPacket::handle);
        INSTANCE.registerMessage(id++, RequestSessionPacket.class, RequestSessionPacket::encode, RequestSessionPacket::decode, RequestSessionPacket::handle);
        INSTANCE.registerMessage(id++, RequestOtherBuffPacket.class, RequestOtherBuffPacket::encode, RequestOtherBuffPacket::decode, RequestOtherBuffPacket::handle);
        INSTANCE.registerMessage(id++, ExchangeBuffPacket.class, ExchangeBuffPacket::encode, ExchangeBuffPacket::decode, ExchangeBuffPacket::handle);
        INSTANCE.registerMessage(id++, SendBuffPacket.class, SendBuffPacket::encode, SendBuffPacket::decode, SendBuffPacket::handle);
        INSTANCE.registerMessage(id++, RemoveBuffPacket.class, RemoveBuffPacket::encode, RemoveBuffPacket::decode, RemoveBuffPacket::handle);
        INSTANCE.registerMessage(id++, SacrificeBuffPacket.class, SacrificeBuffPacket::encode, SacrificeBuffPacket::decode, SacrificeBuffPacket::handle);
        INSTANCE.registerMessage(id++, SessionSyncPacket.class, SessionSyncPacket::encode, SessionSyncPacket::decode, SessionSyncPacket::handle);
        INSTANCE.registerMessage(id++, SessionStateSyncPacket.class, SessionStateSyncPacket::encode, SessionStateSyncPacket::decode, SessionStateSyncPacket::handle);
        INSTANCE.registerMessage(id++, PlayerInteractionStatePacket.class, PlayerInteractionStatePacket::encode, PlayerInteractionStatePacket::decode, PlayerInteractionStatePacket::handle);
        INSTANCE.registerMessage(id++, SetSpeedPacket.class, SetSpeedPacket::encode, SetSpeedPacket::decode, SetSpeedPacket::handle);
        INSTANCE.registerMessage(id++, SpeedUpdatedPacket.class, SpeedUpdatedPacket::encode, SpeedUpdatedPacket::decode, SpeedUpdatedPacket::handle);
        INSTANCE.registerMessage(id++, BossDataPacket.class, BossDataPacket::encode, BossDataPacket::decode, BossDataPacket::handle);
        INSTANCE.registerMessage(id++, RemoveBossDataPacket.class, RemoveBossDataPacket::encode, RemoveBossDataPacket::decode, RemoveBossDataPacket::handle);
        INSTANCE.registerMessage(id++, ConfigSyncPacket.class, ConfigSyncPacket::encode, ConfigSyncPacket::decode, ConfigSyncPacket::handle);

        event.enqueueWork(() -> {
            try {
                OBTAIN_ENHANCE_STONE_TRIGGER = CriteriaTriggers.register(new ObtainEnhanceStoneTrigger());
                FIRST_ENHANCEMENT_TRIGGER = CriteriaTriggers.register(new FirstEnhancementTrigger());
                SACRIFICE_TRIGGER = CriteriaTriggers.register(new SacrificeTrigger());
                ENHANCE_POWER_TRIGGER = CriteriaTriggers.register(new EnhancePowerTrigger());
                EXTREME_REALM_TRIGGER = CriteriaTriggers.register(new ExtremeRealmTrigger());
                PEAK_ACHIEVEMENT_TRIGGER = CriteriaTriggers.register(new PeakAchievementTrigger());
            } catch (Exception e) {}
        });
    }

    private void serverSetup(final FMLDedicatedServerSetupEvent event) {
        ConfigLoader.loadConfigs();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.weaponhouse.enhance.client.EnhanceClient.initClient();
        });
    }

    private void onServerStarting(final FMLServerStartingEvent event) {}

    public static void sendToPlayer(Object msg, ServerPlayerEntity player) {
        INSTANCE.sendTo(msg, player.connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }

    public static final ItemGroup TAB = new ItemGroup("enhance_tab") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(RegistryHandler.RED_GIFT.get());
        }
    };

    public static void debug(String message) {
        if (DEBUG_MODE) {
            LOGGER.info("[ENHANCE DEBUG] " + message);
        }
    }
}
