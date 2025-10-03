package com.weaponhouse.enhance.commands;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class DelayedCommandExecutor {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static void handleDelayedCommand(int delayInSeconds, String command, CommandSource source) {
        scheduler.schedule(() -> {
            MinecraftServer server = source.getServer();
            server.getCommandManager().handleCommand(source, command);
        }, delayInSeconds, TimeUnit.SECONDS);
    }
}
