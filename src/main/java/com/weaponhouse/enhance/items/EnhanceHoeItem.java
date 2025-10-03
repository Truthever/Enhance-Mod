package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraftforge.common.ToolType;
import javax.annotation.Nullable;
import java.util.List;
public class EnhanceHoeItem extends HoeItem {
    private static final String INITIALIZED_TAG = "enhance_hoe_initialized";
    private static final int MAX_ENHANCE_LEVEL = 6;
    private static final String LAST_BOOST_TIME_TAG = "enhance_hoe_last_boost_time";
    private static final int BOOST_INTERVAL = 400;
    private static final int BOOST_RANGE = 3;
    private static final int BOOST_STEP = 1;
    public EnhanceHoeItem(IItemTier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder
                .addToolType(ToolType.HOE, tier.getHarvestLevel()));
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !(entityIn instanceof PlayerEntity) || !isSelected
                || !isInitialized(stack) || ((PlayerEntity) entityIn).getHeldItemMainhand() != stack) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
        CompoundNBT nbt = stack.getOrCreateTag();
        long currentGameTime = worldIn.getGameTime();
        long lastBoostTime = nbt.getLong(LAST_BOOST_TIME_TAG);
        if (currentGameTime - lastBoostTime >= BOOST_INTERVAL) {
            int boostedCount = boostCropsInRange(worldIn, player.getPosition());
            nbt.putLong(LAST_BOOST_TIME_TAG, currentGameTime);
            stack.setTag(nbt);
            if (boostedCount > 0) {
                player.sendMessage(new TranslationTextComponent(
                        "message.enhance_hoe.boost_success", boostedCount
                ).mergeStyle(TextFormatting.YELLOW), player.getUniqueID());
            }
        }
    }
    private int boostCropsInRange(World world, BlockPos centerPos) {
        int boostedCount = 0;
        for (int x = -BOOST_RANGE; x <= BOOST_RANGE; x++) {
            for (int z = -BOOST_RANGE; z <= BOOST_RANGE; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos targetPos = centerPos.add(x, y, z);
                    BlockState state = world.getBlockState(targetPos);
                    if (state.getBlock() instanceof CropsBlock) {
                        CropsBlock crop = (CropsBlock) state.getBlock();
                        int currentAge = state.get(crop.getAgeProperty());
                        int maxAge = crop.getMaxAge();
                        if (currentAge < maxAge) {
                            int newAge = currentAge + BOOST_STEP;
                            BlockState newState = crop.getDefaultState().with(crop.getAgeProperty(), newAge);
                            world.setBlockState(targetPos, newState);
                            boostedCount++;
                        }
                    }
                }
            }
        }
        return boostedCount;
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.base_info")
                .mergeStyle(TextFormatting.BLUE));
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.yield_rule")
                .mergeStyle(TextFormatting.LIGHT_PURPLE));
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.boost_rule")
                .mergeStyle(TextFormatting.GOLD));
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.max_level", MAX_ENHANCE_LEVEL)
                .mergeStyle(TextFormatting.GRAY));
        if (!isInitialized(stack)) {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
    }
    public static boolean isInitialized(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_HOE_TIER.getRepairMaterial().test(repair);
    }
}