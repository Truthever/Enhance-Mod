package com.weaponhouse.enhance.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;
public class SpiritShieldPacket {
    private final float currentShield;
    private final float maxShield;
    public SpiritShieldPacket(float currentShield, float maxShield) {
        this.currentShield = currentShield;
        this.maxShield = maxShield;
    }
    public float getCurrentShield() {
        return currentShield;
    }
    public float getMaxShield() {
        return maxShield;
    }
    public static void encode(SpiritShieldPacket msg, PacketBuffer buffer) {
        buffer.writeFloat(msg.currentShield);
        buffer.writeFloat(msg.maxShield);
    }
    public static SpiritShieldPacket decode(PacketBuffer buffer) {
        return new SpiritShieldPacket(buffer.readFloat(), buffer.readFloat());
    }
    public static void handle(SpiritShieldPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSpiritShieldPacket(msg.getCurrentShield(), msg.getMaxShield())));
        ctx.get().setPacketHandled(true);
    }
}