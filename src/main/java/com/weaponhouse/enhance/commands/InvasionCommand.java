package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.weaponhouse.enhance.invasion.InvasionConfig;
import com.weaponhouse.enhance.invasion.InvasionHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
public class InvasionCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("invasion")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            showStatus(ctx.getSource());
                            return 1;
                        })
                        .then(Commands.literal("start")
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayerEntity) {
                                        ServerPlayerEntity player = (ServerPlayerEntity) ctx.getSource().getEntity();
                                        ServerWorld world = (ServerWorld) player.world;
                                        InvasionHandler.forceStartInvasion(world);
                                        ctx.getSource().sendFeedback(
                                                new StringTextComponent("§a已强制开启增幅入侵"),
                                                true
                                        );
                                    } else {
                                        ServerWorld world = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
                                        if (world != null) {
                                            InvasionHandler.forceStartInvasion(world);
                                            ctx.getSource().sendFeedback(
                                                    new StringTextComponent("§a已强制开启增幅入侵"),
                                                    true
                                            );
                                        }
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("stop")
                                .executes(ctx -> {
                                    InvasionHandler.stopInvasionForTesting();
                                    ctx.getSource().sendFeedback(
                                            new StringTextComponent("§c已立即结束增幅入侵"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    InvasionHandler.resetInvasionCycle();
                                    ctx.getSource().sendFeedback(
                                            new StringTextComponent("§a已重置入侵周期"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("pause")
                                .executes(ctx -> {
                                    InvasionConfig.setEnabled(false);
                                    ctx.getSource().sendFeedback(
                                            new StringTextComponent("§e已暂停入侵周期"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("resume")
                                .executes(ctx -> {
                                    InvasionConfig.setEnabled(true);
                                    ctx.getSource().sendFeedback(
                                            new StringTextComponent("§a已恢复入侵周期"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("setinterval")
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> {
                                            int days = IntegerArgumentType.getInteger(ctx, "days");
                                            InvasionConfig.setInvasionIntervalDays(days);
                                            ctx.getSource().sendFeedback(
                                                    new StringTextComponent(String.format("§a入侵间隔已设置为 §e%d天", days)),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("setduration")
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 30))
                                        .executes(ctx -> {
                                            int days = IntegerArgumentType.getInteger(ctx, "days");
                                            InvasionConfig.setInvasionDurationDays(days);
                                            ctx.getSource().sendFeedback(
                                                    new StringTextComponent(String.format("§a入侵持续时间已设置为 §e%d天", days)),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("trigger")
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayerEntity) {
                                        ServerPlayerEntity player = (ServerPlayerEntity) ctx.getSource().getEntity();
                                        InvasionHandler.checkEnhancementUnlock(player);
                                        ctx.getSource().sendFeedback(
                                                new StringTextComponent("§a已强制触发入侵检查（模拟获得增幅石成就）"),
                                                true
                                        );
                                    } else {
                                        ctx.getSource().sendFeedback(
                                                new StringTextComponent("§c只有玩家可以执行此命令"),
                                                false
                                        );
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    InvasionConfig.reloadConfig();
                                    ctx.getSource().sendFeedback(
                                            new StringTextComponent("§a入侵配置已重新加载"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("addmonster")
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String monsterType = StringArgumentType.getString(ctx, "type");
                                            InvasionConfig.addInvasionMonsterType(monsterType);
                                            ctx.getSource().sendFeedback(
                                                    new StringTextComponent(String.format("§a已添加怪物类型: §e%s", monsterType)),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("removemonster")
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String monsterType = StringArgumentType.getString(ctx, "type");
                                            InvasionConfig.removeInvasionMonsterType(monsterType);
                                            ctx.getSource().sendFeedback(
                                                    new StringTextComponent(String.format("§c已移除怪物类型: §e%s", monsterType)),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("listmonsters")
                                .executes(ctx -> {
                                    List<String> monsterTypes = InvasionConfig.getInvasionMonsterTypes();
                                    StringTextComponent message = new StringTextComponent("§6入侵怪物类型列表 (§e" + monsterTypes.size() + "§6种):");
                                    message.mergeStyle(TextFormatting.GOLD);

                                    ctx.getSource().sendFeedback(message, true);

                                    for (int i = 0; i < monsterTypes.size(); i++) {
                                        ctx.getSource().sendFeedback(
                                                new StringTextComponent(String.format("  §7%d. §f%s", i + 1, monsterTypes.get(i))),
                                                false
                                        );
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("testspawn")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> {
                                            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity) {
                                                ServerPlayerEntity player = (ServerPlayerEntity) ctx.getSource().getEntity();
                                                int count = IntegerArgumentType.getInteger(ctx, "count");

                                                InvasionHandler.spawnTestMonsters(player, count);

                                                ctx.getSource().sendFeedback(
                                                        new StringTextComponent(String.format("§a已生成 §e%d§a 只测试怪物", count)),
                                                        true
                                                );
                                            } else {
                                                ctx.getSource().sendFeedback(
                                                        new StringTextComponent("§c只有玩家可以执行此命令"),
                                                        false
                                                );
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("setmonstercount")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                                .executes(ctx -> {
                                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                                    int count = IntegerArgumentType.getInteger(ctx, "count");

                                                    InvasionConfig.setMonsterCountByLevel(level, count);

                                                    ctx.getSource().sendFeedback(
                                                            new StringTextComponent(String.format("§a玩家等级 §e%d§a 的怪物数量已设置为 §e%d", level, count)),
                                                            true
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    int removedCount = clearAllInvasionMonsters(ctx.getSource());
                                    ctx.getSource().sendFeedback(
                                            new StringTextComponent(String.format("§a已清理 §e%d§a 只入侵怪物", removedCount)),
                                            true
                                    );
                                    return removedCount;
                                })
                                .then(Commands.literal("player")
                                        .executes(ctx -> {
                                            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity) {
                                                ServerPlayerEntity player = (ServerPlayerEntity) ctx.getSource().getEntity();
                                                int removed = InvasionHandler.clearPlayerInvasionMonsters(player);
                                                ctx.getSource().sendFeedback(
                                                        new StringTextComponent(String.format("§a已清理 §e%d§a 只属于你的入侵怪物", removed)),
                                                        true
                                                );
                                                return removed;
                                            } else {
                                                ctx.getSource().sendFeedback(
                                                        new StringTextComponent("§c只有玩家可以执行此命令"),
                                                        false
                                                );
                                                return 0;
                                            }
                                        })
                                )
                        )
                        .then(Commands.literal("help")
                                .executes(ctx -> {
                                    showHelp(ctx.getSource());
                                    return 1;
                                })
                        )

        );
    }
    private static void showStatus(CommandSource source) {
        StringTextComponent statusMessage = new StringTextComponent("§6§l增幅入侵状态:");
        statusMessage.mergeStyle(TextFormatting.GOLD);
        source.sendFeedback(statusMessage, false);
        ITextComponent statusComp = InvasionHandler.getInvasionStatusComponent();
        source.sendFeedback(
                new StringTextComponent("  §e当前状态: §f").appendSibling(statusComp),
                false
        );
        source.sendFeedback(new StringTextComponent("  §e入侵间隔: §f" + InvasionConfig.getInvasionIntervalDays() + "天"), false);
        source.sendFeedback(new StringTextComponent("  §e入侵持续时间: §f" + InvasionConfig.getInvasionDurationDays() + "天"), false);
        source.sendFeedback(new StringTextComponent("  §e怪物类型数量: §f" + InvasionConfig.getInvasionMonsterTypes().size() + "种"), false);
        source.sendFeedback(new StringTextComponent("  §e系统启用: §f" + (InvasionConfig.isEnabled() ? "§a是" : "§c否")), false);
        source.sendFeedback(new StringTextComponent("  §e调试模式: §f" + (InvasionConfig.isDebugMode() ? "§a开启" : "§c关闭")), false);
        source.sendFeedback(new StringTextComponent("  §e各等级怪物数量:"), false);
        for (int i = 1; i <= 6; i++) {
            int count = InvasionConfig.getMonsterCountByLevel(i);
            source.sendFeedback(new StringTextComponent(String.format("    §7等级%d: §f%d只", i, count)), false);
        }
    }
    private static int clearAllInvasionMonsters(CommandSource source) {
        int removedCount;
        ServerWorld world;
        if (source.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
            world = (ServerWorld) player.world;
        } else {
            world = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        }
        if (world == null) {
            source.sendFeedback(new StringTextComponent("§c无法获取世界"), false);
            return 0;
        }
        try {
            String command = "kill @e[tag=invasion_monster]";
            world.getServer().getCommandManager().handleCommand(
                    world.getServer().getCommandSource().withFeedbackDisabled().withPermissionLevel(4),
                    command
            );
            List<net.minecraft.entity.Entity> entities = world.getEntitiesWithinAABB(
                    net.minecraft.entity.Entity.class,
                    new net.minecraft.util.math.AxisAlignedBB(
                            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
                    ),
                    entity -> entity.getTags().contains("invasion_monster") ||
                            entity.getPersistentData().getBoolean("enhance_invasion")
            );
            removedCount = entities.size();
            source.sendFeedback(new StringTextComponent("§a使用命令清理了所有入侵怪物标签"), true);
        } catch (Exception e) {
            source.sendFeedback(new StringTextComponent("§c命令清理失败，使用遍历方式: " + e.getMessage()), false);
            removedCount = clearAllInvasionMonstersInternal(world);
        }
        return removedCount;
    }
    private static int clearAllInvasionMonstersInternal(ServerWorld world) {
        int removedCount = 0;
        List<net.minecraft.entity.Entity> entities = world.getEntitiesWithinAABB(
                net.minecraft.entity.Entity.class,
                new net.minecraft.util.math.AxisAlignedBB(
                        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
                )
        );
        for (net.minecraft.entity.Entity entity : entities) {
            if (entity.getTags().contains("invasion_monster") ||
                    entity.getPersistentData().getBoolean("enhance_invasion")) {
                try {
                    entity.getPersistentData().putBoolean("invasion_removed", true);
                    if (entity instanceof net.minecraft.entity.LivingEntity) {
                        com.weaponhouse.enhance.invasion.InvasionDropHandler.safelyRemoveInvasionMonster(
                                (net.minecraft.entity.LivingEntity) entity);
                    } else {
                        entity.remove();
                    }
                    removedCount++;
                } catch (Exception e) {
                    com.weaponhouse.enhance.Enhance.LOGGER.error("[增幅入侵] 清除怪物时出错: {}", e.getMessage());
                }
            }
        }
        if (removedCount > 0) {
            com.weaponhouse.enhance.Enhance.LOGGER.info("[增幅入侵] 已清除 {} 只入侵怪物", removedCount);
        }
        return removedCount;
    }
    private static void showHelp(CommandSource source) {
        StringTextComponent helpMessage = new StringTextComponent("§6§l增幅入侵命令帮助:");
        helpMessage.mergeStyle(TextFormatting.GOLD);
        source.sendFeedback(helpMessage, false);
        source.sendFeedback(new StringTextComponent("  §e/invasion §f- 显示入侵状态"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion start §f- 立即开始入侵"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion stop §f- 立即结束入侵"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion reset §f- 重新开始入侵周期"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion pause §f- 暂停入侵周期"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion resume §f- 恢复入侵周期"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion trigger §f- 强制触发入侵检查"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion reload §f- 重新加载配置"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion setinterval <天数> §f- 设置入侵间隔"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion setduration <天数> §f- 设置入侵持续时间"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion addmonster <类型> §f- 添加怪物类型"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion removemonster <类型> §f- 移除怪物类型"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion listmonsters §f- 查看怪物类型列表"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion testspawn <数量> §f- 测试怪物生成"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion setmonstercount <等级> <数量> §f- 设置怪物数量"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion clear §f- 清理所有入侵怪物"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion clear player §f- 清理当前玩家的入侵怪物"), false);
        source.sendFeedback(new StringTextComponent("  §e/invasion help §f- 显示此帮助"), false);
        source.sendFeedback(new StringTextComponent("§7怪物类型示例: minecraft:zombie, minecraft:skeleton, minecraft:enderman"), false);
    }
}