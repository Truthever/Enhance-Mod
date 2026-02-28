package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = "enhance")
public class SkeletonArrowHandler {
    private static final float ARROW_SPEED_BONUS_PER_2_ATTACK = 0.025f;
    @SubscribeEvent
    public static void onArrowShoot(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            if (arrow.getShooter() instanceof SkeletonEntity) {
                SkeletonEntity skeleton = (SkeletonEntity) arrow.getShooter();
                applySkeletonArrowBonus(skeleton, arrow);
            }
        }
    }
    private static void applySkeletonArrowBonus(SkeletonEntity skeleton, AbstractArrowEntity arrow) {
        try {
            double dynamicAttack = getSkeletonDynamicAttack(skeleton);
            if (dynamicAttack > 0) {
                float speedBonus = (float) (Math.floor(dynamicAttack / 2) * ARROW_SPEED_BONUS_PER_2_ATTACK);
                if (speedBonus > 0) {
                    double originalMotionX = arrow.getMotion().x;
                    double originalMotionY = arrow.getMotion().y;
                    double originalMotionZ = arrow.getMotion().z;
                    double newMotionX = originalMotionX * (1 + speedBonus);
                    double newMotionY = originalMotionY * (1 + speedBonus);
                    double newMotionZ = originalMotionZ * (1 + speedBonus);
                    arrow.setMotion(newMotionX, newMotionY, newMotionZ);
                }
            }
        } catch (Exception ignored) {}
    }
    private static double getSkeletonDynamicAttack(SkeletonEntity skeleton) {
        try {
            CompoundNBT nbt = skeleton.getPersistentData();
            if (nbt.contains("dynamicAttackDamage")) {
                return nbt.getDouble("dynamicAttackDamage");
            }
            if (skeleton.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                return Objects.requireNonNull(skeleton.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            }
            return 4.0;
        } catch (Exception e) {
            return 4.0;
        }
    }
    public static float getSkeletonArrowSpeedBonus(SkeletonEntity skeleton) {
        try {
            double dynamicAttack = getSkeletonDynamicAttack(skeleton);
            if (dynamicAttack > 0) {
                return (float) (Math.floor(dynamicAttack / 2) * ARROW_SPEED_BONUS_PER_2_ATTACK * 100);
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }
}