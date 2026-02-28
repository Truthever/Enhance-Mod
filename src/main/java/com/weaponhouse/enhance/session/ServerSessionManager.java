package com.weaponhouse.enhance.session;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.network.SessionStateSyncPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class ServerSessionManager {
    private static final ServerSessionManager INSTANCE = new ServerSessionManager();
    public static ServerSessionManager getInstance() {
        return INSTANCE;
    }
    private final Map<BlockPos, ServerBlockSession> sessions = new ConcurrentHashMap<>();
    public void syncSessionState(ServerBlockSession session) {
        SessionStateSyncPacket packet = new SessionStateSyncPacket(
                session.getPos(),
                session.getInteractionStates(),
                null
        );
        for (UUID uuid : session.getPlayers()) {
            ServerPlayerEntity player = getPlayerByUUID(uuid);
            if (player != null) {
                Enhance.sendToClient(packet, player);
            }
        }
    }
    public ServerBlockSession createOrJoinSession(BlockPos blockPos, UUID playerUUID) {
        ServerBlockSession session = sessions.get(blockPos);
        if (session == null) {
            session = new ServerBlockSession(blockPos);
            sessions.put(blockPos, session);
        }
        if (!session.getPlayers().contains(playerUUID)) {
            session.addPlayer(playerUUID);
        }
        syncSessionState(session);
        return session;
    }
    private ServerPlayerEntity getPlayerByUUID(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(uuid);
    }
    public ServerBlockSession getSession(BlockPos pos) {
        return sessions.get(pos);
    }
}