package com.weaponhouse.enhance.commands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
public class DimensionListCommand {
    private static final String KEY_NO_DIMENSIONS = "command.dimensionlist.no_dimensions";
    private static final String KEY_TITLE = "command.dimensionlist.title";
    private static final String KEY_DIMENSION_INFO = "command.dimensionlist.info";
    private static final String KEY_FOOTER = "command.dimensionlist.footer";
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("dimensionlist")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> listDimensions(context.getSource()))
        );
    }
    private static int listDimensions(CommandSource source) {
        List<ServerWorld> worlds = new ArrayList<>((Collection) ServerLifecycleHooks.getCurrentServer().getWorlds());
        if (worlds.isEmpty()) {
            source.sendFeedback(
                    new TranslationTextComponent(KEY_NO_DIMENSIONS)
                            .mergeStyle(TextFormatting.RED),
                    false
            );
            return 0;
        }
        worlds.sort(Comparator.comparing(w -> w.getDimensionKey().getLocation().toString()));
        TranslationTextComponent title = (TranslationTextComponent) new TranslationTextComponent(KEY_TITLE)
                .mergeStyle(TextFormatting.GOLD, TextFormatting.BOLD);
        source.sendFeedback(title, false);
        for (ServerWorld world : worlds) {
            RegistryKey<World> dimensionKey = world.getDimensionKey();
            String dimensionId = dimensionKey.getLocation().toString();
            int loadedChunks = world.getChunkProvider().getLoadedChunkCount();
            int playerCount = world.getPlayers().size();
            TranslationTextComponent dimensionInfo = (TranslationTextComponent) new TranslationTextComponent(
                    KEY_DIMENSION_INFO,
                    dimensionId,
                    loadedChunks,
                    playerCount
            ).mergeStyle(TextFormatting.WHITE);
            source.sendFeedback(dimensionInfo, false);
        }
        TranslationTextComponent footer = (TranslationTextComponent) new TranslationTextComponent(KEY_FOOTER)
                .mergeStyle(TextFormatting.GRAY);
        source.sendFeedback(footer, false);
        return worlds.size();
    }}