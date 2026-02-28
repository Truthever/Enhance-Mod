package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.Objects;

public class SetSpeedCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("setspeed")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.05F, 1.0F))
                                .executes(context -> {
                                    Collection<? extends LivingEntity> targets = (Collection<? extends LivingEntity>) EntityArgument.getEntities(context, "targets");
                                    float speedValue = FloatArgumentType.getFloat(context, "value");
                                    CommandSource source = context.getSource();
                                    return setSpeed(source, targets, speedValue);
                                })
                        )
                )
        );
    }
    private static int setSpeed(CommandSource source, Collection<? extends LivingEntity> targets, float speedValue) {
        int count = 0;
        for (LivingEntity target : targets) {
            Objects.requireNonNull(target.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(speedValue);
            target.getPersistentData().putFloat("BaseMovementSpeed", speedValue);
            if (target instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) target;
                TranslationTextComponent playerMsg = new TranslationTextComponent("command.setspeed.player", speedValue);
                player.sendMessage(playerMsg, player.getUniqueID());
            }
            count++;
        }
        if (count == 0) {
            source.sendErrorMessage(new TranslationTextComponent("command.setspeed.no_valid_entities"));
        } else {
            TranslationTextComponent successMsg = new TranslationTextComponent("command.setspeed.success", count, speedValue);
            source.sendFeedback(successMsg, true);
        }
        return count;
    }
}