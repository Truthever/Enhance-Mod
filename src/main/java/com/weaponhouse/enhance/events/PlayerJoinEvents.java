package com.weaponhouse.enhance.events;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.util.ConfigLoader;
import com.weaponhouse.enhance.network.ConfigSyncPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
@Mod.EventBusSubscriber(modid = "enhance")
public class PlayerJoinEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            Enhance.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ConfigSyncPacket(ConfigLoader.ENHANCE_BOSSBAR_ENABLED)
            );
        }
    }
}