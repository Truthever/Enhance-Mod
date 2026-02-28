package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
public class RobHandler {
    private static final int PICKUP_DELAY = 40;
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source instanceof ThornsHandler.ThornsDamageSource) {
            return;
        }
        if (event.getSource().getTrueSource() instanceof LivingEntity &&
                event.getEntityLiving() instanceof PlayerEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getTrueSource();
            PlayerEntity target = (PlayerEntity) event.getEntityLiving();
            CompoundNBT buffs = attacker.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
            if (buffs.contains("rob")) {
                stealItems(target, buffs.getInt("rob"));
            }
        }
    }
    private static void stealItems(PlayerEntity target, int countToSteal) {
        int stolen = 0;
        for (int i = 0; i < 36 && stolen < countToSteal; i++) {
            ItemStack stack = target.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemStack stolenStack = stack.split(Math.min(stack.getCount(), countToSteal - stolen));
                ItemEntity itemEntity = new ItemEntity(
                        target.world,
                        target.getPosX(),
                        target.getPosY() + 1.0,
                        target.getPosZ(),
                        stolenStack
                );
                double scatterPower = 0.5 + target.world.rand.nextDouble() * 0.5;
                itemEntity.setMotion(
                        target.world.rand.nextGaussian() * scatterPower,
                        0.5 + target.world.rand.nextDouble() * 0.5,
                        target.world.rand.nextGaussian() * scatterPower
                );
                itemEntity.setPickupDelay(PICKUP_DELAY);
                itemEntity.setNoDespawn();
                if (!target.world.isRemote) {
                    target.world.addEntity(itemEntity);
                }
                stolen += stolenStack.getCount();
                target.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F,
                        (target.world.rand.nextFloat() - target.world.rand.nextFloat()) * 0.7F + 1.0F);
            }
        }
    }
}