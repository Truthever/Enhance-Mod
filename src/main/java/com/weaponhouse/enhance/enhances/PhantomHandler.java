package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Random;
@Mod.EventBusSubscriber(modid = "enhance")
public class PhantomHandler {
    private static final Random RANDOM = new Random();
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String PHANTOM_TAG = "phantom";
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.world.isRemote) return;
        if (!hasPhantomBuff(entity)) return;
        int phantomLevel = getPhantomLevel(entity);
        int dodgeProbability = phantomLevel * 5;
        DamageSource source = event.getSource();
        if (shouldIgnoreDamage(source)) {
            return;
        }
        if (RANDOM.nextInt(100) < dodgeProbability) {
            event.setCanceled(true);
            spawnDodgeParticles((ServerWorld) entity.world, entity);
            sendDodgeMessage(entity);
        }
    }
    private static boolean hasPhantomBuff(LivingEntity entity) {
        if (entity.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
            return buffs.contains(PHANTOM_TAG) && buffs.getInt(PHANTOM_TAG) > 0;
        }
        return false;
    }
    private static int getPhantomLevel(LivingEntity entity) {
        if (entity.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
            return buffs.getInt(PHANTOM_TAG);
        }
        return 0;
    }
    private static boolean shouldIgnoreDamage(DamageSource source) {
        return source.isFireDamage() ||
                source.isExplosion() ||
                source.isMagicDamage() ||
                source == DamageSource.OUT_OF_WORLD ||
                source == DamageSource.FALL ||
                isCustomBloodSacrificeDamage(source) ||
                source == DamageSource.DROWN ||
                source == DamageSource.STARVE ||
                source == DamageSource.WITHER ||
                source == DamageSource.ANVIL ||
                source == DamageSource.FLY_INTO_WALL ||
                EnhanceStrikeHandler.isEnhanceStrikeDamage(source);
    }
    private static boolean isCustomBloodSacrificeDamage(DamageSource source) {
        return source.getDamageType().equals("blood_sacrifice") || source.isMagicDamage();
    }
    private static void spawnDodgeParticles(ServerWorld serverWorld, LivingEntity entity) {
        RedstoneParticleData purpleParticle = new RedstoneParticleData(0.5F, 0.0F, 0.5F, 1.0F);
        serverWorld.spawnParticle(
                purpleParticle,
                entity.getPosX(), entity.getPosYHeight(0.5), entity.getPosZ(),
                20,
                0.5, 1.0, 0.5,
                0.05
        );
        serverWorld.playSound(
                null,
                entity.getPosition(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.NEUTRAL,
                0.5F,
                1.5F
        );
    }
    private static void sendDodgeMessage(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            player.sendMessage(
                    new TranslationTextComponent("enhance.buff.phantom.dodge")
                            .mergeStyle(TextFormatting.DARK_PURPLE),
                    player.getUniqueID()
            );
        }
    }
}