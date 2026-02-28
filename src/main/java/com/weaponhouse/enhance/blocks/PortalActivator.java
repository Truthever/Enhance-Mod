package com.weaponhouse.enhance.blocks;

import com.weaponhouse.enhance.data.PortalDataManager;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import java.util.*;
public class PortalActivator {
    static final BlockPos[] PORTAL_FRAME_POSITIONS = {
            new BlockPos(-2, 0, -2),
            new BlockPos(2, 0, -2),
            new BlockPos(0, 0, 0),
            new BlockPos(-2, 0, 2),
            new BlockPos(2, 0, 2)
    };
    private static final int[] OUTER_FRAME_INDICES = {0, 1, 3, 4};
    private static final Set<String> REQUIRED_OUTER_PILL_TYPES = new HashSet<>();
    static {
        REQUIRED_OUTER_PILL_TYPES.add("ATTACK");
        REQUIRED_OUTER_PILL_TYPES.add("LIFE");
        REQUIRED_OUTER_PILL_TYPES.add("SPEED");
        REQUIRED_OUTER_PILL_TYPES.add("DEFENSE");
    }
    private static final BlockPos[] PORTAL_BLOCK_POSITIONS = {
            new BlockPos(1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, -1)
    };
    private static final PortalLine[] PORTAL_LINES = {
            new PortalLine(new BlockPos(-2, 0, -2), new BlockPos(2, 0, -2), 0xFF33CC, "外圈上边-magenta"),
            new PortalLine(new BlockPos(2, 0, -2), new BlockPos(2, 0, 2), 0x33CCCC, "外圈右边-aqua"),
            new PortalLine(new BlockPos(-2, 0, 2), new BlockPos(2, 0, 2), 0x99FF66, "外圈下边-lime"),
            new PortalLine(new BlockPos(-2, 0, -2), new BlockPos(-2, 0, 2), 0xFFFF55, "外圈左边-yellow"),
            new PortalLine(new BlockPos(-2, 0, -2), new BlockPos(0, 0, 0), 0x99FF66, "内圈左上-orange"),
            new PortalLine(new BlockPos(2, 0, -2), new BlockPos(0, 0, 0), 0xFFFF55, "内圈右上-purple"),
            new PortalLine(new BlockPos(2, 0, 2), new BlockPos(0, 0, 0), 0x00AAFF, "内圈右下-blue"),
            new PortalLine(new BlockPos(-2, 0, 2), new BlockPos(0, 0, 0), 0xFF00AA, "内圈左下-pink")
    };
    public static void checkAndUpdateLines(World world, BlockPos centerPos) {
        if (world.isRemote) return;
        boolean[] frameActivated = new boolean[PORTAL_FRAME_POSITIONS.length];
        String[] framePillTypes = new String[PORTAL_FRAME_POSITIONS.length];
        for (int i = 0; i < PORTAL_FRAME_POSITIONS.length; i++) {
            BlockPos framePos = centerPos.add(PORTAL_FRAME_POSITIONS[i]);
            FrameCheckResult result = checkPortalFrame(world, framePos);
            frameActivated[i] = result.activated;
            framePillTypes[i] = result.pillType;
        }
        updatePortalLines(world, centerPos, frameActivated);
        checkAndCreatePortal(world, centerPos, frameActivated, framePillTypes);
    }
    private static class FrameCheckResult {
        public boolean activated;
        public String pillType;
        public FrameCheckResult(boolean activated, String pillType) {
            this.activated = activated;
            this.pillType = pillType;
        }
    }
    private static FrameCheckResult checkPortalFrame(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof EnhanceLandPortalFrameBlock) {
            boolean activated = state.get(EnhanceLandPortalFrameBlock.ACTIVATED);
            String pillType = getFramePillType(world, pos);
            return new FrameCheckResult(activated, pillType);
        }
        return new FrameCheckResult(false, null);
    }
    private static String getFramePillType(World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof EnhanceLandPortalFrameTileEntity) {
            EnhanceLandPortalFrameTileEntity frameTE = (EnhanceLandPortalFrameTileEntity) tileEntity;
            return frameTE.getPillType();
        }
        return null;
    }
    private static void updatePortalLines(World world, BlockPos centerPos, boolean[] frameActivated) {
        PortalDataManager dataManager = PortalDataManager.get((ServerWorld) world);
        Set<PortalLine> currentLines = new HashSet<>();
        for (PortalDataManager.PortalLineData lineData : dataManager.getPortalLines(centerPos)) {
            currentLines.add(new PortalLine(
                    lineData.start,
                    lineData.end,
                    lineData.color,
                    lineData.debugName
            ));
        }
        Set<PortalLine> newValidLines = new HashSet<>();
        for (PortalLine line : PORTAL_LINES) {
            int startIndex = getFrameIndex(line.start);
            int endIndex = getFrameIndex(line.end);
            if (startIndex != -1 && endIndex != -1 && frameActivated[startIndex] && frameActivated[endIndex]) {
                newValidLines.add(line);
            }
        }
        Iterator<PortalLine> iterator = currentLines.iterator();
        while (iterator.hasNext()) {
            PortalLine line = iterator.next();
            if (!newValidLines.contains(line)) {
                removeLine(world, centerPos, line);
                iterator.remove();
                dataManager.removePortalLine(centerPos, convertToData(line));
            }
        }
        for (PortalLine line : newValidLines) {
            if (!currentLines.contains(line)) {
                addLine(world, centerPos, line);
                dataManager.addPortalLine(centerPos, convertToData(line));
            }
        }
    }
    private static void checkAndCreatePortal(World world, BlockPos centerPos, boolean[] frameActivated, String[] framePillTypes) {
        PortalDataManager dataManager = PortalDataManager.get((ServerWorld) world);
        boolean canCreatePortal = checkNewPortalConditions(frameActivated, framePillTypes);
        if (canCreatePortal) {
            if (!dataManager.isPortalActive(centerPos)) {
                createPortalBlocks(world, centerPos);
                dataManager.addActivePortal(centerPos);
                world.playSound(null, centerPos,
                        net.minecraft.util.SoundEvents.BLOCK_END_PORTAL_SPAWN,
                        net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            if (dataManager.isPortalActive(centerPos)) {
                removePortalBlocks(world, centerPos);
                dataManager.removeActivePortal(centerPos);
                world.playSound(null, centerPos,
                        net.minecraft.util.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
    }
    private static boolean checkNewPortalConditions(boolean[] frameActivated, String[] framePillTypes) {
        boolean centerCondition = frameActivated[2] && "HARMONY".equals(framePillTypes[2]);
        if (!centerCondition) {
            return false;
        }
        Set<String> foundTypes = new HashSet<>();
        for (int frameIndex : OUTER_FRAME_INDICES) {
            if (frameActivated[frameIndex] && framePillTypes[frameIndex] != null) {
                foundTypes.add(framePillTypes[frameIndex]);
            }
        }
        return foundTypes.containsAll(REQUIRED_OUTER_PILL_TYPES);
    }
    private static void createPortalBlocks(World world, BlockPos centerPos) {
        for (BlockPos relativePos : PORTAL_BLOCK_POSITIONS) {
            BlockPos portalPos = centerPos.add(relativePos);
            world.setBlockState(portalPos, RegistryHandler.ENHANCE_LAND_PORTAL.get().getDefaultState());
        }
    }
    private static void removePortalBlocks(World world, BlockPos centerPos) {
        for (BlockPos relativePos : PORTAL_BLOCK_POSITIONS) {
            BlockPos portalPos = centerPos.add(relativePos);
            if (world.getBlockState(portalPos).getBlock() == RegistryHandler.ENHANCE_LAND_PORTAL.get()) {
                world.setBlockState(portalPos, Blocks.AIR.getDefaultState());
            }
        }
    }
    private static int getFrameIndex(BlockPos relativePos) {
        for (int i = 0; i < PORTAL_FRAME_POSITIONS.length; i++) {
            if (PORTAL_FRAME_POSITIONS[i].equals(relativePos)) {
                return i;
            }
        }
        return -1;
    }
    private static void addLine(World world, BlockPos centerPos, PortalLine line) {
        BlockPos start = centerPos.add(line.start);
        BlockPos end = centerPos.add(line.end);
        com.weaponhouse.enhance.network.BlockLinkEffectPacket packet =
                new com.weaponhouse.enhance.network.BlockLinkEffectPacket(start, end, true, line.color);
        world.getPlayers().forEach(player -> {
            if (player instanceof net.minecraft.entity.player.ServerPlayerEntity) {
                com.weaponhouse.enhance.Enhance.sendToClient(packet, (net.minecraft.entity.player.ServerPlayerEntity) player);
            }
        });
    }
    private static void removeLine(World world, BlockPos centerPos, PortalLine line) {
        BlockPos start = centerPos.add(line.start);
        BlockPos end = centerPos.add(line.end);
        com.weaponhouse.enhance.network.BlockLinkEffectPacket packet =
                new com.weaponhouse.enhance.network.BlockLinkEffectPacket(start, end, false, line.color);
        world.getPlayers().forEach(player -> {
            if (player instanceof net.minecraft.entity.player.ServerPlayerEntity) {
                com.weaponhouse.enhance.Enhance.sendToClient(packet, (net.minecraft.entity.player.ServerPlayerEntity) player);
            }
        });
    }
    public static void removeAllLines(World world, BlockPos centerPos) {
        if (world.isRemote) return;
        PortalDataManager dataManager = PortalDataManager.get((ServerWorld) world);
        for (PortalDataManager.PortalLineData lineData : dataManager.getPortalLines(centerPos)) {
            PortalLine line = new PortalLine(lineData.start, lineData.end, lineData.color, lineData.debugName);
            removeLine(world, centerPos, line);
        }
        dataManager.removeAllLines(centerPos);
        if (dataManager.isPortalActive(centerPos)) {
            removePortalBlocks(world, centerPos);
            dataManager.removeActivePortal(centerPos);
        }
    }
    public static void restoreAllPortals(World world) {
        if (world.isRemote) return;
        PortalDataManager dataManager = PortalDataManager.get((ServerWorld) world);
        for (BlockPos centerPos : dataManager.getActivePortals()) {
            createPortalBlocks(world, centerPos);
        }
        for (Map.Entry<BlockPos, Set<PortalDataManager.PortalLineData>> entry : dataManager.getAllPortalLines().entrySet()) {
            BlockPos centerPos = entry.getKey();
            for (PortalDataManager.PortalLineData lineData : entry.getValue()) {
                PortalLine line = new PortalLine(lineData.start, lineData.end, lineData.color, lineData.debugName);
                addLine(world, centerPos, line);
            }
        }
    }
    private static PortalDataManager.PortalLineData convertToData(PortalLine line) {
        return new PortalDataManager.PortalLineData(line.start, line.end, line.color, line.debugName);
    }
    public static class PortalLine {
        public final BlockPos start;
        public final BlockPos end;
        public final int color;
        public final String debugName;
        public PortalLine(BlockPos start, BlockPos end, int color, String debugName) {
            if (shouldSwap(start, end)) {
                this.start = end;
                this.end = start;
            } else {
                this.start = start;
                this.end = end;
            }
            this.color = color;
            this.debugName = debugName;
        }
        private boolean shouldSwap(BlockPos a, BlockPos b) {
            if (a.getX() != b.getX()) return a.getX() > b.getX();
            if (a.getZ() != b.getZ()) return a.getZ() > b.getZ();
            return a.getY() > b.getY();
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PortalLine that = (PortalLine) obj;
            return color == that.color &&
                    Objects.equals(start, that.start) &&
                    Objects.equals(end, that.end);
        }
        @Override
        public int hashCode() {
            return Objects.hash(start, end, color);
        }
        @Override
        public String toString() {
            return debugName + " [" + start + "->" + end + "]";
        }
    }
}