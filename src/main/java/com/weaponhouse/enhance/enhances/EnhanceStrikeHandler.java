package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.items.EnhanceSwordItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import java.util.Random;
public class EnhanceStrikeHandler {
    private static final Random RANDOM = new Random();
    private static final float DAMAGE_BONUS_PER_LEVEL = 0.10f;
    private static final float TRIGGER_CHANCE = 0.5f;
    public static class EnhanceStrikeDamageSource extends DamageSource {
        private final Entity trueSource;
        public EnhanceStrikeDamageSource(Entity source) {
            super("enhance_strike");
            this.trueSource = source;
            this.setDamageBypassesArmor();
            this.setDamageIsAbsolute();
        }
        @Override
        public Entity getTrueSource() {
            return trueSource;
        }
    }
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote ||
                isEnhanceStrikeDamage(event.getSource())) {
            return;
        }
        LivingEntity target = event.getEntityLiving();
        DamageSource source = event.getSource();
        ServerWorld serverWorld = (ServerWorld) target.world;
        if (!(source.getTrueSource() instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) source.getTrueSource();
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (!(mainHandItem.getItem() instanceof EnhanceSwordItem)) {
            return;
        }
        int enhanceStrikeLevel = EnhanceSwordItem.getEnhanceStrikeLevel(mainHandItem);
        if (enhanceStrikeLevel <= 0) {
            return;
        }
        if (!EnhanceSwordItem.hasEnoughCharge(mainHandItem, EnhanceSwordItem.getStrikeChargeCost())) {
            return;
        }
        if (RANDOM.nextFloat() < TRIGGER_CHANCE) {
            if (!EnhanceSwordItem.consumeCharge(mainHandItem, EnhanceSwordItem.getStrikeChargeCost())) {
                return;
            }
            float originalDamage = event.getAmount();
            float bonusDamage = originalDamage * (1 + enhanceStrikeLevel * DAMAGE_BONUS_PER_LEVEL);
            EnhanceStrikeDamageSource strikeSource = new EnhanceStrikeDamageSource(player);
            boolean damageSuccess = target.attackEntityFrom(strikeSource, bonusDamage);
            if (damageSuccess) {
                triggerEnhanceStrikeEffects(serverWorld, target, player, enhanceStrikeLevel);
            }
        }
    }
    private static void triggerEnhanceStrikeEffects(ServerWorld world, LivingEntity target, PlayerEntity player, int level) {
        double x = target.getPosX();
        double y = target.getPosY() + target.getHeight() / 2;
        double z = target.getPosZ();
        world.spawnParticle(ParticleTypes.END_ROD,
                x, y, z,
                15, 0.7, 0.7, 0.7, 0.1);
        world.spawnParticle(ParticleTypes.FLASH,
                x, y, z,
                5, 0.3, 0.3, 0.3, 0.0);
        if (level >= 3) {
            world.spawnParticle(ParticleTypes.FIREWORK,
                    x, y, z,
                    10, 0.5, 0.5, 0.5, 0.05);
        }
        if (level >= 5) {
            world.spawnParticle(ParticleTypes.DRAGON_BREATH,
                    x, y, z,
                    8, 0.6, 0.6, 0.6, 0.02);
        }
        world.playSound(null, x, y, z,
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                SoundCategory.PLAYERS, 1.0F, 0.8F);
        world.playSound(null, x, y, z,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 0.5F, 1.2F);
    }
    public static boolean isEnhanceStrikeDamage(DamageSource source) {
        return source instanceof EnhanceStrikeDamageSource;
    }
}