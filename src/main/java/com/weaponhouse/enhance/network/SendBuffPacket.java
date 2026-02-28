package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.Enhance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
    public List<String> getBuffs() {
        return buffs;
    }
    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
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
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSendBuffPacket(msg.getBuffs(), msg.getTargetPlayerUUID())));
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