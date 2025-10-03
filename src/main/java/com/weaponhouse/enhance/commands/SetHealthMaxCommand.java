package com.weaponhouse.enhance.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import java.util.Collection;
public class SetHealthMaxCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("sethealthmax")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("operation", StringArgumentType.word())
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(1.0f))
                                                .executes(context -> {
                                                    Collection<? extends LivingEntity> targets = (Collection<? extends LivingEntity>) EntityArgument.getEntities(context, "targets");
                                                    String operation = StringArgumentType.getString(context, "operation");
                                                    float amount = FloatArgumentType.getFloat(context, "amount");
                                                    return executeSetHealthMax(context.getSource(), targets, operation, amount);
                                                })
                                        )
                                )
                        )
        );
    }
    private static int executeSetHealthMax(CommandSource source, Collection<? extends LivingEntity> targets, String operation, float amount) {
        int count = 0;
        String lowerOperation = operation.toLowerCase();
        if (!isValidOperation(lowerOperation)) {
            String errorMsg = I18n.format("command.sethealthmax.unknown_operation", operation);
            source.sendErrorMessage(new StringTextComponent(errorMsg));
            return 0;
        }
        for (LivingEntity entity : targets) {
            double currentMaxHealth = entity.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            double newMaxHealth = calculateNewHealth(currentMaxHealth, lowerOperation, amount);
            entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
            if (entity.getHealth() > newMaxHealth) {
                entity.setHealth((float) newMaxHealth);
            }
            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                CompoundNBT nbt = player.getPersistentData();
                nbt.putFloat("BaseMaxHealth", (float) newMaxHealth);
                String playerMsg = getPlayerMessage(lowerOperation, newMaxHealth);
                player.sendMessage(new StringTextComponent(playerMsg), player.getUniqueID());
            }
            count++;
        }
        if (count == 0) {
            source.sendErrorMessage(new StringTextComponent(I18n.format("command.sethealthmax.no_valid_entities")));
        } else {
            String successMsg = getSuccessMessage(lowerOperation, count, amount);
            source.sendFeedback(new StringTextComponent(successMsg), true);
        }
        return count;
    }
    private static boolean isValidOperation(String operation) {
        return operation.equals("set") || operation.equals("increase") || operation.equals("reduce");
    }
    private static double calculateNewHealth(double current, String operation, float amount) {
        switch (operation) {
            case "set":
                return amount;
            case "increase":
                return current + amount;
            case "reduce":
                return Math.max(1.0, current - amount);
            default:
                return current;
        }
    }
    private static String getPlayerMessage(String operation, double newHealth) {
        switch (operation) {
            case "set":
                return I18n.format("command.sethealthmax.player.set", newHealth);
            case "increase":
                return I18n.format("command.sethealthmax.player.increase", newHealth);
            case "reduce":
                return I18n.format("command.sethealthmax.player.reduce", newHealth);
            default:
                return I18n.format("command.sethealthmax.player.default", newHealth);
        }
    }
    private static String getSuccessMessage(String operation, int count, float amount) {
        switch (operation) {
            case "set":
                return I18n.format("command.sethealthmax.success.set", count, amount);
            case "increase":
                return I18n.format("command.sethealthmax.success.increase", count, amount);
            case "reduce":
                return I18n.format("command.sethealthmax.success.reduce", count, amount);
            default:
                return I18n.format("command.sethealthmax.success.default", count, operation, amount);
        }
    }
}
