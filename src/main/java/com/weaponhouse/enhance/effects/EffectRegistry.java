package com.weaponhouse.enhance.effects;

import net.minecraft.potion.Effect;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance", bus = Mod.EventBusSubscriber.Bus.MOD)
public class EffectRegistry {
    public static final Effect REDUCE_MAX_HEALTH = new ReduceMaxHealthEffect();
    public static final Effect REDUCE_ARMOR = new ReduceResistanceEffect();
    public static final Effect REDUCE_ATTACK_DAMAGE = new ReduceAttackEffect();
    public static final Effect REDUCE_SPEED = new ReduceSpeedEffect();
    public static final Effect INCREASE_MAX_HEALTH = new IncreaseMaxHealthEffect();
    public static final Effect INCREASE_ARMOR = new IncreaseResistanceEffect();
    public static final Effect INCREASE_ATTACK_DAMAGE = new IncreaseAttackEffect();
    public static final Effect INCREASE_SPEED = new IncreaseSpeedEffect();
    public static final Effect DARKNESS = new DarknessEffect();
    public static final Effect HEALING_REDUCTION = new HealingReductionEffect();
    public static final Effect HUNGRYFIRST = new HungryFirstEffect();
    public static final Effect HUNGRYSECOND = new HungrySecondEffect();
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
    public static final Effect CURSE = new CursePotionEffect();
    public static final Effect CURSEDAMAGE = new CurseDamageBoostEffect();
    public static final Effect AURA = new AuraEffect();
    public static final Effect ICY_BLESSING = new IcyBlessingEffect();
    public static final Effect SWAMP = new SwampEffect();
    @SubscribeEvent
    public static void registerEffects(RegistryEvent.Register<Effect> event) {
        event.getRegistry().registerAll(
                REDUCE_MAX_HEALTH.setRegistryName("reduce_max_health"),
                REDUCE_ARMOR.setRegistryName("reduce_armor"),
                REDUCE_ATTACK_DAMAGE.setRegistryName("reduce_attack_damage"),
                REDUCE_SPEED.setRegistryName("reduce_speed"),
                INCREASE_MAX_HEALTH.setRegistryName("increase_max_health"),
                INCREASE_ARMOR.setRegistryName("increase_armor"),
                INCREASE_ATTACK_DAMAGE.setRegistryName("increase_attack_damage"),
                INCREASE_SPEED.setRegistryName("increase_speed"),
                HEALING_REDUCTION.setRegistryName("healing_reduction"),
                DARKNESS.setRegistryName("enhance", "darkness"),
                HUNGRYFIRST.setRegistryName("enhance", "hungryfirst"),
                HUNGRYSECOND.setRegistryName("enhance", "hungrysecond"),
                FORESTSPEED.setRegistryName("forestspeed"),
                MOUNTAINDEFENSE.setRegistryName("mountaindefense"),
                DESERTATTACK.setRegistryName("desertattack"),
                FROST.setRegistryName("frost"),
                OCEAN.setRegistryName("ocean"),
                PLAINS.setRegistryName("plains"),
                NETHER.setRegistryName("nether"),
                END.setRegistryName("end"),
                DEFENSEREDUCTION.setRegistryName("defensereduction"),
                SWAMPDEFENSE.setRegistryName("swampdefense"),
                MUSHROOMHEALTH.setRegistryName("mushroomhealth"),
                CURSE.setRegistryName("curse"),
                CURSEDAMAGE.setRegistryName("cursedamage"),
                AURA.setRegistryName("aura"),
                ICY_BLESSING.setRegistryName("icy_blessing"),
                SWAMP.setRegistryName("swamp")
        );
    }
}
