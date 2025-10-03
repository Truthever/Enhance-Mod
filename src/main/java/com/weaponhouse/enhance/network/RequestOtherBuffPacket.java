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
public class RequestOtherBuffPacket {
    private final BlockPos sessionPos;
    private final UUID requesterUUID;
    public RequestOtherBuffPacket(BlockPos sessionPos, UUID requesterUUID) {
        this.sessionPos = sessionPos;
        this.requesterUUID = requesterUUID;
    }
    public static void encode(RequestOtherBuffPacket packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.sessionPos);
        buffer.writeUniqueId(packet.requesterUUID);
    }
    public static RequestOtherBuffPacket decode(PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        UUID requester = buffer.readUniqueId();
        return new RequestOtherBuffPacket(pos, requester);
    }
    public static void handle(RequestOtherBuffPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity requester = ctx.get().getSender();
            if (requester == null || requester.server == null) {
                return;
            }
            ServerSessionManager sessionManager = ServerSessionManager.getInstance();
            ServerBlockSession session = sessionManager.getSession(packet.sessionPos);
            if (session == null || !session.getPlayers().contains(packet.requesterUUID)) {
                return;
            }
            Set<UUID> sessionPlayers = session.getPlayers();
            UUID otherPlayerUUID = sessionPlayers.stream()
                    .filter(uuid -> !uuid.equals(packet.requesterUUID))
                    .findFirst()
                    .orElse(null);
            if (otherPlayerUUID == null || !sessionPlayers.contains(otherPlayerUUID)) {
                return;
            }
            ServerPlayerEntity otherPlayer = null;
            for (ServerPlayerEntity onlinePlayer : requester.server.getPlayerList().getPlayers()) {
                if (onlinePlayer.getUniqueID().equals(otherPlayerUUID)) {
                    otherPlayer = onlinePlayer;
                    break;
                }
            }
            if (otherPlayer != null) {
                SendBuffPacket.sendBuffDataToTarget(otherPlayer, requester);
            } else {
            }
        });
        ctx.get().setPacketHandled(true);
    }
}