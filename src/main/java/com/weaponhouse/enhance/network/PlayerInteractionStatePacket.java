package com.weaponhouse.enhance.network;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.session.ServerBlockSession;
import com.weaponhouse.enhance.session.ServerSessionManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;
public class PlayerInteractionStatePacket {
    private final BlockPos sessionPos;
    private final boolean isInteracting;
    public PlayerInteractionStatePacket(BlockPos sessionPos, boolean isInteracting) {
        this.sessionPos = sessionPos;
        this.isInteracting = isInteracting;
    }
    public static void encode(PlayerInteractionStatePacket packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.sessionPos);
        buffer.writeBoolean(packet.isInteracting);
    }
    public static PlayerInteractionStatePacket decode(PacketBuffer buffer) {
        return new PlayerInteractionStatePacket(buffer.readBlockPos(), buffer.readBoolean());
    }
    public static void handle(PlayerInteractionStatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;
            ServerSessionManager manager = ServerSessionManager.getInstance();
            ServerBlockSession session = manager.getSession(packet.sessionPos);
            if (session != null) {
                if (!session.getPlayers().contains(player.getUniqueID())) {
                    return;
                }
                session.setInteracting(player.getUniqueID(), packet.isInteracting);
                manager.syncSessionState(session);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}