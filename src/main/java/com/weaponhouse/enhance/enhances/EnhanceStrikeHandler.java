package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.items.EnhanceSwordItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Random;
@Mod.EventBusSubscriber(modid = "enhance")
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
        public boolean isEnhanceStrikeDamage() {
            return true;
        }
    }
    @SubscribeEvent
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
        if (RANDOM.nextFloat() < TRIGGER_CHANCE) {
            float originalDamage = event.getAmount();
            float bonusDamage = originalDamage * (1 + enhanceStrikeLevel * DAMAGE_BONUS_PER_LEVEL);
            EnhanceStrikeDamageSource strikeSource = new EnhanceStrikeDamageSource(player);
            boolean damageSuccess = target.attackEntityFrom(strikeSource, bonusDamage);
        }
    }
    public static boolean isEnhanceStrikeDamage(DamageSource source) {
        return source instanceof EnhanceStrikeDamageSource;
    }
}
