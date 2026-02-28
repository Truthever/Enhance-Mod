package com.weaponhouse.enhance.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraftforge.common.ToolType;
public class EnhanceBlock extends Block {
    public EnhanceBlock() {
        super(Properties.create(Material.ROCK, MaterialColor.BLACK)
                .hardnessAndResistance(50.0f, 1200.0f)
                .sound(SoundType.STONE)
                .harvestLevel(3)
                .harvestTool(ToolType.PICKAXE)
                .setRequiresTool()
        );
    }
}