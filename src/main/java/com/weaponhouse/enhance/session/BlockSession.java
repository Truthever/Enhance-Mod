package com.weaponhouse.enhance.session;
import com.weaponhouse.enhance.Enhance;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
public class BlockSession {
    private final BlockPos blockPos;
    private final Set<UUID> players = new HashSet<>();
    private long lastInteractionTime;
    private static final long SESSION_TIMEOUT_MS = 3 * 60 * 1000;
    public BlockSession(BlockPos blockPos, UUID initialPlayer) {
        this.blockPos = blockPos;
        this.players.add(initialPlayer);
        this.lastInteractionTime = System.currentTimeMillis();
    }
    public boolean joinSession(UUID playerUUID) {
        if (!players.contains(playerUUID) && players.size() < 2) {
            players.add(playerUUID);
            lastInteractionTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }
    public boolean leaveSession(UUID playerUUID) {
        if (players.contains(playerUUID)) {
            players.remove(playerUUID);
            lastInteractionTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }
    public boolean isEmpty() {
        return players.isEmpty();
    }
    public boolean isExpired() {
        long now = System.currentTimeMillis();
        boolean expired = now - lastInteractionTime > SESSION_TIMEOUT_MS;
        if (expired) {
        }
        return expired;
    }
}
