package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.commands.NBTEntityCommand;
import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.SkeletonArrowHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
@Mod.EventBusSubscriber
public class EnhanceViewerHandler {
    private static final float CURSE_DAMAGE_BONUS_PER_LEVEL = 0.10f;
    private static final float CURSE_ARROW_SPEED_BONUS_PER_LEVEL = 0.05f;
    private static final float MEGAFORCE_DAMAGE_BONUS_PER_LEVEL = 0.05f;
    private static final float MEGAFORCE_ARROW_SPEED_BONUS_PER_LEVEL = 0.025f;
    @SubscribeEvent
    public static void onEntityRightClick(PlayerInteractEvent.EntityInteract event) {
        PlayerEntity player = event.getPlayer();
        ItemStack itemStack = event.getItemStack();
        if (Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString().equals("enhance:attribute_viewer")) {
            if (!player.getEntityWorld().isRemote() && event.getTarget() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getTarget();
                showAttributesForEntity(player, target);
                event.setCanceled(true);
                event.setCancellationResult(ActionResultType.SUCCESS);
            }
        }
    }
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        PlayerEntity player = event.getPlayer();
        ItemStack itemStack = event.getItemStack();
        if (Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString().equals("enhance:attribute_viewer")) {
            if (!player.getEntityWorld().isRemote()) {
                LivingEntity target = getTargetEntity(player);
                if (target == null) {
                    showAttributesForEntity(player, player);
                    event.setCanceled(true);
                    event.setCancellationResult(ActionResultType.SUCCESS);
                }
            }
        }
    }
    private static LivingEntity getTargetEntity(PlayerEntity player) {
        double range = 10.0D;
        Vector3d eyePos = player.getEyePosition(1.0F);
        Vector3d lookVec = player.getLook(1.0F);
        Vector3d endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
        AxisAlignedBB aabb = player.getBoundingBox().expand(lookVec.scale(range)).grow(1.0D, 1.0D, 1.0D);
        List<LivingEntity> entities = player.getEntityWorld().getEntitiesWithinAABB(LivingEntity.class, aabb,
                entity -> entity != player && entity.isAlive());
        LivingEntity closestEntity = null;
        double closestDistance = range;
        for (LivingEntity entity : entities) {
            AxisAlignedBB entityBB = entity.getBoundingBox().grow(0.3D);
            Optional<Vector3d> hitVec = entityBB.rayTrace(eyePos, endPos);
            if (hitVec.isPresent()) {
                Vector3d hitPosition = hitVec.get();
                double distance = eyePos.distanceTo(hitPosition);
                RayTraceResult blockRayTrace = player.getEntityWorld().rayTraceBlocks(new RayTraceContext(
                        eyePos, hitPosition,
                        RayTraceContext.BlockMode.COLLIDER,
                        RayTraceContext.FluidMode.NONE,
                        player
                ));
                if (blockRayTrace.getType() != RayTraceResult.Type.BLOCK) {
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestEntity = entity;
                    }
                }
            }
        }
        return closestEntity;
    }
    private static void showAttributesForEntity(PlayerEntity player, LivingEntity target) {
        try {
            if (target instanceof VillagerEntity) {
                showVillagerAttributes(player, (VillagerEntity) target);
            } else if (target instanceof SkeletonEntity) {
                showSkeletonAttributes(player, (SkeletonEntity) target);
            } else {
                showBaseAttributesForPlayer(player, target);
            }
        } catch (Exception ignored) {}
    }
    private static void showVillagerAttributes(PlayerEntity viewingPlayer, VillagerEntity villager) {
        String entityName = villager.getName().getString();
        CompoundNBT nbt = villager.getPersistentData();
        double baseMaxHealth = java.util.Objects.requireNonNull(villager.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH)).getBaseValue();
        double baseMovementSpeed = java.util.Objects.requireNonNull(villager.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED)).getBaseValue();
        double baseAttackDamage = AttackHandler.getVillagerBaseAttack(villager);
        double dynamicAttackDamage = AttackHandler.getVillagerDynamicAttack(villager);
        float naturalResistance = nbt.contains("naturalArmor") ? nbt.getFloat("naturalArmor") : 0.0f;
        float dynamicArmor = nbt.contains("dynamicArmor") ? nbt.getFloat("dynamicArmor") : 0.0f;
        float baseSpeed = nbt.contains("speed") ? nbt.getFloat("speed") : (float) baseMovementSpeed;
        DamageBonusInfo damageBonusInfo = getDamageBonusInfo(villager);
        StringBuilder buffInfo = new StringBuilder();
        if (nbt.contains(NBTEntityCommand.BUFF_TAG)) {
            CompoundNBT buffs = nbt.getCompound(NBTEntityCommand.BUFF_TAG);
            for (String buffKey : buffs.keySet()) {
                TranslationTextComponent buffName = new TranslationTextComponent("buff.enhance." + buffKey);
                int buffLevel = buffs.getInt(buffKey);
                buffInfo.append(buffName.getString()).append(" Lv.").append(buffLevel).append("; ");
            }
        }
        if (nbt.contains("InspirationMarker")) {
            CompoundNBT inspiration = nbt.getCompound("InspirationMarker");
            if (inspiration.getBoolean("active")) {
                String buffName = inspiration.getString("buffName");
                int buffLevel = inspiration.getInt("buffLevel");
                long expireTime = inspiration.getLong("expireTime");
                long remainingTime = (expireTime - villager.world.getGameTime()) / 20;
                TranslationTextComponent inspirationName = new TranslationTextComponent("buff.enhance." + buffName);
                buffInfo.append(inspirationName.getString())
                        .append(" Lv.").append(buffLevel)
                        .append("(灵感 ").append(remainingTime).append("秒); ");
            }
        }
        String finalBuffInfo = buffInfo.length() > 0 ? buffInfo.toString().trim() : new TranslationTextComponent("command.nbtentity.no_buffs").getString();
        double dynamicMaxHealth = villager.getMaxHealth();
        float dynamicSpeed = (float) java.util.Objects.requireNonNull(villager.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED)).getValue();
        StringBuilder message = new StringBuilder();
        message.append(new TranslationTextComponent("command.nbtentity.attributes", entityName).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_max_health", String.format("%.2f", baseMaxHealth)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_max_health", String.format("%.2f", dynamicMaxHealth)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_attack_damage", String.format("%.2f", baseAttackDamage)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_attack_damage", String.format("%.2f", dynamicAttackDamage)).getString()).append("\n");
        if (damageBonusInfo.meleeBonusPercent > 0) {
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.melee_damage_bonus",
                    String.format("%.1f", damageBonusInfo.meleeBonusPercent * 100)).getString()).append("\n");
        }
        if (damageBonusInfo.arrowSpeedBonusPercent > 0) {
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.arrow_speed_bonus",
                    String.format("%.1f", damageBonusInfo.arrowSpeedBonusPercent * 100)).getString()).append("\n");
        }
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.natural_resistance", String.format("%.2f", naturalResistance)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_armor", String.format("%.2f", dynamicArmor)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.buffs", finalBuffInfo).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_speed", String.format("%.2f", baseSpeed)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_speed", String.format("%.2f", dynamicSpeed)).getString()).append("\n");
        viewingPlayer.sendMessage(new StringTextComponent(message.toString()), viewingPlayer.getUniqueID());
    }
    private static void showSkeletonAttributes(PlayerEntity viewingPlayer, SkeletonEntity skeleton) {
        String entityName = skeleton.getName().getString();
        CompoundNBT nbt = skeleton.getPersistentData();
        double baseMaxHealth = java.util.Objects.requireNonNull(skeleton.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH)).getBaseValue();
        double baseMovementSpeed = java.util.Objects.requireNonNull(skeleton.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED)).getBaseValue();
        double baseAttackDamage = 0.0;
        double dynamicAttackDamage = 0.0;
        if (skeleton.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
            baseAttackDamage = Objects.requireNonNull(skeleton.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue();
            dynamicAttackDamage = Objects.requireNonNull(skeleton.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
        }
        if (nbt.contains("dynamicAttackDamage")) {
            dynamicAttackDamage = nbt.getDouble("dynamicAttackDamage");
        }
        float naturalResistance = nbt.contains("naturalArmor") ? nbt.getFloat("naturalArmor") : 0.0f;
        float dynamicArmor = nbt.contains("dynamicArmor") ? nbt.getFloat("dynamicArmor") : 0.0f;
        float baseSpeed = nbt.contains("speed") ? nbt.getFloat("speed") : (float) baseMovementSpeed;
        DamageBonusInfo damageBonusInfo = getDamageBonusInfo(skeleton);
        float skeletonArrowSpeedBonus = SkeletonArrowHandler.getSkeletonArrowSpeedBonus(skeleton);
        float totalArrowSpeedBonus = skeletonArrowSpeedBonus + (damageBonusInfo.arrowSpeedBonusPercent * 100);
        StringBuilder buffInfo = new StringBuilder();
        if (nbt.contains(NBTEntityCommand.BUFF_TAG)) {
            CompoundNBT buffs = nbt.getCompound(NBTEntityCommand.BUFF_TAG);
            for (String buffKey : buffs.keySet()) {
                TranslationTextComponent buffName = new TranslationTextComponent("buff.enhance." + buffKey);
                int buffLevel = buffs.getInt(buffKey);
                buffInfo.append(buffName.getString()).append(" Lv.").append(buffLevel).append("; ");
            }
        }
        if (nbt.contains("InspirationMarker")) {
            CompoundNBT inspiration = nbt.getCompound("InspirationMarker");
            if (inspiration.getBoolean("active")) {
                String buffName = inspiration.getString("buffName");
                int buffLevel = inspiration.getInt("buffLevel");
                long expireTime = inspiration.getLong("expireTime");
                long remainingTime = (expireTime - skeleton.world.getGameTime()) / 20;

                TranslationTextComponent inspirationName = new TranslationTextComponent("buff.enhance." + buffName);
                buffInfo.append(inspirationName.getString())
                        .append(" Lv.").append(buffLevel)
                        .append("(灵感 ").append(remainingTime).append("秒); ");
            }
        }
        String finalBuffInfo = buffInfo.length() > 0 ? buffInfo.toString().trim() : new TranslationTextComponent("command.nbtentity.no_buffs").getString();
        double dynamicMaxHealth = skeleton.getMaxHealth();
        float dynamicSpeed = (float) java.util.Objects.requireNonNull(skeleton.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED)).getValue();
        StringBuilder message = new StringBuilder();
        message.append(new TranslationTextComponent("command.nbtentity.attributes", entityName).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_max_health", String.format("%.2f", baseMaxHealth)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_max_health", String.format("%.2f", dynamicMaxHealth)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_attack_damage", String.format("%.2f", baseAttackDamage)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_attack_damage", String.format("%.2f", dynamicAttackDamage)).getString()).append("\n");
        if (damageBonusInfo.meleeBonusPercent > 0) {
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.melee_damage_bonus",
                    String.format("%.1f", damageBonusInfo.meleeBonusPercent * 100)).getString()).append("\n");
        }
        if (totalArrowSpeedBonus > 0) {
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.total_arrow_speed_bonus",
                    String.format("%.1f", totalArrowSpeedBonus)).getString()).append("\n");
        }
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.natural_resistance", String.format("%.2f", naturalResistance)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_armor", String.format("%.2f", dynamicArmor)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.buffs", finalBuffInfo).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_speed", String.format("%.2f", baseSpeed)).getString()).append("\n");
        message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_speed", String.format("%.2f", dynamicSpeed)).getString()).append("\n");

        viewingPlayer.sendMessage(new StringTextComponent(message.toString()), viewingPlayer.getUniqueID());
    }
    private static void showBaseAttributesForPlayer(PlayerEntity viewingPlayer, LivingEntity entity) {
        if (entity instanceof LivingEntity) {
            String entityName = entity.getName().getString();
            double baseMaxHealth;
            double baseAttackDamage;
            double baseMovementSpeed;
            double dynamicMaxHealth;
            double dynamicAttackDamage;
            double dynamicSpeed;
            try {
                if (entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
                    baseMaxHealth = Objects.requireNonNull(entity.getAttribute(Attributes.MAX_HEALTH)).getBaseValue();
                    dynamicMaxHealth = entity.getMaxHealth();
                } else {
                    baseMaxHealth = dynamicMaxHealth = 20.0;
                }
                CompoundNBT nbt = entity.getPersistentData();
                if (entity instanceof VillagerEntity) {
                    baseAttackDamage = AttackHandler.getVillagerBaseAttack((VillagerEntity) entity);
                    dynamicAttackDamage = AttackHandler.getVillagerDynamicAttack((VillagerEntity) entity);
                } else if (nbt.contains("HasCustomAttackDamage") && nbt.getBoolean("HasCustomAttackDamage")) {
                    baseAttackDamage = nbt.getDouble("CustomAttackDamage");
                    dynamicAttackDamage = baseAttackDamage;
                } else if (entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                    baseAttackDamage = Objects.requireNonNull(entity.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue();
                    dynamicAttackDamage = Objects.requireNonNull(entity.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
                } else {
                    baseAttackDamage = dynamicAttackDamage = 0.0;
                }
                if (entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED) != null) {
                    baseMovementSpeed = Objects.requireNonNull(entity.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue();
                    dynamicSpeed = Objects.requireNonNull(entity.getAttribute(Attributes.MOVEMENT_SPEED)).getValue();
                } else {
                    baseMovementSpeed = dynamicSpeed = 0.1;
                }
            } catch (Exception e) {
                return;
            }
            float naturalResistance = 0.0f;
            float dynamicArmor = 0.0f;
            float baseSpeed = (float) baseMovementSpeed;
            CompoundNBT nbt = entity.getPersistentData();
            if (nbt.contains("naturalArmor")) {
                naturalResistance = nbt.getFloat("naturalArmor");
            }
            if (nbt.contains("dynamicArmor")) {
                dynamicArmor = nbt.getFloat("dynamicArmor");
            }
            if (nbt.contains("speed")) {
                baseSpeed = nbt.getFloat("speed");
            }
            DamageBonusInfo damageBonusInfo = getDamageBonusInfo(entity);
            StringBuilder buffInfo = new StringBuilder();
            if (nbt.contains(NBTEntityCommand.BUFF_TAG)) {
                CompoundNBT buffs = nbt.getCompound(NBTEntityCommand.BUFF_TAG);
                for (String buffKey : buffs.keySet()) {
                    TranslationTextComponent buffName = new TranslationTextComponent("buff.enhance." + buffKey);
                    int buffLevel = buffs.getInt(buffKey);
                    buffInfo.append(buffName.getString()).append(" Lv.").append(buffLevel).append("; ");
                }
            }
            if (nbt.contains("InspirationMarker")) {
                CompoundNBT inspiration = nbt.getCompound("InspirationMarker");
                if (inspiration.getBoolean("active")) {
                    String buffName = inspiration.getString("buffName");
                    int buffLevel = inspiration.getInt("buffLevel");
                    long expireTime = inspiration.getLong("expireTime");
                    long remainingTime = (expireTime - entity.world.getGameTime()) / 20;

                    TranslationTextComponent inspirationName = new TranslationTextComponent("buff.enhance." + buffName);
                    buffInfo.append(inspirationName.getString())
                            .append(" Lv.").append(buffLevel)
                            .append("(灵感 ").append(remainingTime).append("秒); ");
                }
            }
            String finalBuffInfo = buffInfo.length() > 0 ? buffInfo.toString().trim() : new TranslationTextComponent("command.nbtentity.no_buffs").getString();
            StringBuilder message = new StringBuilder();
            message.append(new TranslationTextComponent("command.nbtentity.attributes", entityName).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_max_health", String.format("%.2f", baseMaxHealth)).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_max_health", String.format("%.2f", dynamicMaxHealth)).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_attack_damage", String.format("%.2f", baseAttackDamage)).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_attack_damage", String.format("%.2f", dynamicAttackDamage)).getString()).append("\n");
            if (damageBonusInfo.meleeBonusPercent > 0) {
                message.append(" - ").append(new TranslationTextComponent("command.nbtentity.melee_damage_bonus",
                        String.format("%.1f", damageBonusInfo.meleeBonusPercent * 100)).getString()).append("\n");
            }
            if (damageBonusInfo.arrowSpeedBonusPercent > 0) {
                message.append(" - ").append(new TranslationTextComponent("command.nbtentity.arrow_speed_bonus",
                        String.format("%.1f", damageBonusInfo.arrowSpeedBonusPercent * 100)).getString()).append("\n");
            }
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.natural_resistance", String.format("%.2f", naturalResistance)).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_resistance", String.format("%.2f", dynamicArmor)).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.buffs", finalBuffInfo).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.base_speed", String.format("%.2f", baseSpeed)).getString()).append("\n");
            message.append(" - ").append(new TranslationTextComponent("command.nbtentity.dynamic_speed", String.format("%.2f", dynamicSpeed)).getString()).append("\n");
            if (entity instanceof PlayerEntity) {
                PlayerEntity playerEntity = (PlayerEntity) entity;
                float flySpeed = playerEntity.abilities.getFlySpeed();
                message.append("\n - ").append(new TranslationTextComponent("command.nbtentity.fly_speed", String.format("%.2f", flySpeed)).getString());
            }
            viewingPlayer.sendMessage(new StringTextComponent(message.toString()), viewingPlayer.getUniqueID());
        }
    }
    private static DamageBonusInfo getDamageBonusInfo(LivingEntity entity) {
        DamageBonusInfo info = new DamageBonusInfo();
        CompoundNBT data = entity.getPersistentData();
        if (data.contains(NBTEntityCommand.BUFF_TAG)) {
            CompoundNBT buffs = data.getCompound(NBTEntityCommand.BUFF_TAG);
            if (buffs.contains("curse")) {
                int curseLevel = buffs.getInt("curse");
                info.meleeBonusPercent += curseLevel * CURSE_DAMAGE_BONUS_PER_LEVEL;
                info.arrowSpeedBonusPercent += curseLevel * CURSE_ARROW_SPEED_BONUS_PER_LEVEL;
            }
            if (buffs.contains("megaforce")) {
                int megaforceLevel = buffs.getInt("megaforce");
                info.meleeBonusPercent += megaforceLevel * MEGAFORCE_DAMAGE_BONUS_PER_LEVEL;
                info.arrowSpeedBonusPercent += megaforceLevel * MEGAFORCE_ARROW_SPEED_BONUS_PER_LEVEL;
            }
        }
        return info;
    }
    private static class DamageBonusInfo {
        float meleeBonusPercent = 0.0f;
        float arrowSpeedBonusPercent = 0.0f;
    }
}