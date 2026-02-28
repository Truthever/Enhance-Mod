package com.weaponhouse.enhance.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;
public class SpeedUpdatedPacket {
    private final float speed;
    public SpeedUpdatedPacket(float speed) {
        this.speed = speed;
    }
    public static void encode(SpeedUpdatedPacket msg, PacketBuffer buf) {
        buf.writeFloat(msg.speed);
    }
    public static SpeedUpdatedPacket decode(PacketBuffer buf) {
        return new SpeedUpdatedPacket(buf.readFloat());
    }
    public static void handle(SpeedUpdatedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                if (net.minecraft.client.Minecraft.getInstance().currentScreen instanceof com.weaponhouse.enhance.client.gui.SpeedReducerScreen) {
                    ((com.weaponhouse.enhance.client.gui.SpeedReducerScreen) net.minecraft.client.Minecraft.getInstance().currentScreen)
                            .updateCurrentSpeed(msg.speed);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}