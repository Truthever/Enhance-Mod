package com.weaponhouse.enhance.events;
import com.weaponhouse.enhance.items.EnhanceShovelItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Iterator;
@Mod.EventBusSubscriber(modid = "enhance")
public class EnhanceShovelEventHandler {
    @SubscribeEvent
    public static void onShovelBlockBreak(BlockEvent.BreakEvent event) {
        World world = (World) event.getWorld();
        PlayerEntity player = event.getPlayer();
        if (world.isRemote || player == null) {
            return;
        }
        ItemStack heldShovel = player.getHeldItemMainhand();
        if (!(heldShovel.getItem() instanceof EnhanceShovelItem)) {
            return;
        }
        if (!EnhanceShovelItem.isInitialized(heldShovel) || !EnhanceShovelItem.isAutoSmeltEnabled(heldShovel)) {
            return;
        }
        BlockPos breakPos = event.getPos();
        BlockState breakState = world.getBlockState(breakPos);
        Block breakBlock = breakState.getBlock();
        ItemStack blockItem = new ItemStack(breakBlock.asItem());
        RecipeManager recipeManager = world.getRecipeManager();
        Iterator<IRecipe<?>> smeltingRecipes = recipeManager.getRecipes().iterator();
        ItemStack smeltResult = ItemStack.EMPTY;
        while (smeltingRecipes.hasNext()) {
            IRecipe<?> recipe = smeltingRecipes.next();
            if (recipe.getIngredients().size() == 1) {
                Ingredient input = recipe.getIngredients().get(0);
                if (input.test(blockItem)) {
                    smeltResult = recipe.getRecipeOutput().copy();
                    break;
                }
            }
        }
        if (!smeltResult.isEmpty()) {
            event.setCanceled(true);
            world.destroyBlock(breakPos, false);
            Block.spawnAsEntity(world, breakPos, smeltResult);
            heldShovel.damageItem(1, player, p -> p.sendBreakAnimation(player.getActiveHand()));
        }
    }
}