package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
public class SetResistanceCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("setresistance")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("operation", StringArgumentType.word())
                                        .then(Commands.argument("resistance", FloatArgumentType.floatArg(0.0f))
                                                .executes(context -> executeSetResistance(
                                                        context.getSource(),
                                                        EntityArgument.getEntities(context, "targets"),
                                                        StringArgumentType.getString(context, "operation"),
                                                        FloatArgumentType.getFloat(context, "resistance")
                                                ))
                                        )
                                )
                        )
        );
    }
    private static int executeSetResistance(CommandSource source, Collection<? extends Entity> targets,
                                            String operation, float resistance) {
        int count = 0;
        String lowerOperation = operation.toLowerCase();
        if (!isValidOperation(lowerOperation)) {
            TranslationTextComponent errorMsg = new TranslationTextComponent("command.setresistance.unknown_operation", operation);
            source.sendErrorMessage(errorMsg);
            return 0;
        }
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                CompoundNBT nbt = livingEntity.getPersistentData();
                float currentNaturalArmor = nbt.getFloat("naturalArmor");
                float newNaturalArmor = calculateNewResistance(currentNaturalArmor, lowerOperation, resistance);
                float otherBonuses = nbt.contains("otherBonuses") ? nbt.getFloat("otherBonuses") : 0.0f;
                float newDynamicArmor = newNaturalArmor + otherBonuses;
                nbt.putFloat("naturalArmor", newNaturalArmor);
                nbt.putFloat("dynamicArmor", newDynamicArmor);
                if (livingEntity instanceof ServerPlayerEntity) {
                    ServerPlayerEntity player = (ServerPlayerEntity) livingEntity;
                    TranslationTextComponent playerMsg = getPlayerMessage(lowerOperation, newNaturalArmor, newDynamicArmor);
                    player.sendMessage(playerMsg, player.getUniqueID());
                }
                count++;
            }
        }
        if (count == 0) {
            source.sendErrorMessage(new TranslationTextComponent("command.setresistance.no_valid_entities"));
        } else {
            TranslationTextComponent successMsg = getSuccessMessage(lowerOperation, count, resistance);
            source.sendFeedback(successMsg, true);
        }
        return count;
    }
    private static boolean isValidOperation(String operation) {
        return operation.equals("set") || operation.equals("increase") || operation.equals("reduce");
    }
    private static float calculateNewResistance(float current, String operation, float value) {
        switch (operation) {
            case "set": return value;
            case "increase": return current + value;
            case "reduce": return Math.max(0.0f, current - value);
            default: return current;
        }
    }
    private static TranslationTextComponent getPlayerMessage(String operation, float newNaturalArmor, float newDynamicArmor) {
        switch (operation) {
            case "set": return new TranslationTextComponent("command.setresistance.player.set", newNaturalArmor, newDynamicArmor);
            case "increase": return new TranslationTextComponent("command.setresistance.player.increase", newNaturalArmor, newDynamicArmor);
            case "reduce": return new TranslationTextComponent("command.setresistance.player.reduce", newNaturalArmor, newDynamicArmor);
            default: return new TranslationTextComponent("command.setresistance.player.default", newNaturalArmor, newDynamicArmor);
        }
    }
    private static TranslationTextComponent getSuccessMessage(String operation, int count, float value) {
        switch (operation) {
            case "set": return new TranslationTextComponent("command.setresistance.success.set", count, value);
            case "increase": return new TranslationTextComponent("command.setresistance.success.increase", count, value);
            case "reduce": return new TranslationTextComponent("command.setresistance.success.reduce", count, value);
            default: return new TranslationTextComponent("command.setresistance.success.default", count, operation, value);
        }
    }
}