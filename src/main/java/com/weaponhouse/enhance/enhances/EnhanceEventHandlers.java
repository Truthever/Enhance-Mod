package com.weaponhouse.enhance.enhances;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
public class EnhanceEventHandlers {
    public static void onLivingDeath(LivingDeathEvent event) {
        AnnihilationHandler.onLivingDeath(event);
    }
    public static void onEntityDeath(LivingDeathEvent event) {
        DeathBombHandler.onEntityDeath(event);
    }
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        AttackHandler.onEntityJoinWorld(event);
        LifeHandler.onEntityJoinWorld(event);
    }
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        AttackHandler.onPlayerLoggedIn(event);
        LifeHandler.onPlayerLoggedIn(event);
    }
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        AttackHandler.onPlayerRespawn(event);
        LifeHandler.onPlayerRespawn(event);
    }
    public static void onLivingAttack(LivingAttackEvent event) {
        AuraHandler.onLivingAttack(event);
        ChaosHandler.onLivingAttack(event);
        ComboHandler.onLivingAttack(event);
        CorrosionHandler.onLivingAttack(event);
        DisplacementHandler.onLivingAttack(event);
        FastingHandler.onLivingAttack(event);
        FrostHandler.onLivingAttack(event);
        HungerHandler.onLivingAttack(event);
        PhantomHandler.onLivingAttack(event);
        ThunderHandler.onLivingAttack(event);
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        AuraHandler.onProjectileImpact(event);
        ChaosHandler.onProjectileImpact(event);
        ComboHandler.onProjectileImpact(event);
        CorrosionHandler.onProjectileImpact(event);
        CurseHandler.onProjectileImpact(event);
        FastingHandler.onProjectileImpact(event);
        FrostHandler.onProjectileImpact(event);
        HungerHandler.onProjectileImpact(event);
        MegaForceHandler.onProjectileImpact(event);
        RicochetHandler.onProjectileImpact(event);
        ThunderHandler.onProjectileImpact(event);
    }
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        ComboHandler.onServerTick(event);
    }
    public static void onLivingHurt(LivingHurtEvent event) {
        CurseDamageAmplifier.onLivingHurt(event);
        CurseHandler.onLivingHurt(event);
        EnhanceStrikeHandler.onLivingHurt(event);
        InspirationHandler.onLivingHurt(event);
        MegaForceHandler.onLivingHurt(event);
        RobHandler.onLivingHurt(event);
        SummonHandler.onLivingHurt(event);
        ThornsHandler.onLivingHurt(event);
        UnyieldingHandler.onLivingHurt(event);
        VampireHandler.onLivingHurt(event);
    }
    public static void onLivingDamage(LivingDamageEvent event) {
        CurseHandler.onLivingDamage(event);
        MegaForceHandler.onLivingDamage(event);
        SpiritShieldHandler.onLivingDamage(event);
    }
    public static void onArrowShoot(EntityJoinWorldEvent event) {
        CurseHandler.onArrowShoot(event);
        MegaForceHandler.onArrowShoot(event);
    }
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        DisplacementHandler.onPlayerLogin(event);
    }
    public static void onPlayerDeath(LivingDeathEvent event) {
        DisplacementHandler.onPlayerDeath(event);
    }
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        EnhancerParticleHandler.onEntityUpdate(event);
        HarmonyHandler.onEntityUpdate(event);
        TrackingHandler.onEntityUpdate(event);
    }
    public static void onPlayerUseItemStart(LivingEntityUseItemEvent.Start event) {
        FastingHandler.onPlayerUseItemStart(event);
    }
    public static void onPlayerUseItemTick(LivingEntityUseItemEvent.Tick event) {
        FastingHandler.onPlayerUseItemTick(event);
    }
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        InspirationBuffExpiryHandler.onWorldTick(event);
    }
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PhotosynthesisHandler.onPlayerTick(event);
    }
    public static void onEntityTick(TickEvent.WorldTickEvent event) {
        PhotosynthesisHandler.onEntityTick(event);
    }
    public static void onLivingHeal(LivingHealEvent event) {
        SpiritShieldHandler.onLivingHeal(event);
    }
    public static void onEntityDamage(LivingDamageEvent event) {
        TrackingHandler.onEntityDamage(event);
    }
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        UnyieldingHandler.onLivingUpdate(event);
    }
}