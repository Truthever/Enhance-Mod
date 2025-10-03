package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.command.Commands;
import net.minecraft.nbt.CompoundNBT;
import java.util.Collection;
public class SetAttackCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("setattack")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("operation", StringArgumentType.word())
                                        .then(Commands.argument("damage", FloatArgumentType.floatArg(0.0f))
                                                .executes(context -> {
                                                    return executeSetAttack(
                                                            context.getSource(),
                                                            EntityArgument.getEntities(context, "targets"),
                                                            StringArgumentType.getString(context, "operation"),
                                                            FloatArgumentType.getFloat(context, "damage")
                                                    );
                                                })
                                        )
                                )
                        )
        );
    }
    private static int executeSetAttack(CommandSource source, Collection<? extends Entity> targets, String operation, float damage) {
        int count = 0;
        String lowerOperation = operation.toLowerCase();
        if (!isValidOperation(lowerOperation)) {
            String errorMsg = I18n.format("command.setattack.unknown_operation", operation);
            source.sendErrorMessage(new StringTextComponent(errorMsg));
            return 0;
        }
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                double currentDamage = livingEntity.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
                double newDamage = calculateNewDamage(currentDamage, lowerOperation, damage);
                livingEntity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newDamage);
                CompoundNBT nbt = livingEntity.getPersistentData();
                nbt.putFloat("BaseAttackDamage", (float) newDamage);
                String displayName = I18n.format("command.setattack.display_name", newDamage);
                livingEntity.setCustomName(new StringTextComponent(displayName));
                livingEntity.setCustomNameVisible(true);
                count++;
            }
        }
        if (count == 0) {
            source.sendErrorMessage(new StringTextComponent(I18n.format("command.setattack.no_valid_entities")));
        } else {
            String successMsg = getLocalizedSuccessMessage(lowerOperation, count, damage);
            source.sendFeedback(new StringTextComponent(successMsg), true);
        }
        return count;
    }
    private static boolean isValidOperation(String operation) {
        return operation.equals("set") || operation.equals("increase") || operation.equals("reduce");
    }
    private static double calculateNewDamage(double current, String operation, float value) {
        switch (operation) {
            case "set":
                return value;
            case "increase":
                return current + value;
            case "reduce":
                return Math.max(0.0, current - value);
            default:
                return current;
        }
    }
    private static String getLocalizedSuccessMessage(String operation, int count, float value) {
        switch (operation) {
            case "set":
                return I18n.format("command.setattack.success.set", count, value);
            case "increase":
                return I18n.format("command.setattack.success.increase", count, value);
            case "reduce":
                return I18n.format("command.setattack.success.reduce", count, value);
            default:
                return I18n.format("command.setattack.success.default", count, operation, value);
        }
    }
}