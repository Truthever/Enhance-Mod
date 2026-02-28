package com.weaponhouse.enhance.gen;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.template.RuleTest;
import net.minecraft.world.gen.placement.Placement;
import net.minecraft.world.gen.placement.TopSolidRangeConfig;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = Enhance.MOD_ID)
public class OreGeneration {
    private static final RuleTest NATURAL_STONE = OreFeatureConfig.FillerBlockType.BASE_STONE_OVERWORLD;
    private static final int VEIN_SIZE = 4;
    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 30;
    private static final int COUNT = 1;
    @SubscribeEvent
    public static void generateOres(final BiomeLoadingEvent event) {
        BlockState enhanceStoneOre = RegistryHandler.ENHANCE_STONE_ORE.get().getDefaultState();
        BiomeGenerationSettingsBuilder generation = event.getGeneration();
        Biome.Category category = event.getCategory();
        if (category != Biome.Category.NETHER && category != Biome.Category.THEEND &&
                category != Biome.Category.OCEAN && category != Biome.Category.RIVER) {
            generation.withFeature(GenerationStage.Decoration.UNDERGROUND_ORES,
                    Feature.ORE.withConfiguration(
                                    new OreFeatureConfig(NATURAL_STONE, enhanceStoneOre, VEIN_SIZE))
                            .withPlacement(Placement.RANGE.configure(
                                    new TopSolidRangeConfig(MIN_HEIGHT, MIN_HEIGHT, MAX_HEIGHT)))
                            .square().count(COUNT));
        }
    }
}