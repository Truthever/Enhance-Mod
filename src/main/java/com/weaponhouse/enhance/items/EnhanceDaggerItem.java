package com.weaponhouse.enhance.items;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
public class EnhanceDaggerItem extends SwordItem {
    private static final float ATTACK_DAMAGE = 4.0f;
    private static final float ATTACK_SPEED = 10.0f;
    private static final IItemTier SIMPLE_TIER = new IItemTier() {
        @Override
        public int getMaxUses() {
            return 0;
        }
        @Override
        public float getEfficiency() {
            return 0;
        }
        @Override
        public float getAttackDamage() {
            return ATTACK_DAMAGE;
        }
        @Override
        public int getHarvestLevel() {
            return 0;
        }
        @Override
        public int getEnchantability() {
            return 15;
        }
        @Override
        public net.minecraft.item.crafting.Ingredient getRepairMaterial() {
            return net.minecraft.item.crafting.Ingredient.EMPTY;
        }
    };
    public EnhanceDaggerItem(Properties properties) {
        super(SIMPLE_TIER, 0, ATTACK_SPEED, properties);
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getHeldItem(hand);
        if (!world.isRemote) {
            ThrownDaggerEntity daggerEntity = new ThrownDaggerEntity(world, player);
            daggerEntity.setItem(itemStack);
            int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);
            daggerEntity.setPowerLevel(powerLevel);
            daggerEntity.shoot(player.getLookVec().x, player.getLookVec().y, player.getLookVec().z, 1.5F, 1.0F);
            world.addEntity(daggerEntity);
            world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                    SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL,
                    0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        }
        if (!player.abilities.isCreativeMode) {
            itemStack.shrink(1);
        }
        return ActionResult.resultSuccess(itemStack);
    }
    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damageItem(1, attacker, (entity) -> entity.sendBreakAnimation(EquipmentSlotType.MAINHAND));
        return true;
    }
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.MENDING ||
                enchantment == Enchantments.UNBREAKING ||
                enchantment == Enchantments.SWEEPING) {
            return false;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    @Override
    public boolean isDamageable() {
        return false;
    }
    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }
}