package com.weaponhouse.enhance.world.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryLookupCodec;
import net.minecraft.world.Blockreader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.gen.SimplexNoiseGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
public class SkylandsChunkGenerator extends ChunkGenerator {
    private static final int MAX_ISLAND_DIAMETER = 400;
    private static final int MAX_ISLAND_RADIUS = MAX_ISLAND_DIAMETER / 2;
    private static final double ISLAND_CENTER_THRESHOLD = 0.7;
    private static final double ROUND_SHAPE_NOISE = 0.001;
    private static final int BASE_ISLAND_HEIGHT = 60;
    private static final int MAX_HEIGHT_OFFSET = 10;
    private static final DimensionStructuresSettings ENABLED_STRUCTURE_SETTINGS;
    private static final StructureSeparationSettings DEFAULT_SEPARATION = new StructureSeparationSettings(24, 8, 123456789);
    private static final Map<Structure<?>, StructureSeparationSettings> STRUCTURE_LIST;
    static {
        ENABLED_STRUCTURE_SETTINGS = new DimensionStructuresSettings(true);
        STRUCTURE_LIST = new HashMap<>();
        STRUCTURE_LIST.put(Structure.DESERT_PYRAMID, DEFAULT_SEPARATION);
        STRUCTURE_LIST.put(Structure.JUNGLE_PYRAMID, DEFAULT_SEPARATION);
        STRUCTURE_LIST.put(Structure.IGLOO, DEFAULT_SEPARATION);
        STRUCTURE_LIST.put(Structure.SWAMP_HUT, DEFAULT_SEPARATION);
    }
    public static final Codec<SkylandsChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeProvider.CODEC.fieldOf("biome_source").forGetter(SkylandsChunkGenerator::getBiomeProvider),
                    Codec.LONG.fieldOf("seed").forGetter(SkylandsChunkGenerator::getSeed),
                    RegistryLookupCodec.getLookUpCodec(Registry.BIOME_KEY).forGetter(SkylandsChunkGenerator::getBiomeRegistry)
            ).apply(instance, instance.stable(SkylandsChunkGenerator::new))
    );
    private final long seed;
    private final Registry<Biome> biomeRegistry;
    private final SimplexNoiseGenerator islandNoise;
    private final SimplexNoiseGenerator terrainNoise;
    private final SimplexNoiseGenerator caveNoise;
    private final SimplexNoiseGenerator enhanceBlockNoise;
    private final Random structureRandom;
    private final SimplexNoiseGenerator roundShapeNoise;
    public SkylandsChunkGenerator(BiomeProvider biomeProvider, long seed, Registry<Biome> biomeRegistry) {
        super(biomeProvider, ENABLED_STRUCTURE_SETTINGS);
        this.seed = seed;
        this.biomeRegistry = biomeRegistry;
        this.islandNoise = new SimplexNoiseGenerator(new Random(seed));
        this.terrainNoise = new SimplexNoiseGenerator(new Random(seed + 1));
        this.caveNoise = new SimplexNoiseGenerator(new Random(seed + 2));
        this.enhanceBlockNoise = new SimplexNoiseGenerator(new Random(seed + 3));
        this.structureRandom = new Random(seed + 5);
        this.roundShapeNoise = new SimplexNoiseGenerator(new Random(seed + 6));
    }
    public long getSeed() {
        return seed;
    }
    public Registry<Biome> getBiomeRegistry() {
        return biomeRegistry;
    }
    @Override
    protected Codec<? extends ChunkGenerator> func_230347_a_() {
        return CODEC;
    }
    @Override
    public ChunkGenerator func_230349_a_(long seed) {
        return new SkylandsChunkGenerator(this.biomeProvider.getBiomeProvider(seed), seed, this.biomeRegistry);
    }
    @Override
    public void generateSurface(WorldGenRegion region, IChunk chunk) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                double islandValue = islandNoise.getValue(worldX * 0.002, worldZ * 0.002);
                if (islandValue <= 0.3) continue;
                BlockPos islandCenter = findIslandCenter(worldX, worldZ);
                if (islandCenter == null) continue;
                double dx = worldX - islandCenter.getX();
                double dz = worldZ - islandCenter.getZ();
                double sphericalDistance = Math.sqrt(dx * dx + dz * dz);
                double shapeNoise = roundShapeNoise.getValue(
                        islandCenter.getX() * ROUND_SHAPE_NOISE,
                        islandCenter.getZ() * ROUND_SHAPE_NOISE
                );
                double dynamicRadius = MAX_ISLAND_RADIUS * (1 + shapeNoise * 0.05);
                if (sphericalDistance > dynamicRadius) continue;
                double terrainValue = terrainNoise.getValue(worldX * 0.01, worldZ * 0.01);
                double heightMultiplier = (islandValue - 0.3) / 0.7;
                int baseHeight = BASE_ISLAND_HEIGHT + (int)(heightMultiplier * MAX_HEIGHT_OFFSET);
                int terrainHeight = (int)((terrainValue + 1.0) * 1 * heightMultiplier);
                int totalHeight = baseHeight + terrainHeight;
                generateIslandBlocks(chunk, x, z, totalHeight, worldX, worldZ);
            }
        }
        generateEnhanceBlocks(chunk, chunkX, chunkZ);
    }
    private void generateIslandBlocks(IChunk chunk, int x, int z, int surfaceHeight, int worldX, int worldZ) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        Biome biome = this.biomeProvider.getNoiseBiome(worldX, 64, worldZ);
        ResourceLocation biomeId = this.biomeRegistry.getKey(biome);
        BlockState top = Blocks.GRASS_BLOCK.getDefaultState();
        BlockState middle = Blocks.DIRT.getDefaultState();
        BlockState bottom = Blocks.STONE.getDefaultState();
        if (biomeId != null) {
            String biomeName = biomeId.getPath().toLowerCase();
            if (biomeName.contains("desert")) {
                top = Blocks.SAND.getDefaultState();
                middle = Blocks.SANDSTONE.getDefaultState();
            } else if (biomeName.contains("nether")) {
                top = Blocks.NETHERRACK.getDefaultState();
                middle = Blocks.NETHERRACK.getDefaultState();
                bottom = Blocks.NETHERRACK.getDefaultState();
            } else if (biomeName.contains("end")) {
                top = Blocks.END_STONE.getDefaultState();
                middle = Blocks.END_STONE.getDefaultState();
                bottom = Blocks.END_STONE.getDefaultState();
            }
        }
        for (int y = 1; y <= surfaceHeight; y++) {
            BlockState state;
            if (y == surfaceHeight) {
                state = top;
            } else if (y > surfaceHeight - 3) {
                state = middle;
            } else {
                state = bottom;
                if (shouldGenerateCave(worldX, y, worldZ, surfaceHeight)) continue;
            }
            if (state.getBlock() == Blocks.SNOW_BLOCK || state.getBlock() == Blocks.SNOW) {
                state = Blocks.GRASS_BLOCK.getDefaultState();
            }
            if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA) {
                state = Blocks.STONE.getDefaultState();
            }
            chunk.setBlockState(pos.setPos(x, y, z), state, false);
        }
    }
    private BlockPos findIslandCenter(int worldX, int worldZ) {
        int searchRadius = 50;
        BlockPos center = new BlockPos(worldX, 80, worldZ);
        double maxNoise = islandNoise.getValue(worldX * 0.002, worldZ * 0.002);
        for (int dx = -searchRadius; dx <= searchRadius; dx += 5) {
            for (int dz = -searchRadius; dz <= searchRadius; dz += 5) {
                int cx = worldX + dx;
                int cz = worldZ + dz;
                double noise = islandNoise.getValue(cx * 0.002, cz * 0.002);
                if (noise > maxNoise && noise > ISLAND_CENTER_THRESHOLD) {
                    maxNoise = noise;
                    center = new BlockPos(cx, 80, cz);
                }
            }
        }
        return maxNoise > ISLAND_CENTER_THRESHOLD ? center : null;
    }
    @Override
    public void func_230352_b_(IWorld world, StructureManager structureManager, IChunk chunk) {
        if (!(world instanceof WorldGenRegion)) {
            return;
        }
        WorldGenRegion genRegion = (WorldGenRegion) world;
        ChunkPos chunkPos = chunk.getPos();
        if (!isChunkInIsland(chunkPos.x, chunkPos.z)) {
            return;
        }
        int islandHeight = getSafeIslandHeight(genRegion, chunkPos);
        if (islandHeight <= 0) {
            return;
        }
        for (Map.Entry<Structure<?>, StructureSeparationSettings> entry : STRUCTURE_LIST.entrySet()) {
            Structure<?> structure = entry.getKey();
            StructureSeparationSettings settings = entry.getValue();
            try {
                BlockPos chunkCenter = new BlockPos(
                        chunkPos.x * 16 + 8,
                        islandHeight,
                        chunkPos.z * 16 + 8
                );
                if (!genRegion.chunkExists(chunkCenter.getX() >> 4, chunkCenter.getZ() >> 4)) {
                    continue;
                }
                BlockPos structPos = structure.func_236388_a_(
                        genRegion, structureManager, chunkCenter,
                        0,
                        false, this.seed, settings
                );
                if (structPos == null) {
                    continue;
                }
                ChunkPos structChunkPos = new ChunkPos(structPos);
                if (!genRegion.chunkExists(structChunkPos.x, structChunkPos.z)) {
                    continue;
                }
                if (structChunkPos.x != chunkPos.x || structChunkPos.z != chunkPos.z) {
                    continue;
                }
                structPos = new BlockPos(structPos.getX(), islandHeight, structPos.getZ());
                markSafeStructurePosition(genRegion, structureManager, chunk, structure, structPos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private int getSafeIslandHeight(WorldGenRegion genRegion, ChunkPos chunkPos) {
        int total = 0;
        int count = 0;
        int[] samples = {4, 12};
        for (int x : samples) {
            for (int z : samples) {
                int wx = chunkPos.x * 16 + x;
                int wz = chunkPos.z * 16 + z;
                int cx = wx >> 4;
                int cz = wz >> 4;
                if (!genRegion.chunkExists(cx, cz)) {
                    continue;
                }
                int h = genRegion.getHeight(Heightmap.Type.WORLD_SURFACE_WG, wx, wz);
                if (h > 0) {
                    total += h;
                    count++;
                }
            }
        }
        return count > 0 ? total / count : 0;
    }
    private void markSafeStructurePosition(
            WorldGenRegion genRegion,
            StructureManager structureManager,
            IChunk chunk,
            Structure<?> structure,
            BlockPos pos
    ) {
        try {
            ChunkPos structChunkPos = new ChunkPos(pos);
            if (!genRegion.chunkExists(structChunkPos.x, structChunkPos.z)) {
                return;
            }
            SectionPos sectionPos = SectionPos.from(structChunkPos, 0);
            structureManager.addReference(sectionPos, structure, this.seed, chunk);
        } catch (Exception ignored) {}
    }
    private boolean isChunkInIsland(int chunkX, int chunkZ) {
        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;
        double noise = islandNoise.getValue(centerX * 0.002, centerZ * 0.002);
        return noise > 0.3;
    }
    private boolean shouldGenerateCave(int x, int y, int z, int surfaceHeight) {
        if (y <= 10 || y >= surfaceHeight - 5) return false;
        double n1 = caveNoise.getValue(x * 0.03, y * 0.1);
        double n2 = caveNoise.getValue(x * 0.05, y * 0.08);
        double n3 = caveNoise.getValue(x * 0.08, y * 0.05);
        double avg = (n1 + n2 * 0.7 + n3 * 0.4) / 2.1;
        double depth = 1.0 - (double)(y - 10) / (surfaceHeight - 15);
        double threshold = 0.4 + depth * 0.3;
        return avg > threshold;
    }
    private void generateEnhanceBlocks(IChunk chunk, int chunkX, int chunkZ) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        Random rand = new Random(seed + chunkX * 31L + chunkZ * 17L);
        BlockState enhanceBlock = getEnhanceBlock();
        if (enhanceBlock == null) return;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkX * 16 + x;
                int wz = chunkZ * 16 + z;
                if (isFarFromIsland(wx, wz) && rand.nextFloat() < 0.0005f) {
                    int y = 1 + rand.nextInt(30);
                    if (chunk.getBlockState(pos.setPos(x, y, z)).isAir()) {
                        chunk.setBlockState(pos, enhanceBlock, false);
                        generateEnhanceCluster(chunk, x, y, z, rand, enhanceBlock);
                    }
                }
            }
        }
    }
    private boolean isFarFromIsland(int x, int z) {
        double noise = islandNoise.getValue(x * 0.002, z * 0.002);
        if (noise > 0.3) return false;
        int count = 0;
        int check = 0;
        for (int dx = -12; dx <= 12; dx += 3) {
            for (int dz = -12; dz <= 12; dz += 3) {
                if (dx == 0 && dz == 0) continue;
                check++;
                if (islandNoise.getValue((x + dx) * 0.002, (z + dz) * 0.002) > 0.3) count++;
            }
        }
        return count <= check * 0.1;
    }
    private void generateEnhanceCluster(IChunk chunk, int x, int y, int z, Random rand, BlockState state) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int size = 1 + rand.nextInt(3);
        for (int i = 0; i < size; i++) {
            int dx = x + rand.nextInt(5) - 2;
            int dz = z + rand.nextInt(5) - 2;
            int dy = y + rand.nextInt(3) - 1;

            if (dx >= 0 && dx < 16 && dz >= 0 && dz < 16 && dy >= 1 && dy <= 30) {
                if (chunk.getBlockState(pos.setPos(dx, dy, dz)).isAir()) {
                    chunk.setBlockState(pos, state, false);
                }
            }
        }
    }
    private BlockState getEnhanceBlock() {
        try {
            return Registry.BLOCK.getOrDefault(new ResourceLocation("enhance:enhance_block")).getDefaultState();
        } catch (Exception e) {
            return null;
        }
    }
    @Override
    public int getHeight(int x, int z, Heightmap.Type type) {
        double noise = islandNoise.getValue(x * 0.002, z * 0.002);
        if (noise <= 0.3) {
            return 0;
        }
        BlockPos center = findIslandCenter(x, z);
        if (center != null) {
            double dx = x - center.getX();
            double dz = z - center.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double shape = roundShapeNoise.getValue(center.getX() * 0.001, center.getZ() * 0.001);
            double radius = MAX_ISLAND_RADIUS * (1 + shape * 0.05);
            if (dist > radius) {
                return 0;
            }
        }
        double multiplier = (noise - 0.3) / 0.7;
        return BASE_ISLAND_HEIGHT + (int)(multiplier * MAX_HEIGHT_OFFSET);
    }
    @Override
    public IBlockReader func_230348_a_(int x, int z) {
        return new Blockreader(new BlockState[0]);
    }
}