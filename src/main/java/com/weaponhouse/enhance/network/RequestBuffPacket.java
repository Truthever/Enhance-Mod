package com.weaponhouse.enhance.network;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;
public class RequestBuffPacket {
    public RequestBuffPacket() {}
    public static void encode(RequestBuffPacket msg, PacketBuffer buffer) {}
    public static RequestBuffPacket decode(PacketBuffer buffer) {
        return new RequestBuffPacket();
    }
    public static void handle(RequestBuffPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                SendBuffPacket.sendBuffData(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}