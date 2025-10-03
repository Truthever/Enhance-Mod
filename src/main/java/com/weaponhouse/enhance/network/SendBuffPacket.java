package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.client.gui.EnhanceDisplayScreen;
import com.weaponhouse.enhance.client.gui.EnhanceRemoveScreen;
import com.weaponhouse.enhance.client.gui.ReplaceScreen;
import com.weaponhouse.enhance.client.gui.SacrificeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
public class SendBuffPacket {
    private final List<String> buffs;
    private final UUID targetPlayerUUID;
    public SendBuffPacket(List<String> buffs, UUID targetPlayerUUID) {
        this.buffs = buffs;
        this.targetPlayerUUID = targetPlayerUUID;
    }
    public static void encode(SendBuffPacket msg, PacketBuffer buffer) {
        buffer.writeInt(msg.buffs.size());
        for (String buff : msg.buffs) {
            buffer.writeString(buff, 32767);
        }
        buffer.writeBoolean(msg.targetPlayerUUID != null);
        if (msg.targetPlayerUUID != null) {
            buffer.writeUniqueId(msg.targetPlayerUUID);
        }
    }
    public static SendBuffPacket decode(PacketBuffer buffer) {
        int size = buffer.readInt();
        List<String> buffs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            buffs.add(buffer.readString(32767));
        }
        UUID targetUUID = null;
        if (buffer.readBoolean()) {
            targetUUID = buffer.readUniqueId();
        }
        return new SendBuffPacket(buffs, targetUUID);
    }
    public static void handle(SendBuffPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.currentScreen == null || mc.player == null) {
                return;
            }
            UUID localPlayerUUID = mc.player.getUniqueID();
            if (msg.targetPlayerUUID == null || msg.targetPlayerUUID.equals(localPlayerUUID)) {
                if (mc.currentScreen instanceof EnhanceRemoveScreen) {
                    ((EnhanceRemoveScreen) mc.currentScreen).updateBuffs(msg.buffs);
                } else if (mc.currentScreen instanceof EnhanceDisplayScreen) {
                    ((EnhanceDisplayScreen) mc.currentScreen).updateBuffs(msg.buffs);
                } else if (mc.currentScreen instanceof SacrificeScreen) {
                    ((SacrificeScreen) mc.currentScreen).updateBuffs(msg.buffs);
                } else if (mc.currentScreen instanceof ReplaceScreen) {
                    ((ReplaceScreen) mc.currentScreen).updateCurrentBuffs(msg.buffs);
                }
            } else {
                if (mc.currentScreen instanceof ReplaceScreen) {
                    ((ReplaceScreen) mc.currentScreen).updateOtherBuffs(msg.buffs, msg.targetPlayerUUID);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    public static void sendBuffData(PlayerEntity player) {
        sendBuffDataToTarget(player, (ServerPlayerEntity) player);
    }
    public static void sendBuffDataToTarget(PlayerEntity buffOwner, ServerPlayerEntity target) {
        CompoundNBT nbt = buffOwner.getPersistentData();
        List<String> buffs = new ArrayList<>();
        if (nbt.contains("WeaponHouseBuffs")) {
            CompoundNBT buffsNbt = nbt.getCompound("WeaponHouseBuffs");
            buffsNbt.keySet().forEach(buff -> {
                int level = buffsNbt.getInt(buff);
                buffs.add(buff + " Lv." + level);
            });
        }
        Enhance.sendToPlayer(
                new SendBuffPacket(buffs, buffOwner.getUniqueID()),
                target
        );
    }
}