package com.weaponhouse.enhance.data;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import java.util.*;
public class PortalDataManager extends WorldSavedData {
    private static final String DATA_NAME = "enhance_portal_data";
    private final Set<BlockPos> activePortals = new HashSet<>();
    private final Map<BlockPos, Set<PortalLineData>> portalLines = new HashMap<>();
    public PortalDataManager() {
        super(DATA_NAME);
    }
    @Override
    public void read(CompoundNBT nbt) {
        activePortals.clear();
        portalLines.clear();
        ListNBT portalsList = nbt.getList("activePortals", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < portalsList.size(); i++) {
            CompoundNBT portalTag = portalsList.getCompound(i);
            BlockPos pos = BlockPos.fromLong(portalTag.getLong("pos"));
            activePortals.add(pos);
        }
        ListNBT centersList = nbt.getList("portalLines", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < centersList.size(); i++) {
            CompoundNBT centerTag = centersList.getCompound(i);
            BlockPos centerPos = BlockPos.fromLong(centerTag.getLong("centerPos"));
            Set<PortalLineData> lines = new HashSet<>();
            ListNBT linesList = centerTag.getList("lines", Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < linesList.size(); j++) {
                CompoundNBT lineTag = linesList.getCompound(j);
                BlockPos start = BlockPos.fromLong(lineTag.getLong("start"));
                BlockPos end = BlockPos.fromLong(lineTag.getLong("end"));
                int color = lineTag.getInt("color");
                String debugName = lineTag.getString("debugName");

                lines.add(new PortalLineData(start, end, color, debugName));
            }
            portalLines.put(centerPos, lines);
        }
    }
    @Override
    public CompoundNBT write(CompoundNBT compound) {
        ListNBT portalsList = new ListNBT();
        for (BlockPos pos : activePortals) {
            CompoundNBT portalTag = new CompoundNBT();
            portalTag.putLong("pos", pos.toLong());
            portalsList.add(portalTag);
        }
        compound.put("activePortals", portalsList);
        ListNBT centersList = new ListNBT();
        for (Map.Entry<BlockPos, Set<PortalLineData>> entry : portalLines.entrySet()) {
            CompoundNBT centerTag = new CompoundNBT();
            centerTag.putLong("centerPos", entry.getKey().toLong());
            ListNBT linesList = new ListNBT();
            for (PortalLineData line : entry.getValue()) {
                CompoundNBT lineTag = new CompoundNBT();
                lineTag.putLong("start", line.start.toLong());
                lineTag.putLong("end", line.end.toLong());
                lineTag.putInt("color", line.color);
                lineTag.putString("debugName", line.debugName);
                linesList.add(lineTag);
            }
            centerTag.put("lines", linesList);
            centersList.add(centerTag);
        }
        compound.put("portalLines", centersList);
        return compound;
    }
    public static PortalDataManager get(ServerWorld world) {
        return world.getSavedData().getOrCreate(PortalDataManager::new, DATA_NAME);
    }
    public void addActivePortal(BlockPos centerPos) {
        activePortals.add(centerPos);
        markDirty();
    }
    public void removeActivePortal(BlockPos centerPos) {
        activePortals.remove(centerPos);
        portalLines.remove(centerPos);
        markDirty();
    }
    public void addPortalLine(BlockPos centerPos, PortalLineData line) {
        portalLines.computeIfAbsent(centerPos, k -> new HashSet<>()).add(line);
        markDirty();
    }
    public void removePortalLine(BlockPos centerPos, PortalLineData line) {
        Set<PortalLineData> lines = portalLines.get(centerPos);
        if (lines != null) {
            lines.remove(line);
            if (lines.isEmpty()) {
                portalLines.remove(centerPos);
            }
            markDirty();
        }
    }
    public void removeAllLines(BlockPos centerPos) {
        portalLines.remove(centerPos);
        markDirty();
    }
    public Set<BlockPos> getActivePortals() {
        return new HashSet<>(activePortals);
    }
    public Set<PortalLineData> getPortalLines(BlockPos centerPos) {
        return new HashSet<>(portalLines.getOrDefault(centerPos, new HashSet<>()));
    }
    public Map<BlockPos, Set<PortalLineData>> getAllPortalLines() {
        return new HashMap<>(portalLines);
    }
    public boolean isPortalActive(BlockPos centerPos) {
        return activePortals.contains(centerPos);
    }
    public static class PortalLineData {
        public final BlockPos start;
        public final BlockPos end;
        public final int color;
        public final String debugName;
        public PortalLineData(BlockPos start, BlockPos end, int color, String debugName) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.debugName = debugName;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PortalLineData that = (PortalLineData) obj;
            return color == that.color &&
                    Objects.equals(start, that.start) &&
                    Objects.equals(end, that.end);
        }
        @Override
        public int hashCode() {
            return Objects.hash(start, end, color);
        }
    }
}