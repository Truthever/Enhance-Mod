package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import java.util.Collection;
public class ForceKillCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("forcekill")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> {
                                    try {
                                        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
                                        return executeForceKill(context.getSource(), entities);
                                    } catch (CommandSyntaxException e) {
                                        context.getSource().sendErrorMessage(
                                                new StringTextComponent("命令执行出错: " + e.getMessage())
                                                        .mergeStyle(TextFormatting.RED));
                                        return 0;
                                    }
                                }))
                        .executes(context -> {
                            context.getSource().sendErrorMessage(
                                    new StringTextComponent("请指定目标实体！用法: /forcekill <目标选择器>")
                                            .mergeStyle(TextFormatting.RED));
                            return 0;
                        })
        );
    }
    private static int executeForceKill(CommandSource source, Collection<? extends Entity> targets) {
        if (targets.isEmpty()) {
            source.sendFeedback(
                    new StringTextComponent("未找到指定实体")
                            .mergeStyle(TextFormatting.YELLOW),
                    true);
            return 0;
        }
        int removedCount = 0;
        for (Entity entity : targets) {
            ResourceLocation entityType = entity.getType().getRegistryName();
            if (entityType != null && "minecraft:player".equals(entityType.toString())) {
                source.sendFeedback(
                        new StringTextComponent("跳过玩家: " + entity.getDisplayName().getString())
                                .mergeStyle(TextFormatting.GRAY),
                        true);
                continue;
            }
            try {
                String entityTypeStr = entityType != null ? entityType.toString() : "未知实体";
                String entityPos = String.format("(%.1f, %.1f, %.1f)",
                        entity.getPosX(), entity.getPosY(), entity.getPosZ());
                String dimensionStr = "未知维度";
                if (entity.world != null) {
                    ResourceLocation dimensionKey = entity.world.getDimensionKey().getLocation();
                    dimensionStr = dimensionKey.toString();
                }
                entity.remove();
                removedCount++;
                if (com.weaponhouse.enhance.Enhance.DEBUG_MODE) {
                    com.weaponhouse.enhance.Enhance.debug(
                            String.format("强制移除实体: %s 位置: %s 维度: %s",
                                    entityTypeStr, entityPos, dimensionStr));
                }
            } catch (Exception e) {
                source.sendErrorMessage(
                        new StringTextComponent("移除实体失败: " + e.getMessage())
                                .mergeStyle(TextFormatting.RED));
                if (com.weaponhouse.enhance.Enhance.DEBUG_MODE) {
                    com.weaponhouse.enhance.Enhance.LOGGER.error("移除实体时发生错误", e);
                }
            }
        }
        String feedbackMessage;
        TextFormatting color = TextFormatting.GREEN;
        if (removedCount == 0) {
            feedbackMessage = "没有实体被移除。";
            color = TextFormatting.YELLOW;
        } else if (removedCount == 1) {
            feedbackMessage = "已强制移除 1 个实体。";
        } else {
            feedbackMessage = String.format("已强制移除 %d 个实体。", removedCount);
        }
        source.sendFeedback(
                new StringTextComponent(feedbackMessage)
                        .mergeStyle(color),
                true);

        return removedCount;
    }
}