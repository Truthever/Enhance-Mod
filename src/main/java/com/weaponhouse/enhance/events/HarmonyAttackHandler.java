package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class HarmonyAttackHandler {
    private static final int EFFECT_DURATION = 30 * 20;
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getTrueSource() == null || !(event.getSource().getTrueSource() instanceof LivingEntity)) {
            return;
        }
        LivingEntity attacker = (LivingEntity) event.getSource().getTrueSource();
        LivingEntity target = event.getEntityLiving();
        Biome.Category attackerBiome = getAttackerBiomeCategory(attacker);
        if (attackerBiome == Biome.Category.THEEND) {
            handleEndDefenseReduction(attacker, target);
        } else if (attackerBiome == Biome.Category.SWAMP) {
            handleSwampDefenseReduction(attacker, target);
        }
    }
    private static Biome.Category getAttackerBiomeCategory(LivingEntity attacker) {
        World world = attacker.getEntityWorld();
        BlockPos pos = attacker.getPosition();
        Biome biome = world.getBiome(pos);
        return biome.getCategory();
    }
    private static void handleEndDefenseReduction(LivingEntity attacker, LivingEntity target) {
        int harmonyLevel = getHarmonyLevel(attacker);
        if (harmonyLevel <= 0) return;
        applyDefenseEffect(
                target,
                EffectRegistry.DEFENSEREDUCTION,
                harmonyLevel
        );
    }
    private static void handleSwampDefenseReduction(LivingEntity attacker, LivingEntity target) {
        int harmonyLevel = getHarmonyLevel(attacker);
        if (harmonyLevel <= 0) return;
        applyDefenseEffect(
                target,
                EffectRegistry.SWAMPDEFENSE,
                harmonyLevel
        );
    }
    private static void applyDefenseEffect(LivingEntity target, net.minecraft.potion.Effect effectType, int level) {
        EffectInstance effect = new EffectInstance(
                effectType,
                EFFECT_DURATION,
                level - 1,
                false,
                true,
                true
        );
        target.addPotionEffect(effect);
    }
    private static int getHarmonyLevel(LivingEntity entity) {
        CompoundNBT data = entity.getPersistentData();
        if (!data.contains(EnhanceCommand.BUFF_TAG)) {
            return 0;
        }
        CompoundNBT buffs = data.getCompound(EnhanceCommand.BUFF_TAG);
        return buffs.contains("harmony") ? buffs.getInt("harmony") : 0;
    }
}
