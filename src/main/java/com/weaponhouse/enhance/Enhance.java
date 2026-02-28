package com.weaponhouse.enhance;

import com.weaponhouse.enhance.advancements.*;
import com.weaponhouse.enhance.client.ClientSide;
import com.weaponhouse.enhance.client.EntityRenderHandler;
import com.weaponhouse.enhance.commands.CommandRegistrationHandler;
import com.weaponhouse.enhance.effects.EffectRegistry;
import com.weaponhouse.enhance.enhances.EnhanceEventHandlers;
import com.weaponhouse.enhance.entity.ChaoticMerchantEntity;
import com.weaponhouse.enhance.network.*;
import com.weaponhouse.enhance.util.ConfigLoader;
import com.weaponhouse.enhance.util.RegistryHandler;
import com.weaponhouse.enhance.world.dimension.SkylandsBiomeProvider;
import com.weaponhouse.enhance.world.dimension.SkylandsChunkGenerator;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSide::new);
        commonStart(modEventBus);
    }
    private static void commonStart(IEventBus modEventBus) {
        modEventBus.addListener(Enhance::setup);
        modEventBus.addListener(Enhance::serverSetup);
        RegistryHandler.init(modEventBus);
        modEventBus.addGenericListener(Effect.class, EffectRegistry::registerEffects);
        MinecraftForge.EVENT_BUS.addListener(CommandRegistrationHandler::onCommandRegister);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onEntityJoinWorld);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onArrowShoot);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onPlayerDeath);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onEntityDeath);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onEntityUpdate);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onPlayerUseItemStart);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onPlayerUseItemTick);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onWorldTick);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onEntityTick);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onProjectileImpact);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onEntityDamage);
        MinecraftForge.EVENT_BUS.addListener(EnhanceEventHandlers::onLivingUpdate);
        MinecraftForge.EVENT_BUS.addListener(com.weaponhouse.enhance.invasion.InvasionEventHandlers::onPlayerDeath);
        MinecraftForge.EVENT_BUS.addListener(com.weaponhouse.enhance.invasion.InvasionEventHandlers::onPlayerLogout);
        MinecraftForge.EVENT_BUS.addListener(com.weaponhouse.enhance.events.AdvancementEventHandler::onAdvancementEarned);
        MinecraftForge.EVENT_BUS.addListener(com.weaponhouse.enhance.invasion.InvasionEventHandlers::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(com.weaponhouse.enhance.items.ItemEventHandlers::onEntityRightClick);
        MinecraftForge.EVENT_BUS.addListener(com.weaponhouse.enhance.items.ItemEventHandlers::onItemRightClick);
        ConfigLoader.loadConfigs();
        MinecraftForge.EVENT_BUS.addListener(Enhance::onServerStarting);
    }
    private static void setup(final FMLCommonSetupEvent event) {
        int id = 0;
        INSTANCE.registerMessage(id++, RequestSessionPacket.class, RequestSessionPacket::encode, RequestSessionPacket::decode, RequestSessionPacket::handle);
        INSTANCE.registerMessage(id++, SessionSyncPacket.class, SessionSyncPacket::encode, SessionSyncPacket::decode, SessionSyncPacket::handle);
        INSTANCE.registerMessage(id++, RequestBuffPacket.class, RequestBuffPacket::encode, RequestBuffPacket::decode, RequestBuffPacket::handle);
        INSTANCE.registerMessage(id++, RequestOtherBuffPacket.class, RequestOtherBuffPacket::encode, RequestOtherBuffPacket::decode, RequestOtherBuffPacket::handle);
        INSTANCE.registerMessage(id++, ExchangeBuffPacket.class, ExchangeBuffPacket::encode, ExchangeBuffPacket::decode, ExchangeBuffPacket::handle);
        INSTANCE.registerMessage(id++, SendBuffPacket.class, SendBuffPacket::encode, SendBuffPacket::decode, SendBuffPacket::handle);
        INSTANCE.registerMessage(id++, RemoveBuffPacket.class, RemoveBuffPacket::encode, RemoveBuffPacket::decode, RemoveBuffPacket::handle);
        INSTANCE.registerMessage(id++, SacrificeBuffPacket.class, SacrificeBuffPacket::encode, SacrificeBuffPacket::decode, SacrificeBuffPacket::handle);
        INSTANCE.registerMessage(id++, SessionStateSyncPacket.class, SessionStateSyncPacket::encode, SessionStateSyncPacket::decode, SessionStateSyncPacket::handle);
        INSTANCE.registerMessage(id++, PlayerInteractionStatePacket.class, PlayerInteractionStatePacket::encode, PlayerInteractionStatePacket::decode, PlayerInteractionStatePacket::handle);
        INSTANCE.registerMessage(id++, SetSpeedPacket.class, SetSpeedPacket::encode, SetSpeedPacket::decode, SetSpeedPacket::handle);
        INSTANCE.registerMessage(id++, SpeedUpdatedPacket.class, SpeedUpdatedPacket::encode, SpeedUpdatedPacket::decode, SpeedUpdatedPacket::handle);
        INSTANCE.registerMessage(id++, BossDataPacket.class, BossDataPacket::encode, BossDataPacket::decode, BossDataPacket::handle);
        INSTANCE.registerMessage(id++, RemoveBossDataPacket.class, RemoveBossDataPacket::encode, RemoveBossDataPacket::decode, RemoveBossDataPacket::handle);
        INSTANCE.registerMessage(id++, ConfigSyncPacket.class, ConfigSyncPacket::encode, ConfigSyncPacket::decode, ConfigSyncPacket::handle);
        INSTANCE.registerMessage(id++, SpiritShieldPacket.class, SpiritShieldPacket::encode, SpiritShieldPacket::decode, SpiritShieldPacket::handle);
        INSTANCE.registerMessage(id++, OpenAlchemyFurnacePacket.class, OpenAlchemyFurnacePacket::encode, OpenAlchemyFurnacePacket::decode, OpenAlchemyFurnacePacket::handle);
        INSTANCE.registerMessage(id++, AlchemyFurnaceSyncPacket.class, AlchemyFurnaceSyncPacket::encode, AlchemyFurnaceSyncPacket::decode, AlchemyFurnaceSyncPacket::handle);
        INSTANCE.registerMessage(id++, BlockLinkEffectPacket.class, BlockLinkEffectPacket::encode, BlockLinkEffectPacket::decode, BlockLinkEffectPacket::handle);
        event.enqueueWork(() -> {
            OBTAIN_ENHANCE_STONE_TRIGGER = CriteriaTriggers.register(new ObtainEnhanceStoneTrigger());
            FIRST_ENHANCEMENT_TRIGGER = CriteriaTriggers.register(new FirstEnhancementTrigger());
            SACRIFICE_TRIGGER = CriteriaTriggers.register(new SacrificeTrigger());
            ENHANCE_POWER_TRIGGER = CriteriaTriggers.register(new EnhancePowerTrigger());
            EXTREME_REALM_TRIGGER = CriteriaTriggers.register(new ExtremeRealmTrigger());
            PEAK_ACHIEVEMENT_TRIGGER = CriteriaTriggers.register(new PeakAchievementTrigger());
            GlobalEntityTypeAttributes.put(
                    RegistryHandler.CHAOTIC_MERCHANT.get(),
                    ChaoticMerchantEntity.registerAttributes().create()
            );
            if (FMLEnvironment.dist == Dist.CLIENT) {
                EntityRenderHandler.registerEntityRenderers();
            }
            Registry.register(Registry.CHUNK_GENERATOR_CODEC,
                    new ResourceLocation(MOD_ID, "chaoslands_chunk_generator"),
                    SkylandsChunkGenerator.CODEC);
            Registry.register(Registry.BIOME_PROVIDER_CODEC,
                    new ResourceLocation(MOD_ID, "chaoslands_biome_provider"),
                    SkylandsBiomeProvider.CODEC);
        });
    }
    private static void serverSetup(final FMLDedicatedServerSetupEvent event) {
        ConfigLoader.loadConfigs();
    }
    private static void onServerStarting(final FMLServerStartingEvent event) {}
    public static void sendToClient(Object msg, ServerPlayerEntity player) {
        INSTANCE.sendTo(
                msg,
                player.connection.getNetworkManager(),
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
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