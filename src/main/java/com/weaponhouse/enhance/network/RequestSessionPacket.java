package com.weaponhouse.enhance.network;
import com.weaponhouse.enhance.session.ServerBlockSession;
import com.weaponhouse.enhance.session.ServerSessionManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
public class RequestSessionPacket {
    private final BlockPos blockPos;
    private final UUID playerUUID;
    public RequestSessionPacket(BlockPos blockPos, UUID playerUUID) {
        this.blockPos = blockPos;
        this.playerUUID = playerUUID;
    }
    public static void encode(RequestSessionPacket packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeUniqueId(packet.playerUUID);
    }
    public static RequestSessionPacket decode(PacketBuffer buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        UUID playerUUID = buffer.readUniqueId();
        return new RequestSessionPacket(blockPos, playerUUID);
    }
    public static void handle(RequestSessionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null || sender.server == null) {
                return;
            }
            ServerSessionManager serverSessionManager = ServerSessionManager.getInstance();
            ServerBlockSession serverSession = serverSessionManager.createOrJoinSession(
                    packet.blockPos,
                    packet.playerUUID
            );
            Set<UUID> sessionPlayers = serverSession.getPlayers();
            for (UUID playerUUID : sessionPlayers) {
                ServerPlayerEntity targetPlayer = sender.server.getPlayerList().getPlayerByUUID(playerUUID);
                if (targetPlayer != null) {}
            }
        });
        ctx.get().setPacketHandled(true);
    }
}