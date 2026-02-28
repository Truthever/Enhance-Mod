package com.weaponhouse.enhance.effects;

import net.minecraft.potion.Effect;
import net.minecraftforge.event.RegistryEvent;
public class EffectRegistry {
    public static final Effect DARKNESS = new DarknessEffect();
    public static final Effect FORESTSPEED = new ForestSpeedEffect();
    public static final Effect FROST = new FrostEffect();
    public static final Effect MOUNTAINDEFENSE = new MountainDefenseEffect();
    public static final Effect DESERTATTACK = new DesertAttackEffect();
    public static final Effect OCEAN = new OceanEffect();
    public static final Effect PLAINS = new PlainsEffect();
    public static final Effect NETHER = new NetherEffect();
    public static final Effect END = new EndEffect();
    public static final Effect DEFENSEREDUCTION = new DefenseReductionEffect();
    public static final Effect SWAMPDEFENSE = new SwampDefenseEffect();
    public static final Effect MUSHROOMHEALTH = new MushroomHealthEffect();
    public static final Effect AURA = new AuraEffect();
    public static final Effect ICY_BLESSING = new IcyBlessingEffect();
    public static final Effect SWAMP = new SwampEffect();
    public static final Effect CORROSION = new CorrosionEffect();
    public static void registerEffects(RegistryEvent.Register<Effect> event) {
        event.getRegistry().registerAll(
                DARKNESS.setRegistryName("enhance", "darkness"),
                FORESTSPEED.setRegistryName("forest_speed"),
                MOUNTAINDEFENSE.setRegistryName("mountain_defense"),
                DESERTATTACK.setRegistryName("desert_attack"),
                FROST.setRegistryName("frost"),
                OCEAN.setRegistryName("ocean"),
                PLAINS.setRegistryName("plains"),
                NETHER.setRegistryName("nether"),
                END.setRegistryName("end"),
                DEFENSEREDUCTION.setRegistryName("defense_reduction"),
                SWAMPDEFENSE.setRegistryName("swamp_defense"),
                MUSHROOMHEALTH.setRegistryName("mushroom_health"),
                AURA.setRegistryName("aura"),
                ICY_BLESSING.setRegistryName("icy_blessing"),
                SWAMP.setRegistryName("swamp"),
                CORROSION.setRegistryName("corrosion")
        );
    }
}
