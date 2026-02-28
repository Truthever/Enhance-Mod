package com.weaponhouse.enhance.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
public class EnhanceCandyItem extends Item {
    public EnhanceCandyItem(Properties properties) {
        super(properties.food(new Food.Builder()
                .hunger(10)
                .saturation(10.0f)
                .build()));
    }
    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, LivingEntity entityLiving) {
        if (!world.isRemote && entityLiving instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entityLiving;
            CompoundNBT playerData = player.getPersistentData();
            playerData.putBoolean("EnhanceCandy", true);
            player.sendMessage(
                    new TranslationTextComponent("message.enhance_candy.activated")
                            .mergeStyle(TextFormatting.LIGHT_PURPLE),
                    player.getUniqueID()
            );
        }
        return super.onItemUseFinish(stack, world, entityLiving);
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }
    @Override
    public SoundEvent getEatSound() {
        return SoundEvents.ENTITY_GENERIC_EAT;
    }
}