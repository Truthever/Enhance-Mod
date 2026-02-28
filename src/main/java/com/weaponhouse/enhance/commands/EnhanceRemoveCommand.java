package com.weaponhouse.enhance.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
public class EnhanceRemoveCommand {
    private static final Map<String, String> REMOVABLE_BUFFS = new HashMap<>();
    static {
        REMOVABLE_BUFFS.put("vampire", "vampire");
        REMOVABLE_BUFFS.put("rob", "rob");
        REMOVABLE_BUFFS.put("megaforce", "megaforce");
        REMOVABLE_BUFFS.put("thunder", "thunder");
        REMOVABLE_BUFFS.put("frost", "frost");
        REMOVABLE_BUFFS.put("ricochet", "ricochet");
        REMOVABLE_BUFFS.put("harmony", "harmony");
        REMOVABLE_BUFFS.put("curse", "curse");
        REMOVABLE_BUFFS.put("life", "life");
        REMOVABLE_BUFFS.put("thorns", "thorns");
        REMOVABLE_BUFFS.put("aura", "aura");
        REMOVABLE_BUFFS.put("hunger", "hunger");
        REMOVABLE_BUFFS.put("attack", "attack");
        REMOVABLE_BUFFS.put("displacement", "displacement");
        REMOVABLE_BUFFS.put("death_bomb", "death_bomb");
        REMOVABLE_BUFFS.put("tracking", "tracking");
        REMOVABLE_BUFFS.put("unyielding", "unyielding");
        REMOVABLE_BUFFS.put("summon", "summon");
        REMOVABLE_BUFFS.put("phantom", "phantom");
        REMOVABLE_BUFFS.put("photosynthesis", "photosynthesis");
        REMOVABLE_BUFFS.put("enhance_level", "enhance_level");
        REMOVABLE_BUFFS.put("fasting", "fasting");
        REMOVABLE_BUFFS.put("chaos", "chaos");
        REMOVABLE_BUFFS.put("inspiration", "inspiration");
        REMOVABLE_BUFFS.put("annihilation", "annihilation");
        REMOVABLE_BUFFS.put("spirit_shield", "spirit_shield");
        REMOVABLE_BUFFS.put("corrosion", "corrosion");
        REMOVABLE_BUFFS.put("combo", "combo");
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSource> baseCommand = Commands.literal("enhanceremove")
                .requires(source -> source.hasPermissionLevel(2))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> removeAllBuffs(
                                ctx.getSource(),
                                EntityArgument.getEntities(ctx, "targets")
                        )));

        for (Map.Entry<String, String> entry : REMOVABLE_BUFFS.entrySet()) {
            String buffType = entry.getKey();
            String buffLangKey = entry.getValue();
            baseCommand.then(Commands.literal(buffType)
                    .then(Commands.argument("targets", EntityArgument.entities())
                            .executes(ctx -> removeBuff(
                                    ctx.getSource(),
                                    EntityArgument.getEntities(ctx, "targets"),
                                    buffType,
                                    buffLangKey
                            ))
                    )
            );
        }
        dispatcher.register(baseCommand);
    }
    private static int removeAllBuffs(CommandSource source, Collection<? extends Entity> targets) {
        int removedCount = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                CompoundNBT data = living.getPersistentData();
                if (data.contains(EnhanceCommand.BUFF_TAG)) {
                    CompoundNBT buffs = data.getCompound(EnhanceCommand.BUFF_TAG);
                    int countBefore = buffs.keySet().size();
                    for (String buffType : buffs.keySet()) {
                        handleSpecialBuffRemoval(living, buffType, true);
                        purgeDerivedTags(living, buffType);
                    }
                    buffs = new CompoundNBT();
                    data.put(EnhanceCommand.BUFF_TAG, buffs);
                    removedCount += countBefore;
                    TranslationTextComponent feedback = new TranslationTextComponent(
                            "command.enhanceremove.all_removed",
                            living.getName().getString()
                    );
                    feedback.mergeStyle(TextFormatting.GOLD);
                    source.sendFeedback(feedback, true);
                }
            }
        }
        if (removedCount == 0) {
            source.sendFeedback(
                    new TranslationTextComponent("command.enhanceremove.no_buffs")
                            .mergeStyle(TextFormatting.YELLOW),
                    true
            );
        }
        return removedCount;
    }
    private static int removeBuff(CommandSource source, Collection<? extends Entity> targets, String buffType, String buffLangKey) {
        int removedCount = 0;
        String displayName = new TranslationTextComponent("buff.enhance." + buffLangKey).getString();
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                CompoundNBT data = living.getPersistentData();
                if (data.contains(EnhanceCommand.BUFF_TAG)) {
                    CompoundNBT buffs = data.getCompound(EnhanceCommand.BUFF_TAG);
                    if (buffs.contains(buffType)) {
                        handleSpecialBuffRemoval(living, buffType, false);
                        purgeDerivedTags(living, buffType);
                        buffs.remove(buffType);
                        data.put(EnhanceCommand.BUFF_TAG, buffs);
                        removedCount++;
                        TranslationTextComponent feedback = new TranslationTextComponent(
                                "command.enhanceremove.removed",
                                living.getName().getString(),
                                displayName
                        );
                        feedback.mergeStyle(TextFormatting.GOLD);
                        source.sendFeedback(feedback, true);
                    }
                }
            }
        }
        if (removedCount == 0) {
            source.sendFeedback(
                    new TranslationTextComponent("command.enhanceremove.not_found", displayName)
                            .mergeStyle(TextFormatting.YELLOW),
                    true
            );
        }
        return removedCount;
    }
    private static void handleSpecialBuffRemoval(LivingEntity living, String buffType, boolean isRemoveAll) {
        switch (buffType) {
            case "life":
            case "photosynthesis":
            case "vampire":
            case "curse":
            case "unyielding":
                LifeHandler.restoreOriginalMaxHealth(living, living.getAttribute(Attributes.MAX_HEALTH));
                break;
            case "attack":
                AttackHandler.restoreOriginalAttack(living, living.getAttribute(Attributes.ATTACK_DAMAGE));
                break;
            case "inspiration":
                if (!isRemoveAll) {
                    clearInspirationMarkers(living);
                }
                break;
        }
    }
    private static void purgeDerivedTags(LivingEntity living, String buffType) {
        CompoundNBT data = living.getPersistentData();
        switch (buffType) {
            case "life":
            case "photosynthesis":
            case "vampire":
            case "curse":
            case "unyielding":
                if (data.contains("BaseMaxHealth")) data.remove("BaseMaxHealth");
                break;

            case "attack":
                if (data.contains("BaseAttackDamage")) data.remove("BaseAttackDamage");
                break;

            case "inspiration":
                if (data.contains("InspirationMarker")) data.remove("InspirationMarker");
                break;
            default:
                break;
        }
    }
    private static void clearInspirationMarkers(LivingEntity entity) {
        CompoundNBT data = entity.getPersistentData();
        if (data.contains("InspirationMarker")) {
            data.remove("InspirationMarker");
        }
    }
}