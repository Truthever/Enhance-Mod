package com.weaponhouse.enhance.session;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
public class ClientBlockSession {
    private final BlockPos blockPos;
    private final Set<UUID> playerUUIDs = new HashSet<>();
    private long lastUpdateTime;
    public ClientBlockSession(BlockPos blockPos) {
        this.blockPos = blockPos;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    public void updatePlayers(Set<UUID> newPlayers) {
        playerUUIDs.clear();
        playerUUIDs.addAll(newPlayers);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    public void removePlayer(UUID playerUUID) {
        if (playerUUIDs.remove(playerUUID)) {
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }
    public Set<UUID> getPlayers() {
        return new HashSet<>(playerUUIDs);
    }
}