package com.weaponhouse.enhance.network;
import com.weaponhouse.enhance.client.gui.ReplaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
public class SessionStateSyncPacket {
    private final BlockPos sessionPos;
    private final Map<UUID, Boolean> interactionStates;
    private final UUID disconnectedPlayer;
    public SessionStateSyncPacket(BlockPos sessionPos, Map<UUID, Boolean> interactionStates, UUID disconnectedPlayer) {
        this.sessionPos = sessionPos;
        this.interactionStates = interactionStates;
        this.disconnectedPlayer = disconnectedPlayer;
    }
    public static void encode(SessionStateSyncPacket packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.sessionPos);
        buffer.writeInt(packet.interactionStates.size());
        for (Map.Entry<UUID, Boolean> entry : packet.interactionStates.entrySet()) {
            buffer.writeUniqueId(entry.getKey());
            buffer.writeBoolean(entry.getValue());
        }
        buffer.writeBoolean(packet.disconnectedPlayer != null);
        if (packet.disconnectedPlayer != null) {
            buffer.writeUniqueId(packet.disconnectedPlayer);
        }
    }
    public static SessionStateSyncPacket decode(PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        int size = buffer.readInt();
        Map<UUID, Boolean> states = new HashMap<>();
        for (int i = 0; i < size; i++) {
            states.put(buffer.readUniqueId(), buffer.readBoolean());
        }
        UUID disconnected = buffer.readBoolean() ? buffer.readUniqueId() : null;
        return new SessionStateSyncPacket(pos, states, disconnected);
    }
    public static void handle(SessionStateSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().enqueue(() -> {
                Screen screen = Minecraft.getInstance().currentScreen;
                if (screen instanceof ReplaceScreen) {
                    ReplaceScreen replaceScreen = (ReplaceScreen) screen;
                    replaceScreen.handleSessionStateUpdate(packet.interactionStates, packet.disconnectedPlayer);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}