package com.weaponhouse.enhance.network;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.session.ClientSessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
public class SessionSyncPacket {
    private BlockPos blockPos;
    private Set<UUID> playerUUIDs;
    public SessionSyncPacket(BlockPos blockPos, Set<UUID> playerUUIDs) {
        this.blockPos = blockPos;
        this.playerUUIDs = playerUUIDs;
    }
    public static void encode(SessionSyncPacket packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeVarInt(packet.playerUUIDs.size());
        for (UUID uuid : packet.playerUUIDs) {
            buffer.writeUniqueId(uuid);
        }
    }
    public static SessionSyncPacket decode(PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        int size = buffer.readVarInt();
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < size; i++) {
            uuids.add(buffer.readUniqueId());
        }
        return new SessionSyncPacket(pos, uuids);
    }
    public static void handle(SessionSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            context.get().setPacketHandled(true);
            return;
        }
        context.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.world != null && mc.player != null) {
                ClientSessionManager.getInstance().updateSession(
                        packet.blockPos,
                        packet.playerUUIDs
                );
            }
        });
        context.get().setPacketHandled(true);
    }
}