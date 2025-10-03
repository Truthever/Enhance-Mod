package com.weaponhouse.enhance.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
public class BossDataPacket {
    public UUID entityId;
    public List<String> modNames;
    public int tier;
    public String entityType;
    public float currentHealth;
    public float maxHealth;
    public double distance;
    public BossDataPacket(UUID entityId, List<String> modNames, int tier, String entityType,
                          float currentHealth, float maxHealth, double distance) {
        this.entityId = entityId;
        this.modNames = modNames;
        this.tier = tier;
        this.entityType = entityType;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.distance = distance;
    }
    public static void encode(BossDataPacket packet, PacketBuffer buffer) {
        buffer.writeUniqueId(packet.entityId);
        buffer.writeInt(packet.tier);
        buffer.writeString(packet.entityType);
        buffer.writeFloat(packet.currentHealth);
        buffer.writeFloat(packet.maxHealth);
        buffer.writeDouble(packet.distance);
        buffer.writeInt(packet.modNames.size());
        for (String modName : packet.modNames) {
            buffer.writeString(modName);
        }
    }
    public static BossDataPacket decode(PacketBuffer buffer) {
        UUID entityId = buffer.readUniqueId();
        int tier = buffer.readInt();
        String entityType = buffer.readString(32767);
        float currentHealth = buffer.readFloat();
        float maxHealth = buffer.readFloat();
        double distance = buffer.readDouble();
        int size = buffer.readInt();
        List<String> modNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            modNames.add(buffer.readString(32767));
        }
        return new BossDataPacket(entityId, modNames, tier, entityType, currentHealth, maxHealth, distance);
    }
    public static void handle(BossDataPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                com.weaponhouse.enhance.client.EnhancementHUD.BossData data =
                        new com.weaponhouse.enhance.client.EnhancementHUD.BossData(
                                packet.modNames, packet.tier, packet.entityType,
                                packet.currentHealth, packet.maxHealth, packet.distance
                        );
                com.weaponhouse.enhance.client.EnhancementHUD.setBossData(packet.entityId, data);
            }
        });
        context.get().setPacketHandled(true);
    }
}
