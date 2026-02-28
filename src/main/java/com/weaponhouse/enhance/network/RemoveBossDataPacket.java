package com.weaponhouse.enhance.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;
public class RemoveBossDataPacket {
    public UUID entityId;
    public RemoveBossDataPacket(UUID entityId) {
        this.entityId = entityId;
    }
    public static void encode(RemoveBossDataPacket packet, PacketBuffer buffer) {
        buffer.writeUniqueId(packet.entityId);
    }
    public static RemoveBossDataPacket decode(PacketBuffer buffer) {
        UUID entityId = buffer.readUniqueId();
        return new RemoveBossDataPacket(entityId);
    }
    public static void handle(RemoveBossDataPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                com.weaponhouse.enhance.client.EnhancementHUD.removeBossData(packet.entityId);
            }
        });
        context.get().setPacketHandled(true);
    }
}