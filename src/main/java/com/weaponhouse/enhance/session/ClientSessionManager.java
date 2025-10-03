package com.weaponhouse.enhance.session;
import net.minecraft.util.math.BlockPos;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class ClientSessionManager {
    private static ClientSessionManager instance;
    private final Map<BlockPos, ClientBlockSession> sessions = new ConcurrentHashMap<>();
    private ClientSessionManager() {}
    public static synchronized ClientSessionManager getInstance() {
        if (instance == null) {
            instance = new ClientSessionManager();
        }
        return instance;
    }
    public void updateSession(BlockPos blockPos, Set<UUID> playerUUIDs) {
        ClientBlockSession session = sessions.computeIfAbsent(blockPos,
                pos -> new ClientBlockSession(pos)
        );
        session.updatePlayers(playerUUIDs);
    }
    public void removePlayerFromSession(BlockPos blockPos, UUID disconnectedPlayer) {
        ClientBlockSession session = sessions.get(blockPos);
        if (session != null) {
            session.removePlayer(disconnectedPlayer);
            if (session.getPlayers().isEmpty()) {
                sessions.remove(blockPos);
            }
        }
    }
    public ClientBlockSession getSession(BlockPos blockPos) {
        return sessions.get(blockPos);
    }
}