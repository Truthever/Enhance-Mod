package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.stream.Collectors;
public class CommandRegistrationHandler {
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("damage")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            try {
                                                Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
                                                Collection<? extends LivingEntity> targets = entities.stream()
                                                        .filter(e -> e instanceof LivingEntity)
                                                        .map(e -> (LivingEntity) e)
                                                        .collect(Collectors.toList());

                                                int amount = IntegerArgumentType.getInteger(context, "amount");

                                                return damageTargets(context.getSource(), targets, amount);
                                            } catch (CommandSyntaxException e) {
                                                context.getSource().sendErrorMessage(new StringTextComponent("Error: " + e.getMessage()));
                                                return 0;
                                            }
                                        }))));
        dispatcher.register(
                Commands.literal("sethealth")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            try {
                                                Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
                                                Collection<? extends LivingEntity> targets = entities.stream()
                                                        .filter(e -> e instanceof LivingEntity)
                                                        .map(e -> (LivingEntity) e)
                                                        .collect(Collectors.toList());

                                                int amount = IntegerArgumentType.getInteger(context, "amount");

                                                return setHealthTargets(context.getSource(), targets, amount);
                                            } catch (CommandSyntaxException e) {
                                                context.getSource().sendErrorMessage(new StringTextComponent("Error: " + e.getMessage()));
                                                return 0;
                                            }
                                        }))));
        EnhanceCommand.register(dispatcher);
        LinkBlockCommand.register(dispatcher);
        EnhanceRemoveCommand.register(dispatcher);
        NextCommand.register(dispatcher);
        SetHealthMaxCommand.register(dispatcher);
        DimensionListCommand.register(dispatcher);
        SetAttackCommand.register(dispatcher);
        SetResistanceCommand.register(dispatcher);
        SetDimensionSpawnCommand.register(dispatcher);
        SetSpeedCommand.register(dispatcher);
        NBTEntityCommand.register(dispatcher);
        SetFlySpeedCommand.register(dispatcher);
        InvasionCommand.register(dispatcher);
        ForceKillCommand.register(dispatcher);
    }
    private static int damageTargets(CommandSource source, Collection<? extends LivingEntity> targets, int amount) {
        for (LivingEntity target : targets) {
            target.attackEntityFrom(DamageSource.GENERIC, 0.01F);
            float currentHealth = target.getHealth();
            float newHealth = Math.max(currentHealth - amount, 0);
            target.setHealth(newHealth);
            if (newHealth == 0) {
                target.onDeath(DamageSource.GENERIC);
            }
        }
        source.sendFeedback(new StringTextComponent("Dealt " + amount + " direct damage to " + targets.size() + " entities."), true);
        return targets.size();
    }
    private static int setHealthTargets(CommandSource source, Collection<? extends LivingEntity> targets, int amount) {
        for (LivingEntity target : targets) {
            float newHealth = Math.min(amount, target.getMaxHealth());
            target.setHealth(newHealth);
        }
        source.sendFeedback(new StringTextComponent("Set health to " + amount + " for " + targets.size() + " entities."), true);
        return targets.size();
    }
}
