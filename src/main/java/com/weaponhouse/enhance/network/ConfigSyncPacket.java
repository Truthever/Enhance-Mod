package com.weaponhouse.enhance.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;
public class ConfigSyncPacket {
    private final boolean bossbarEnabled;
    public ConfigSyncPacket(boolean bossbarEnabled) {
        this.bossbarEnabled = bossbarEnabled;
    }
    public static ConfigSyncPacket decode(PacketBuffer buffer) {
        return new ConfigSyncPacket(buffer.readBoolean());
    }
    public static void encode(ConfigSyncPacket packet, PacketBuffer buffer) {
        buffer.writeBoolean(packet.bossbarEnabled);
    }
    public static void handle(ConfigSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            com.weaponhouse.enhance.client.ClientConfigCache.setBossbarEnabled(packet.bossbarEnabled);
        });
        context.setPacketHandled(true);
    }
}