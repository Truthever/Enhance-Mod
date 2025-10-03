package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.command.Commands;
import net.minecraft.nbt.CompoundNBT;
import java.util.Collection;
import java.util.Objects;
public class NBTEntityCommand {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("nbtentity")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> showBaseAttributes(
                                        context.getSource(),
                                        EntityArgument.getEntities(context, "targets")
                                ))
                        )
        );
    }
    private static int showBaseAttributes(CommandSource source, Collection<? extends Entity> targets) {
        int count = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                String entityName = entity.getName().getString();
                double baseMaxHealth = Objects.requireNonNull(livingEntity.getAttribute(Attributes.MAX_HEALTH)).getBaseValue();
                double baseAttackDamage = Objects.requireNonNull(livingEntity.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue();
                double baseMovementSpeed = Objects.requireNonNull(livingEntity.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue();
                CompoundNBT nbt = livingEntity.getPersistentData();
                float naturalResistance = nbt.contains("naturalArmor") ? nbt.getFloat("naturalArmor") : 0.0f;
                float dynamicArmor = nbt.contains("dynamicArmor") ? nbt.getFloat("dynamicArmor") : 0.0f;
                float baseSpeed = nbt.contains("speed") ? nbt.getFloat("speed") : (float) baseMovementSpeed;
                StringBuilder buffInfo = new StringBuilder();
                if (nbt.contains(BUFF_TAG)) {
                    CompoundNBT buffs = nbt.getCompound(BUFF_TAG);
                    buffs.keySet().forEach(buffKey -> {
                        String localizedBuffName = I18n.format("buff.enhance." + buffKey);
                        int buffLevel = buffs.getInt(buffKey);
                        buffInfo.append(localizedBuffName).append(" Lv.").append(buffLevel).append("; ");
                    });
                }
                String finalBuffInfo = buffInfo.length() > 0 ? buffInfo.toString().trim() : I18n.format("command.nbtentity.no_buffs");
                double dynamicMaxHealth = livingEntity.getMaxHealth();
                double dynamicAttackDamage = Objects.requireNonNull(livingEntity.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
                float dynamicSpeed = (float) Objects.requireNonNull(livingEntity.getAttribute(Attributes.MOVEMENT_SPEED)).getValue();
                StringBuilder message = new StringBuilder(
                        I18n.format("command.nbtentity.attributes", entityName) + "\n"
                );
                message.append(" - ").append(I18n.format("command.nbtentity.base_max_health", baseMaxHealth)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.dynamic_max_health", dynamicMaxHealth)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.base_attack_damage", baseAttackDamage)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.dynamic_attack_damage", dynamicAttackDamage)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.natural_resistance", naturalResistance)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.dynamic_armor", dynamicArmor)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.buffs", finalBuffInfo)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.base_speed", baseSpeed)).append("\n");
                message.append(" - ").append(I18n.format("command.nbtentity.dynamic_speed", dynamicSpeed)).append("\n");
                if (entity instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) entity;
                    float flySpeed = player.abilities.getFlySpeed();
                    message.append("\n - ").append(I18n.format("command.nbtentity.fly_speed", flySpeed));
                }
                source.sendFeedback(new StringTextComponent(message.toString()), false);
                count++;
            } else {
                String errorMsg = I18n.format("command.nbtentity.not_living_entity", entity.getName().getString());
                source.sendErrorMessage(new StringTextComponent(errorMsg));
            }
        }
        if (count == 0) {
            source.sendErrorMessage(new StringTextComponent(I18n.format("command.nbtentity.no_valid_entities")));
        } else {
            source.sendFeedback(new StringTextComponent(I18n.format("command.nbtentity.success_count", count)), true);
        }
        return count;
    }
}