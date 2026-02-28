package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.Objects;

public class SetAttackCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("setattack")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("operation", StringArgumentType.word())
                                        .then(Commands.argument("damage", FloatArgumentType.floatArg(0.0f))
                                                .executes(context -> executeSetAttack(
                                                        context.getSource(),
                                                        EntityArgument.getEntities(context, "targets"),
                                                        StringArgumentType.getString(context, "operation"),
                                                        FloatArgumentType.getFloat(context, "damage")
                                                ))
                                        )
                                )
                        )
        );
    }
    private static int executeSetAttack(CommandSource source, Collection<? extends Entity> targets, String operation, float damage) {
        int count = 0;
        String lowerOperation = operation.toLowerCase();
        if (!isValidOperation(lowerOperation)) {
            TranslationTextComponent errorMsg = new TranslationTextComponent("command.setattack.unknown_operation", operation);
            source.sendErrorMessage(errorMsg);
            return 0;
        }
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                double currentDamage = Objects.requireNonNull(livingEntity.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue();
                double newDamage = calculateNewDamage(currentDamage, lowerOperation, damage);
                Objects.requireNonNull(livingEntity.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(newDamage);
                CompoundNBT nbt = livingEntity.getPersistentData();
                nbt.putFloat("BaseAttackDamage", (float) newDamage);
                TranslationTextComponent displayName = new TranslationTextComponent("command.setattack.display_name", newDamage);
                livingEntity.setCustomName(displayName);
                livingEntity.setCustomNameVisible(true);

                count++;
            }
        }
        if (count == 0) {
            source.sendErrorMessage(new TranslationTextComponent("command.setattack.no_valid_entities"));
        } else {
            TranslationTextComponent successMsg = getLocalizedSuccessMessage(lowerOperation, count, damage);
            source.sendFeedback(successMsg, true);
        }
        return count;
    }
    private static boolean isValidOperation(String operation) {
        return operation.equals("set") || operation.equals("increase") || operation.equals("reduce");
    }
    private static double calculateNewDamage(double current, String operation, float value) {
        switch (operation) {
            case "set": return value;
            case "increase": return current + value;
            case "reduce": return Math.max(0.0, current - value);
            default: return current;
        }
    }
    private static TranslationTextComponent getLocalizedSuccessMessage(String operation, int count, float value) {
        switch (operation) {
            case "set": return new TranslationTextComponent("command.setattack.success.set", count, value);
            case "increase": return new TranslationTextComponent("command.setattack.success.increase", count, value);
            case "reduce": return new TranslationTextComponent("command.setattack.success.reduce", count, value);
            default: return new TranslationTextComponent("command.setattack.success.default", count, operation, value);
        }
    }
}