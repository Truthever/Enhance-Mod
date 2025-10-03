package com.weaponhouse.enhance.events;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
@Mod.EventBusSubscriber(modid = "enhance", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        IEventBus bus = MinecraftForge.EVENT_BUS;
    }
    public static void onClientSetup(FMLClientSetupEvent event) {
    }
    public static void onServerStarting(FMLServerStartingEvent event) {
    }
}
