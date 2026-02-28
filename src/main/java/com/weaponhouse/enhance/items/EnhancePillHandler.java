package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.blocks.EnhanceLandPortalFrameBlock;
import com.weaponhouse.enhance.enhances.AttackHandler;
import com.weaponhouse.enhance.enhances.BossBarHandler;
import com.weaponhouse.enhance.enhances.LifeHandler;
import com.weaponhouse.enhance.enhances.SpiritShieldHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
public class EnhancePillHandler {
    private static final String ENHANCE_PILL_ID = "enhance:enhance_pill";
    private static boolean isProcessing = false;
    public static void onEntityRightClick(PlayerInteractEvent.EntityInteract event) {
        if (isProcessing) return;
        PlayerEntity player = event.getPlayer();
        ItemStack itemStack = event.getItemStack();
        if (Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString().equals(ENHANCE_PILL_ID)) {
            if (!player.getEntityWorld().isRemote() && event.getTarget() instanceof LivingEntity) {
                isProcessing = true;
                try {
                    LivingEntity target = (LivingEntity) event.getTarget();
                    applyPillBuffs(itemStack, player, target, true);
                    if (!player.abilities.isCreativeMode) {
                        itemStack.shrink(1);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(ActionResultType.SUCCESS);
                } finally {
                    isProcessing = false;
                }
            }
        }
    }
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (isProcessing) return;
        PlayerEntity player = event.getPlayer();
        ItemStack itemStack = event.getItemStack();
        if (Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString().equals(ENHANCE_PILL_ID)) {
            BlockRayTraceResult rayTrace = (BlockRayTraceResult) player.pick(5.0D, 1.0F, false);
            BlockPos pos = rayTrace.getPos();
            BlockState state = player.getEntityWorld().getBlockState(pos);
            if (state.getBlock() instanceof EnhanceLandPortalFrameBlock) {
                return;
            }
            if (!player.getEntityWorld().isRemote()) {
                isProcessing = true;
                try {
                    LivingEntity target = getTargetEntity(player);
                    if (target != null && target != player) {
                        return;
                    }
                    applyPillBuffs(itemStack, player, player, false);
                    if (!player.abilities.isCreativeMode) {
                        itemStack.shrink(1);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(ActionResultType.SUCCESS);
                } finally {
                    isProcessing = false;
                }
            }
        }
    }
    private static void applyPillBuffs(ItemStack pillStack, PlayerEntity user, LivingEntity target, boolean isEntityInteract) {
        CompoundNBT pillNBT = pillStack.getOrCreateTag();
        if (pillNBT.contains("EnhancePillBuffs")) {
            CompoundNBT pillBuffs = pillNBT.getCompound("EnhancePillBuffs");
            CompoundNBT targetData = target.getPersistentData();
            CompoundNBT targetBuffs = targetData.contains("WeaponHouseBuffs") ?
                    targetData.getCompound("WeaponHouseBuffs") : new CompoundNBT();
            int appliedCount = 0;
            StringBuilder appliedBuffs = new StringBuilder();
            for (String buffKey : pillBuffs.keySet()) {
                if ("PillTextureType".equals(buffKey)) {
                    continue;
                }
                if (pillBuffs.contains(buffKey, Constants.NBT.TAG_INT)) {
                    int buffLevel = pillBuffs.getInt(buffKey);
                    targetBuffs.putInt(buffKey, buffLevel);
                    if ("life".equals(buffKey)) {
                        LifeHandler.applyLifeBuff(target);
                    }
                    if ("attack".equals(buffKey)) {
                        AttackHandler.applyAttackBuff(target);
                    }
                    if ("spirit_shield".equals(buffKey)) {
                        SpiritShieldHandler.initializeSpiritShield(target);
                    }
                    appliedCount++;
                    String buffName = new TranslationTextComponent("buff.enhance." + buffKey).getString();
                    appliedBuffs.append(buffName).append(" Lv.").append(buffLevel);
                    if (appliedCount < getRealBuffCount(pillBuffs)) {
                        appliedBuffs.append(", ");
                    }
                }
            }
            targetData.put("WeaponHouseBuffs", targetBuffs);
            if (!(target instanceof PlayerEntity)) {
                BossBarHandler.createOrUpdateBossBar(target);
            }
            if (appliedCount > 0 && !user.world.isRemote) {
                String targetName = target.getName().getString();
                String finalBuffs = appliedBuffs.toString();
                if (target == user) {
                    user.sendMessage(
                            new TranslationTextComponent("message.enhance_pill.applied",
                                    targetName, appliedCount, finalBuffs)
                                    .mergeStyle(TextFormatting.GREEN),
                            user.getUniqueID()
                    );
                } else if (isEntityInteract) {
                    user.sendMessage(
                            new TranslationTextComponent("message.enhance_pill.applied",
                                    targetName, appliedCount, finalBuffs)
                                    .mergeStyle(TextFormatting.GREEN),
                            user.getUniqueID()
                    );
                    if (target instanceof PlayerEntity) {
                        target.sendMessage(
                                new TranslationTextComponent("message.enhance_pill.received",
                                        user.getName().getString(), appliedCount, finalBuffs)
                                        .mergeStyle(TextFormatting.AQUA),
                                target.getUniqueID()
                        );
                    }
                }
            }
        } else if (!user.world.isRemote) {
            user.sendMessage(
                    new TranslationTextComponent("message.enhance_pill.no_buffs")
                            .mergeStyle(TextFormatting.RED),
                    user.getUniqueID()
            );
        }
    }
    private static int getRealBuffCount(CompoundNBT pillBuffs) {
        int count = 0;
        for (String buffKey : pillBuffs.keySet()) {
            if (!"PillTextureType".equals(buffKey) && pillBuffs.contains(buffKey, Constants.NBT.TAG_INT)) {
                count++;
            }
        }
        return count;
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
    @OnlyIn(Dist.CLIENT)
    public static void addPillInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        CompoundNBT pillNBT = stack.getOrCreateTag();
        if (pillNBT.contains("EnhancePillBuffs")) {
            CompoundNBT pillBuffs = pillNBT.getCompound("EnhancePillBuffs");
            List<String> validBuffs = new ArrayList<>();
            for (String buffKey : pillBuffs.keySet()) {
                if (pillBuffs.getInt(buffKey) > 0) {
                    validBuffs.add(buffKey);
                }
            }
            if (!validBuffs.isEmpty()) {
                tooltip.add(new TranslationTextComponent("tooltip.enhance_pill.contains").mergeStyle(TextFormatting.GRAY));

                for (String buffKey : validBuffs) {
                    int buffLevel = pillBuffs.getInt(buffKey);
                    tooltip.add(new TranslationTextComponent("tooltip.enhance_pill.buff_entry",
                            new TranslationTextComponent("buff.enhance." + buffKey), buffLevel)
                            .mergeStyle(TextFormatting.AQUA));
                }
            } else {
                tooltip.add(new TranslationTextComponent("tooltip.enhance_pill.no_buffs").mergeStyle(TextFormatting.RED));
            }
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_pill.no_buffs").mergeStyle(TextFormatting.RED));
        }

        tooltip.add(new TranslationTextComponent("tooltip.enhance_pill.usage_right_click")
                .mergeStyle(TextFormatting.DARK_GRAY));
        tooltip.add(new TranslationTextComponent("tooltip.enhance_pill.usage_entity_interact")
                .mergeStyle(TextFormatting.DARK_GRAY));
    }}