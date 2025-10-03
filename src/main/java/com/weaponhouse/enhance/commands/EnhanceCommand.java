package com.weaponhouse.enhance.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
public class EnhanceCommand {
    public static final String BUFF_TAG = "WeaponHouseBuffs";
    public static final String ENHANCE_LEVEL_TAG = "enhance_level";
    public static final int MAX_ENHANCE_LEVEL = 6;
    private static final String KEY_BUFF_NAME = "buff.enhance.%s";
    private static final String KEY_FEEDBACK = "command.enhance.feedback";
    private static class BuffInfo {
        final String buffId;
        final int minLevel;
        final int maxLevel;
        BuffInfo(String buffId, int minLevel, int maxLevel) {
            this.buffId = buffId;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }
    }
    private static final Map<String, BuffInfo> BUFF_CONFIG = new HashMap<>();
    static {
        BUFF_CONFIG.put("vampire", new BuffInfo("vampire", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("rob", new BuffInfo("rob", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("megaforce", new BuffInfo("megaforce", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("thunder", new BuffInfo("thunder", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("frost", new BuffInfo("frost", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("ricochet", new BuffInfo("ricochet", 1, 5));
        BUFF_CONFIG.put("harmony", new BuffInfo("harmony", 1, 20));
        BUFF_CONFIG.put("curse", new BuffInfo("curse", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("life", new BuffInfo("life", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("thorns", new BuffInfo("thorns", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("aura", new BuffInfo("aura", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("hunger", new BuffInfo("hunger", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("attack", new BuffInfo("attack", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("displacement", new BuffInfo("displacement", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("death_bomb", new BuffInfo("death_bomb", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("tracking", new BuffInfo("tracking", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("unyielding", new BuffInfo("unyielding", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("summon", new BuffInfo("summon", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("phantom", new BuffInfo("phantom", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put("photosynthesis", new BuffInfo("photosynthesis", 1, Integer.MAX_VALUE));
        BUFF_CONFIG.put(ENHANCE_LEVEL_TAG, new BuffInfo("enhance_level", 1, MAX_ENHANCE_LEVEL));
        BUFF_CONFIG.put("fasting", new BuffInfo("fasting", 1, Integer.MAX_VALUE));
    }
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSource> enhanceCommand = Commands.literal("enhance")
                .requires(source -> source.hasPermissionLevel(2));
        com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSource, net.minecraft.command.arguments.EntitySelector> targetsArgument = Commands.argument("targets", EntityArgument.entities());
        for (Map.Entry<String, BuffInfo> entry : BUFF_CONFIG.entrySet()) {
            String buffType = entry.getKey();
            BuffInfo info = entry.getValue();
            targetsArgument.then(Commands.literal(buffType)
                    .then(Commands.argument("level", IntegerArgumentType.integer(info.minLevel, info.maxLevel))
                            .executes(ctx -> applyBuff(
                                    ctx.getSource(),
                                    EntityArgument.getEntities(ctx, "targets"),
                                    buffType,
                                    IntegerArgumentType.getInteger(ctx, "level"),
                                    info
                            ))
                    )
            );
        }
        enhanceCommand.then(targetsArgument);
        dispatcher.register(enhanceCommand);
    }
    private static int applyBuff(
            CommandSource source,
            Collection<? extends Entity> targets,
            String buffType,
            int level,
            BuffInfo info
    ) {
        String displayName = new TranslationTextComponent(String.format(KEY_BUFF_NAME, info.buffId)).getString();
        targets.stream()
                .filter(e -> e instanceof LivingEntity)
                .forEach(entity -> {
                    CompoundNBT data = ((LivingEntity) entity).getPersistentData();
                    CompoundNBT buffs = data.contains(BUFF_TAG) ? data.getCompound(BUFF_TAG) : new CompoundNBT();
                    buffs.putInt(buffType, level);
                    data.put(BUFF_TAG, buffs);
                    LivingEntity livingEntity = (LivingEntity) entity;

                    if ("life".equals(buffType)) {
                        LifeHandler.applyLifeBuff(livingEntity);
                    }
                    if ("attack".equals(buffType)) {
                        AttackHandler.applyAttackBuff(livingEntity);
                    }
                    TranslationTextComponent feedback = new TranslationTextComponent(
                            KEY_FEEDBACK,
                            entity.getName().getString(),
                            displayName,
                            level
                    );
                    feedback.mergeStyle(TextFormatting.GOLD)
                            .appendSibling(new StringTextComponent(" ")
                                    .mergeStyle(TextFormatting.LIGHT_PURPLE)
                                    .appendSibling(new StringTextComponent(displayName + " Lv." + level)
                                            .mergeStyle(TextFormatting.AQUA)
                                    )
                            );
                    source.sendFeedback(feedback, true);
                });
        return targets.size();
    }
}
