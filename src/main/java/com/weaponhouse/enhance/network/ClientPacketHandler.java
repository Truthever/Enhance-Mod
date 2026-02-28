package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.client.EnhancementHUD;
import com.weaponhouse.enhance.client.gui.EnhanceDisplayScreen;
import com.weaponhouse.enhance.client.gui.EnhanceRemoveScreen;
import com.weaponhouse.enhance.client.gui.ReplaceScreen;
import com.weaponhouse.enhance.client.gui.SacrificeScreen;
import com.weaponhouse.enhance.session.ClientSessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {
    public static void handleSendBuffPacket(List<String> buffs, UUID targetPlayerUUID) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.currentScreen == null || mc.player == null) {
            return;
        }
        UUID localPlayerUUID = mc.player.getUniqueID();
        if (targetPlayerUUID == null || targetPlayerUUID.equals(localPlayerUUID)) {
            if (mc.currentScreen instanceof EnhanceRemoveScreen) {
                ((EnhanceRemoveScreen) mc.currentScreen).updateBuffs(buffs);
            } else if (mc.currentScreen instanceof EnhanceDisplayScreen) {
                ((EnhanceDisplayScreen) mc.currentScreen).updateBuffs(buffs);
            } else if (mc.currentScreen instanceof SacrificeScreen) {
                ((SacrificeScreen) mc.currentScreen).updateBuffs(buffs);
            } else if (mc.currentScreen instanceof ReplaceScreen) {
                ((ReplaceScreen) mc.currentScreen).updateCurrentBuffs(buffs);
            }
        } else {
            if (mc.currentScreen instanceof ReplaceScreen) {
                ((ReplaceScreen) mc.currentScreen).updateOtherBuffs(buffs, targetPlayerUUID);
            }
        }
    }
    public static void handleSessionSyncPacket(BlockPos blockPos, Set<UUID> playerUUIDs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.world != null && mc.player != null) {
            ClientSessionManager.getInstance().updateSession(blockPos, playerUUIDs);
        }
    }
    public static void handleSessionStateSyncPacket(BlockPos sessionPos, Map<UUID, Boolean> interactionStates, UUID disconnectedPlayer) {
        Minecraft.getInstance().enqueue(() -> {
            Screen screen = Minecraft.getInstance().currentScreen;
            if (screen instanceof ReplaceScreen) {
                ReplaceScreen replaceScreen = (ReplaceScreen) screen;
                replaceScreen.handleSessionStateUpdate(interactionStates, disconnectedPlayer);
            }
        });
    }
    public static void handleSpiritShieldPacket(float currentShield, float maxShield) {
        EnhancementHUD.setSpiritShieldData(currentShield, maxShield);
    }
}