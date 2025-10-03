package com.weaponhouse.enhance.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
public class SetDimensionSpawnCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("setdimensionspawn")
                .then(Commands.argument("dimension", StringArgumentType.string())
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(context -> {
                                                    String dimension = StringArgumentType.getString(context, "dimension");
                                                    double x = DoubleArgumentType.getDouble(context, "x");
                                                    double y = DoubleArgumentType.getDouble(context, "y");
                                                    double z = DoubleArgumentType.getDouble(context, "z");
                                                    ServerPlayerEntity player = context.getSource().asPlayer();
                                                    CompoundNBT playerData = player.getPersistentData();
                                                    CompoundNBT spawnData = new CompoundNBT();
                                                    spawnData.putInt("SpawnX", (int) x);
                                                    spawnData.putInt("SpawnY", (int) y);
                                                    spawnData.putInt("SpawnZ", (int) z);
                                                    spawnData.putString("SpawnDimension", dimension);
                                                    playerData.put("PlayerSpawn", spawnData);
                                                    player.sendMessage(
                                                            new StringTextComponent("重生点已设置为: ")
                                                                    .mergeStyle(TextFormatting.GREEN)
                                                                    .appendSibling(
                                                                            new StringTextComponent(dimension)
                                                                                    .mergeStyle(TextFormatting.YELLOW)
                                                                    )
                                                                    .appendSibling(
                                                                            new StringTextComponent(" (" + (int)x + ", " + (int)y + ", " + (int)z + ")")
                                                                                    .mergeStyle(TextFormatting.GREEN)
                                                                    ),
                                                            player.getUniqueID()
                                                    );
                                                    return 1;
                                                }))))));
    }
}