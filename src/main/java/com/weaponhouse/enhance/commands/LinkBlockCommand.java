package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.network.BlockLinkEffectPacket;
import java.util.*;
public class LinkBlockCommand {
    private static final Map<UUID, Map<BlockPos, List<LinkInfo>>> blockLinks = new HashMap<>();
    private static final Map<String, ColorInfo> COLOR_MAP = new LinkedHashMap<>();
    static {
        COLOR_MAP.put("red", new ColorInfo(0xFF5555, "§c红色"));
        COLOR_MAP.put("green", new ColorInfo(0x55FF55, "§a绿色"));
        COLOR_MAP.put("blue", new ColorInfo(0x5555FF, "§9蓝色"));
        COLOR_MAP.put("yellow", new ColorInfo(0xFFFF55, "§e黄色"));
        COLOR_MAP.put("purple", new ColorInfo(0xFF55FF, "§d紫色"));
        COLOR_MAP.put("cyan", new ColorInfo(0x55FFFF, "§b青色"));
        COLOR_MAP.put("white", new ColorInfo(0xFFFFFF, "§f白色"));
        COLOR_MAP.put("orange", new ColorInfo(0xFFAA00, "§6橙色"));
        COLOR_MAP.put("pink", new ColorInfo(0xFF99CC, "§d粉色"));
        COLOR_MAP.put("lime", new ColorInfo(0x99FF66, "§a亮绿色"));
        COLOR_MAP.put("aqua", new ColorInfo(0x33CCCC, "§b水蓝色"));
        COLOR_MAP.put("magenta", new ColorInfo(0xFF33CC, "§d洋红色"));
        COLOR_MAP.put("brown", new ColorInfo(0x996633, "§6棕色"));
        COLOR_MAP.put("gray", new ColorInfo(0x999999, "§7灰色"));
        COLOR_MAP.put("light_gray", new ColorInfo(0xCCCCCC, "§7浅灰色"));
        COLOR_MAP.put("dark_red", new ColorInfo(0x990000, "§4暗红色"));
        COLOR_MAP.put("dark_green", new ColorInfo(0x009900, "§2暗绿色"));
        COLOR_MAP.put("dark_blue", new ColorInfo(0x000099, "§1暗蓝色"));
        COLOR_MAP.put("dark_purple", new ColorInfo(0x660066, "§5暗紫色"));
        COLOR_MAP.put("gold", new ColorInfo(0xFFD700, "§6金色"));
        COLOR_MAP.put("emerald", new ColorInfo(0x00FF99, "§a翡翠绿"));
        COLOR_MAP.put("ruby", new ColorInfo(0xFF0066, "§c红宝石色"));
        COLOR_MAP.put("sapphire", new ColorInfo(0x0066FF, "§9蓝宝石色"));
        COLOR_MAP.put("amethyst", new ColorInfo(0x9966FF, "§5紫水晶色"));
    }
    static class ColorInfo {
        public final int rgb;
        public final String displayName;
        public ColorInfo(int rgb, String displayName) {
            this.rgb = rgb;
            this.displayName = displayName;
        }
    }
    static class LinkInfo {
        public final BlockPos targetPos;
        public final int color;
        public final String colorName;
        public final String colorKey;
        public LinkInfo(BlockPos targetPos, int color, String colorName, String colorKey) {
            this.targetPos = targetPos;
            this.color = color;
            this.colorName = colorName;
            this.colorKey = colorKey;
        }
    }
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("linkblock")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                                .executes(context -> addLink(
                                                        context.getSource(),
                                                        BlockPosArgument.getBlockPos(context, "pos1"),
                                                        BlockPosArgument.getBlockPos(context, "pos2"),
                                                        null
                                                ))
                                                .then(Commands.argument("color", StringArgumentType.word())
                                                        .suggests((context, builder) -> {
                                                            for (String color : COLOR_MAP.keySet()) {
                                                                builder.suggest(color);
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(context -> addLink(
                                                                context.getSource(),
                                                                BlockPosArgument.getBlockPos(context, "pos1"),
                                                                BlockPosArgument.getBlockPos(context, "pos2"),
                                                                StringArgumentType.getString(context, "color")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                                .executes(context -> removeLink(
                                                        context.getSource(),
                                                        BlockPosArgument.getBlockPos(context, "pos1"),
                                                        BlockPosArgument.getBlockPos(context, "pos2")
                                                ))
                                        )
                                )
                                .then(Commands.literal("all")
                                        .executes(context -> clearLinks(context.getSource()))
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(context -> listLinks(context.getSource()))
                        )
                        .then(Commands.literal("search")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> searchLinks(
                                                context.getSource(),
                                                BlockPosArgument.getBlockPos(context, "pos")
                                        ))
                                )
                        )
                        .then(Commands.literal("colors")
                                .executes(context -> showColors(context.getSource()))
                        )
                        .executes(context -> showUsage(context.getSource()))
        );
    }
    private static int addLink(CommandSource source, BlockPos pos1, BlockPos pos2, String colorStr) throws CommandSyntaxException {
        World world = source.getWorld();
        ServerPlayerEntity player = source.asPlayer();
        UUID playerId = player.getUniqueID();
        if (isValidPosition(world, pos1) || isValidPosition(world, pos2)) {
            source.sendErrorMessage(new StringTextComponent("§c错误: 坐标必须在同一个世界且有效"));
            return 0;
        }
        if (pos1.equals(pos2)) {
            source.sendErrorMessage(new StringTextComponent("§c错误: 不能链接同一个坐标"));
            return 0;
        }
        Map<BlockPos, List<LinkInfo>> playerLinks = blockLinks.computeIfAbsent(playerId, k -> new HashMap<>());
        if (playerLinks.containsKey(pos1)) {
            for (LinkInfo existingLink : playerLinks.get(pos1)) {
                if (existingLink.targetPos.equals(pos2)) {
                    source.sendErrorMessage(new StringTextComponent("§c错误: 这两个坐标之间已经存在链接"));
                    return 0;
                }
            }
        }
        int color;
        String colorName;
        String colorKey;
        if (colorStr != null) {
            String lowerColor = colorStr.toLowerCase();
            if (!COLOR_MAP.containsKey(lowerColor)) {
                source.sendErrorMessage(new StringTextComponent("§c错误: 未知颜色 '" + colorStr + "'。使用 /linkblock colors 查看可用颜色"));
                return 0;
            }
            ColorInfo colorInfo = COLOR_MAP.get(lowerColor);
            color = colorInfo.rgb;
            colorName = colorInfo.displayName;
            colorKey = lowerColor;
        } else {
            List<String> colorKeys = new ArrayList<>(COLOR_MAP.keySet());
            String randomColorKey = colorKeys.get(new Random().nextInt(colorKeys.size()));
            ColorInfo colorInfo = COLOR_MAP.get(randomColorKey);
            color = colorInfo.rgb;
            colorName = colorInfo.displayName;
            colorKey = randomColorKey;
        }
        playerLinks.computeIfAbsent(pos1, k -> new ArrayList<>()).add(new LinkInfo(pos2, color, colorName, colorKey));
        source.sendFeedback(new StringTextComponent(
                String.format("§a✓ 成功创建链接: §e(%d, %d, %d) §a→ §e(%d, %d, %d) §7[%s§7]",
                        pos1.getX(), pos1.getY(), pos1.getZ(),
                        pos2.getX(), pos2.getY(), pos2.getZ(),
                        colorName)
        ), true);
        BlockLinkEffectPacket packet = new BlockLinkEffectPacket(pos1, pos2, true, color);
        Enhance.sendToClient(packet, player);
        return 1;
    }
    private static int removeLink(CommandSource source, BlockPos pos1, BlockPos pos2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.asPlayer();
        UUID playerId = player.getUniqueID();
        boolean removed = removeLink(playerId, pos1, pos2);
        if (removed) {
            source.sendFeedback(new StringTextComponent(
                    String.format("§a✓ 成功移除链接: §e(%d, %d, %d) §a↔ §e(%d, %d, %d)",
                            pos1.getX(), pos1.getY(), pos1.getZ(),
                            pos2.getX(), pos2.getY(), pos2.getZ())
            ), true);
            sendUnlinkPacket(player, pos1, pos2);
            return 1;
        } else {
            source.sendErrorMessage(new StringTextComponent("§c错误: 未找到指定的链接"));
            return 0;
        }
    }
    private static int searchLinks(CommandSource source, BlockPos pos) throws CommandSyntaxException {
        ServerPlayerEntity player = source.asPlayer();
        UUID playerId = player.getUniqueID();
        Map<BlockPos, List<LinkInfo>> playerLinks = blockLinks.get(playerId);
        if (playerLinks == null || playerLinks.isEmpty()) {
            source.sendFeedback(new StringTextComponent("§7你没有创建任何链接"), true);
            return 0;
        }
        int foundCount = 0;
        source.sendFeedback(new StringTextComponent("§6=== 与坐标 (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") 相关的链接 ==="), true);
        if (playerLinks.containsKey(pos)) {
            for (LinkInfo link : playerLinks.get(pos)) {
                source.sendFeedback(new StringTextComponent(
                        String.format("§7- §e(%d, %d, %d) §7→ §e(%d, %d, %d) §7[%s§7]",
                                pos.getX(), pos.getY(), pos.getZ(),
                                link.targetPos.getX(), link.targetPos.getY(), link.targetPos.getZ(),
                                link.colorName)
                ), true);
                foundCount++;
            }
        }
        for (Map.Entry<BlockPos, List<LinkInfo>> entry : playerLinks.entrySet()) {
            for (LinkInfo link : entry.getValue()) {
                if (link.targetPos.equals(pos)) {
                    source.sendFeedback(new StringTextComponent(
                            String.format("§7- §e(%d, %d, %d) §7→ §e(%d, %d, %d) §7[%s§7]",
                                    entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ(),
                                    pos.getX(), pos.getY(), pos.getZ(),
                                    link.colorName)
                    ), true);
                    foundCount++;
                }
            }
        }
        if (foundCount == 0) {
            source.sendFeedback(new StringTextComponent("§7未找到与该坐标相关的链接"), true);
        } else {
            source.sendFeedback(new StringTextComponent("§7总计找到: " + foundCount + " 个链接"), true);
        }
        return foundCount;
    }
    private static int listLinks(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.asPlayer();
        UUID playerId = player.getUniqueID();
        Map<BlockPos, List<LinkInfo>> playerLinks = blockLinks.get(playerId);
        if (playerLinks == null || playerLinks.isEmpty()) {
            source.sendFeedback(new StringTextComponent("§7你没有创建任何链接"), true);
            return 0;
        }
        int totalLinks = 0;
        source.sendFeedback(new StringTextComponent("§6=== 你的方块链接列表 ==="), true);
        for (Map.Entry<BlockPos, List<LinkInfo>> entry : playerLinks.entrySet()) {
            BlockPos from = entry.getKey();
            for (LinkInfo link : entry.getValue()) {
                source.sendFeedback(new StringTextComponent(
                        String.format("§7- §e(%d, %d, %d) §7→ §e(%d, %d, %d) §7[%s§7]",
                                from.getX(), from.getY(), from.getZ(),
                                link.targetPos.getX(), link.targetPos.getY(), link.targetPos.getZ(),
                                link.colorName)
                ), true);
                totalLinks++;
            }
        }
        source.sendFeedback(new StringTextComponent("§7总计: " + totalLinks + " 个链接"), true);
        return totalLinks;
    }
    private static int clearLinks(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.asPlayer();
        UUID playerId = player.getUniqueID();
        Map<BlockPos, List<LinkInfo>> playerLinks = blockLinks.get(playerId);
        if (playerLinks == null || playerLinks.isEmpty()) {
            source.sendFeedback(new StringTextComponent("§7你没有创建任何链接"), true);
            return 0;
        }
        int removedCount = playerLinks.values().stream().mapToInt(List::size).sum();
        blockLinks.remove(playerId);
        source.sendFeedback(new StringTextComponent("§a✓ 已清除所有链接，共 " + removedCount + " 个"), true);
        return removedCount;
    }
    private static int showColors(CommandSource source) {
        source.sendFeedback(new StringTextComponent("§6=== 可用链接颜色 ==="), true);
        int count = 0;
        StringBuilder line = new StringBuilder();
        for (Map.Entry<String, ColorInfo> entry : COLOR_MAP.entrySet()) {
            String colorEntry = entry.getValue().displayName + "§7(" + entry.getKey() + ")";
            if (count > 0) {
                line.append("§7, ");
            }
            line.append(colorEntry);
            count++;
            if (count % 4 == 0) {
                source.sendFeedback(new StringTextComponent("§7" + line), true);
                line = new StringBuilder();
            }
        }
        if (line.length() > 0) {
            source.sendFeedback(new StringTextComponent("§7" + line), true);
        }
        source.sendFeedback(new StringTextComponent("§7用法: /linkblock add <pos1> <pos2> [颜色]"), true);
        return 1;
    }
    private static int showUsage(CommandSource source) {
        source.sendFeedback(new StringTextComponent("§6=== 方块链接指令用法 ==="), true);
        source.sendFeedback(new StringTextComponent("§7/linkblock add <pos1> <pos2> [颜色] §f- 创建链接"), true);
        source.sendFeedback(new StringTextComponent("§7/linkblock remove <pos1> <pos2> §f- 移除特定链接"), true);
        source.sendFeedback(new StringTextComponent("§7/linkblock remove all §f- 移除所有链接"), true);
        source.sendFeedback(new StringTextComponent("§7/linkblock list §f- 列出所有链接"), true);
        source.sendFeedback(new StringTextComponent("§7/linkblock search <pos> §f- 搜索与坐标相关的链接"), true);
        source.sendFeedback(new StringTextComponent("§7/linkblock colors §f- 显示可用颜色"), true);
        return 1;
    }
    private static boolean isValidPosition(World world, BlockPos pos) {
        return pos.getY() < 0 || pos.getY() > world.getHeight();
    }
    public static boolean removeLink(UUID playerId, BlockPos pos1, BlockPos pos2) {
        Map<BlockPos, List<LinkInfo>> playerLinks = blockLinks.get(playerId);
        if (playerLinks != null) {
            if (playerLinks.containsKey(pos1)) {
                List<LinkInfo> links = playerLinks.get(pos1);
                boolean removed = links.removeIf(link -> link.targetPos.equals(pos2));
                if (links.isEmpty()) {
                    playerLinks.remove(pos1);
                }
                if (removed) return true;
            }
            if (playerLinks.containsKey(pos2)) {
                List<LinkInfo> links = playerLinks.get(pos2);
                boolean removed = links.removeIf(link -> link.targetPos.equals(pos1));
                if (links.isEmpty()) {
                    playerLinks.remove(pos2);
                }
                return removed;
            }
        }
        return false;
    }
    public static void sendUnlinkPacket(ServerPlayerEntity player, BlockPos pos1, BlockPos pos2) {
        BlockLinkEffectPacket packet = new BlockLinkEffectPacket(pos1, pos2, false, 0);
        Enhance.sendToClient(packet, player);
    }
}