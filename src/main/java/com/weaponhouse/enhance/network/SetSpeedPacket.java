package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.Enhance;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;
public class SetSpeedPacket {
    private final float speed;
    public SetSpeedPacket(float speed) {
        this.speed = speed;
    }
    public static void encode(SetSpeedPacket msg, PacketBuffer buf) {
        buf.writeFloat(msg.speed);
    }
    public static SetSpeedPacket decode(PacketBuffer buf) {
        return new SetSpeedPacket(buf.readFloat());
    }
    public static void handle(SetSpeedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                float clampedSpeed = Math.max(0.0F, Math.min(1.0F, msg.speed));
                Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(clampedSpeed);
                player.getPersistentData().putFloat("BaseMovementSpeed", clampedSpeed);
                Enhance.INSTANCE.sendTo(new SpeedUpdatedPacket(clampedSpeed), player.connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}