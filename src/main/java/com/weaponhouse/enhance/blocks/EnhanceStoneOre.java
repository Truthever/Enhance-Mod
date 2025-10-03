package com.weaponhouse.enhance.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.common.ToolType;

import java.util.Random;
public class EnhanceStoneOre extends OreBlock {

    public EnhanceStoneOre() {
        super(AbstractBlock.Properties.create(Material.ROCK)
                .hardnessAndResistance(5.0F, 6.0F)
                .harvestLevel(3)
                .harvestTool(ToolType.PICKAXE)
                .setRequiresTool()
                .sound(SoundType.STONE));
    }
    @Override
    protected int getExperience(Random rand) {
        return MathHelper.nextInt(rand, 3, 7);
    }
    @Override
    public int getExpDrop(BlockState state, IWorldReader world, BlockPos pos, int fortune, int silktouch) {
        return silktouch > 0 ? 0 : this.getExperience(new Random());
    }
}