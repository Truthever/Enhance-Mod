package com.weaponhouse.enhance.world.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryLookupCodec;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.SimplexNoiseGenerator;
import java.util.*;
import java.util.stream.Collectors;
public class SkylandsBiomeProvider extends BiomeProvider {
    public static final Codec<SkylandsBiomeProvider> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("seed").forGetter(provider -> provider.seed),
                    RegistryLookupCodec.getLookUpCodec(Registry.BIOME_KEY).forGetter(provider -> provider.biomeRegistry)
            ).apply(instance, instance.stable(SkylandsBiomeProvider::new))
    );
    private final long seed;
    private final Registry<Biome> biomeRegistry;
    private final SimplexNoiseGenerator biomeNoise;
    private final List<Biome> allBiomes;
    private final Set<Biome> vanillaForestBiomes = new HashSet<>();
    public SkylandsBiomeProvider(long seed, Registry<Biome> biomeRegistry) {
        super(getAvailableBiomes(biomeRegistry));
        this.seed = seed;
        this.biomeRegistry = biomeRegistry;
        this.biomeNoise = new SimplexNoiseGenerator(new Random(seed));
        this.allBiomes = getAvailableBiomes(biomeRegistry);
        initVanillaForestBiomes();
    }
    private void initVanillaForestBiomes() {
        ResourceLocation[] forestBiomeIds = {
                Biomes.FOREST.getLocation(),
                Biomes.BIRCH_FOREST.getLocation(),
                Biomes.DARK_FOREST.getLocation(),
                Biomes.TAIGA.getLocation(),
                Biomes.SNOWY_TAIGA.getLocation(),
                Biomes.FLOWER_FOREST.getLocation(),
                Biomes.BIRCH_FOREST_HILLS.getLocation(),
                Biomes.DARK_FOREST_HILLS.getLocation(),
                Biomes.TAIGA_HILLS.getLocation(),
                Biomes.SNOWY_TAIGA_HILLS.getLocation()
        };
        for (ResourceLocation id : forestBiomeIds) {
            biomeRegistry.getOptional(id).ifPresent(vanillaForestBiomes::add);
        }
    }
    private static List<Biome> getAvailableBiomes(Registry<Biome> biomeRegistry) {
        List<Biome> allBiomes = new ArrayList<>();
        for (Map.Entry<RegistryKey<Biome>, Biome> entry : biomeRegistry.getEntries()) {
            Biome biome = entry.getValue();
            ResourceLocation biomeId = entry.getKey().getLocation();
            if (shouldSkipBiome(biomeId)) {
                continue;
            }
            if (biomeId.getNamespace().equals("minecraft") &&
                    (biome.getCategory() == Biome.Category.OCEAN || biome.getCategory() == Biome.Category.RIVER)) {
                continue;
            }
            allBiomes.add(biome);
        }
        if (allBiomes.size() < 10) {
            addFallbackBiomes(biomeRegistry, allBiomes);
        }

        return allBiomes;
    }
    private static boolean shouldSkipBiome(ResourceLocation biomeId) {
        String namespace = biomeId.getNamespace();
        String path = biomeId.getPath();
        return path.contains("void") || path.contains("empty") || path.contains("null") ||
                path.contains("test") || path.contains("debug");
    }
    private static void addFallbackBiomes(Registry<Biome> biomeRegistry, List<Biome> biomes) {
        ResourceLocation[] fallbackBiomes = {
                Biomes.PLAINS.getLocation(),
                Biomes.FOREST.getLocation(),
                Biomes.BIRCH_FOREST.getLocation(),
                Biomes.DARK_FOREST.getLocation(),
                Biomes.TAIGA.getLocation(),
                Biomes.SNOWY_TAIGA.getLocation(),
                Biomes.JUNGLE.getLocation(),
                Biomes.DESERT.getLocation(),
                Biomes.SAVANNA.getLocation(),
                Biomes.BADLANDS.getLocation(),
                Biomes.SWAMP.getLocation(),
                Biomes.MUSHROOM_FIELDS.getLocation(),
                Biomes.ICE_SPIKES.getLocation(),
                Biomes.SUNFLOWER_PLAINS.getLocation(),
                Biomes.NETHER_WASTES.getLocation(),
                Biomes.CRIMSON_FOREST.getLocation(),
                Biomes.WARPED_FOREST.getLocation(),
                Biomes.SOUL_SAND_VALLEY.getLocation(),
                Biomes.BASALT_DELTAS.getLocation(),
                Biomes.THE_END.getLocation(),
                Biomes.END_HIGHLANDS.getLocation(),
                Biomes.END_MIDLANDS.getLocation(),
                Biomes.END_BARRENS.getLocation(),
                Biomes.SMALL_END_ISLANDS.getLocation()
        };
        for (ResourceLocation biomeId : fallbackBiomes) {
            biomeRegistry.getOptional(biomeId).ifPresent(biome -> {
                if (!biomes.contains(biome) &&
                        biome.getCategory() != Biome.Category.OCEAN &&
                        biome.getCategory() != Biome.Category.RIVER) {
                    biomes.add(biome);
                }
            });
        }
    }
    @Override
    public Biome getNoiseBiome(int x, int y, int z) {
        if (allBiomes.isEmpty()) {
            return biomeRegistry.getOrDefault(Biomes.PLAINS.getLocation());
        }
        double mainNoise = biomeNoise.getValue(x * 0.01, z * 0.01);
        double detailNoise = biomeNoise.getValue(x * 0.03, z * 0.03);
        double variationNoise = biomeNoise.getValue(x * 0.05, z * 0.05);
        double combinedNoise = (mainNoise + detailNoise * 0.5 + variationNoise * 0.25) / 1.75;
        int biomeIndex = (int) (((combinedNoise + 1.0) / 2.0) * allBiomes.size());
        biomeIndex = Math.max(0, Math.min(biomeIndex, allBiomes.size() - 1));
        Biome selectedBiome = allBiomes.get(biomeIndex);
        if (vanillaForestBiomes.contains(selectedBiome)) {
            Random random = new Random(seed + x * 31L + z * 17L);
            if (random.nextFloat() < 0.5) {
                selectedBiome = getNonForestBiome(random);
            }
        }
        return selectedBiome;
    }
    private Biome getNonForestBiome(Random random) {
        List<Biome> nonForestBiomes = allBiomes.stream()
                .filter(biome -> !vanillaForestBiomes.contains(biome))
                .collect(Collectors.toList());
        if (nonForestBiomes.isEmpty()) {
            return biomeRegistry.getOrDefault(Biomes.PLAINS.getLocation());
        }
        return nonForestBiomes.get(random.nextInt(nonForestBiomes.size()));
    }
    @Override
    protected Codec<? extends BiomeProvider> getBiomeProviderCodec() {
        return CODEC;
    }
    @Override
    public BiomeProvider getBiomeProvider(long seed) {
        return new SkylandsBiomeProvider(seed, this.biomeRegistry);
    }
}