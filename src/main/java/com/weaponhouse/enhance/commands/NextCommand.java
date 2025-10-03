package com.weaponhouse.enhance.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class NextCommand {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("next")
                        .then(Commands.argument("time", FloatArgumentType.floatArg(0.1f))
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            float time = FloatArgumentType.getFloat(context, "time");
                                            String command = StringArgumentType.getString(context, "command");
                                            CommandSource source = context.getSource();
                                            handleDelayedCommand(time, command, source);
                                            source.sendFeedback(new StringTextComponent("Command '" + command + "' will run in " + time + " seconds!")
                                                    .mergeStyle(TextFormatting.GOLD), true);
                                            return 1;
                                        }))));
    }
    private static void handleDelayedCommand(float delayInSeconds, String command, CommandSource source) {
        long delayInMillis = (long) (delayInSeconds * 1000);
        scheduler.schedule(() -> {
            source.getServer().getCommandManager().handleCommand(source, command);
        }, delayInMillis, TimeUnit.MILLISECONDS);
    }
}
