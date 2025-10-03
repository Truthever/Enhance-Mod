package com.weaponhouse.enhance.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.effects.HealingReductionEffect;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Collection;
import java.util.stream.Collectors;
@Mod.EventBusSubscriber
public class CommandRegistrationHandler {
    @SubscribeEvent
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
                Commands.literal("health")
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

                                                return healTargets(context.getSource(), targets, amount);
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
    private static int healTargets(CommandSource source, Collection<? extends LivingEntity> targets, int amount) {
        for (LivingEntity target : targets) {
            float adjustedHealing = HealingReductionEffect.handleHealing(target, amount);
            if (adjustedHealing > 0) {
                float newHealth = Math.min(target.getHealth() + adjustedHealing, target.getMaxHealth());
                target.setHealth(newHealth);
            }
        }
        source.sendFeedback(new StringTextComponent("Healed " + amount + " health to " + targets.size() + " entities."), true);
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
