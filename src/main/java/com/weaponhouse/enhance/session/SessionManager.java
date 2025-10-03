package com.weaponhouse.enhance.session;
import com.weaponhouse.enhance.Enhance;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class SessionManager {
    private static SessionManager instance;
    private final Map<BlockPos, BlockSession> sessions = new ConcurrentHashMap<>();
    private SessionManager() {}
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    public void leaveSession(BlockPos blockPos, UUID playerUUID) {
        BlockSession session = sessions.get(blockPos);
        if (session != null) {
            session.leaveSession(playerUUID);
            if (session.isEmpty()) {
                sessions.remove(blockPos);
            }
        }
        cleanupExpiredSessions();
    }
    public BlockSession getSession(BlockPos blockPos) {
        return sessions.get(blockPos);
    }
    public void cleanupExpiredSessions() {
        List<BlockPos> expiredPositions = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredPositions.add(entry.getKey());
            }
        }
        expiredPositions.forEach(sessions::remove);
    }
}
