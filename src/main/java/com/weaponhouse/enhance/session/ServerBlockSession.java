package com.weaponhouse.enhance.session;
import net.minecraft.util.math.BlockPos;
import java.util.*;
public class ServerBlockSession {
    private final BlockPos pos;
    private final Set<UUID> players = new HashSet<>();
    private final Map<UUID, Boolean> isInteracting = new HashMap<>();
    public ServerBlockSession(BlockPos pos) {
        this.pos = pos;
    }
    public boolean addPlayer(UUID playerUUID) {
        if (players.size() < 2) {
            players.add(playerUUID);
            isInteracting.put(playerUUID, false);
            return true;
        }
        return false;
    }
    public void removePlayer(UUID playerUUID) {
        if (players.remove(playerUUID)) {
            isInteracting.remove(playerUUID);
        }
    }
    public boolean isEmpty() {
        return players.isEmpty();
    }
    public void setInteracting(UUID playerUUID, boolean interacting) {
        if (players.contains(playerUUID)) {
            isInteracting.put(playerUUID, interacting);
        }
    }
    public Map<UUID, Boolean> getInteractionStates() {
        return new HashMap<>(isInteracting);
    }
    public BlockPos getPos() {
        return pos;
    }
    public Set<UUID> getPlayers() {
        return new HashSet<>(players);
    }
}