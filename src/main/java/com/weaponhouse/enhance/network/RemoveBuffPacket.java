package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;
public class RemoveBuffPacket {
    private final String buffToRemove;
    public RemoveBuffPacket(String buffToRemove) {
        this.buffToRemove = buffToRemove;
    }
    public static void encode(RemoveBuffPacket msg, PacketBuffer buffer) {
        buffer.writeString(msg.buffToRemove, 32767);
    }
    public static RemoveBuffPacket decode(PacketBuffer buffer) {
        String buffToRemove = buffer.readString(32767);
        return new RemoveBuffPacket(buffToRemove);
    }
    public static void handle(RemoveBuffPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                CompoundNBT data = player.getPersistentData();
                if (data.contains(EnhanceCommand.BUFF_TAG)) {
                    CompoundNBT buffs = data.getCompound(EnhanceCommand.BUFF_TAG);
                    if (buffs.contains(msg.buffToRemove)) {
                        buffs.remove(msg.buffToRemove);
                        data.put(EnhanceCommand.BUFF_TAG, buffs);
                        SendBuffPacket.sendBuffData(player);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
