package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.util.text.TranslationTextComponent;
public class HungryFirstEffect extends BaseEffect {
    public HungryFirstEffect() {
        super(EffectType.HARMFUL, 0xAA5500);
    }
    @Override
    public void applyEffect(LivingEntity entity, int amplifier) {
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        if (!player.getPersistentData().contains("baseAttack")) {
            float baseAttack = (float) player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue();
            player.getPersistentData().putFloat("baseAttack", baseAttack);
        }
        float baseAttack = player.getPersistentData().getFloat("baseAttack");
        player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * 0.8f);
        if (!player.getPersistentData().contains("baseSpeed")) {
            float baseSpeed = (float) player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
            player.getPersistentData().putFloat("baseSpeed", baseSpeed);
        }
        float baseSpeed = player.getPersistentData().getFloat("baseSpeed");
        player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * 0.5f);
        player.jumpMovementFactor = 0;
        player.abilities.allowFlying = false;
        player.abilities.isFlying = false;
        player.sendMessage(
                new TranslationTextComponent("effect.hungry.apply_message"),
                player.getUniqueID()
        );
    }
    @Override
    public void removeEffect(LivingEntity entity, int amplifier) {
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        if (player.getPersistentData().contains("baseAttack")) {
            float baseAttack = player.getPersistentData().getFloat("baseAttack");
            player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack);
            player.getPersistentData().remove("baseAttack");
        }
        if (player.getPersistentData().contains("baseSpeed")) {
            float baseSpeed = player.getPersistentData().getFloat("baseSpeed");
            player.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);
            player.getPersistentData().remove("baseSpeed");
        }
        player.jumpMovementFactor = 0.02f;
        player.sendMessage(
                new TranslationTextComponent("effect.hungry.remove_message"),
                player.getUniqueID()
        );
    }
}
