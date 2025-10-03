package com.weaponhouse.enhance.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.command.Commands;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
public class SetFlySpeedCommand {
    private static String lastModifiedPlayer = null;
    private static float lastModifiedSpeed = 0.0f;
    private static Field flySpeedField = null;
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("setflyspeed")
                .requires(source -> source.hasPermissionLevel(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0.0f, 1.0f))
                                .executes(context -> {
                                    return setFlySpeed(
                                            context.getSource(),
                                            EntityArgument.getPlayers(context, "targets"),
                                            FloatArgumentType.getFloat(context, "speed")
                                    );
                                })
                        )
                )
        );
    }
    private static int setFlySpeed(CommandSource source, Collection<? extends Entity> targets, float speed) {
        int count = 0;
        int failedCount = 0;
        Set<String> allInfo = new HashSet<>();
        for (Entity entity : targets) {
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                PlayerAbilities abilities = player.abilities;
                try {
                    float originalFlySpeed = abilities.getFlySpeed();
                    if (flySpeedField == null) {
                        findFlySpeedField(abilities);
                    }
                    if (flySpeedField != null) {
                        flySpeedField.setAccessible(true);
                        flySpeedField.setFloat(abilities, speed);
                    } else {
                        callSetFlySpeedMethod(abilities, speed);
                    }
                    player.sendPlayerAbilities();
                    float newFlySpeed = abilities.getFlySpeed();
                    if (Math.abs(newFlySpeed - speed) < 0.001f) {
                        String successSingle = I18n.format(
                                "command.setflyspeed.success.single",
                                player.getName().getString(),
                                speed
                        );
                        source.sendFeedback(new StringTextComponent(successSingle), true);
                        count++;
                        lastModifiedPlayer = player.getName().getString();
                        lastModifiedSpeed = speed;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    failedCount++;
                }
            }
        }
        if (count > 0) {
            String successBatch = I18n.format("command.setflyspeed.success.batch", count);
            source.sendFeedback(new StringTextComponent(successBatch), true);
        }
        if (failedCount > 0) {
            String failBatch = I18n.format("command.setflyspeed.fail.batch", failedCount);
            source.sendErrorMessage(new StringTextComponent(failBatch));
        }
        if (count == 0 && failedCount == 0) {
            String noPlayers = I18n.format("command.setflyspeed.no_players");
            source.sendErrorMessage(new StringTextComponent(noPlayers));
        }
        return count;
    }

    private static void findFlySpeedField(PlayerAbilities abilities) {
        try {
            flySpeedField = findFieldByDefaultValue(abilities, 0.05f);
            if (flySpeedField == null) {
                String[] possibleFieldNames = {
                        "flySpeed", "field_75097_g", "field_75096_f", "d", "flySpeedMultiplier"
                };
                for (String fieldName : possibleFieldNames) {
                    try {
                        flySpeedField = abilities.getClass().getDeclaredField(fieldName);
                        break;
                    } catch (NoSuchFieldException e) {
                    }
                }
            }
            if (flySpeedField == null) {
                for (Field field : abilities.getClass().getDeclaredFields()) {
                    if (field.getType() == float.class) {
                        flySpeedField = field;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static Field findFieldByDefaultValue(PlayerAbilities abilities, float defaultValue) {
        try {
            for (Field field : abilities.getClass().getDeclaredFields()) {
                if (field.getType() == float.class) {
                    field.setAccessible(true);
                    float value = field.getFloat(abilities);
                    if (Math.abs(value - defaultValue) < 0.001f) {
                        return field;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private static void callSetFlySpeedMethod(PlayerAbilities abilities, float speed) {
        try {
            Method setFlySpeed = abilities.getClass().getDeclaredMethod("setFlySpeed", float.class);
            setFlySpeed.setAccessible(true);
            setFlySpeed.invoke(abilities, speed);
        } catch (NoSuchMethodException e) {
            try {
                Method setFlySpeed = abilities.getClass().getDeclaredMethod("func_75092_a", float.class);
                setFlySpeed.setAccessible(true);
                setFlySpeed.invoke(abilities, speed);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
